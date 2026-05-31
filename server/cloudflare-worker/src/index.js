/**
 * MealTime API proxy — Cloudflare Worker (free plan, no credit card).
 *
 * WHY THIS EXISTS
 * ---------------
 * The Android app must not ship the Spoonacular / Gemini / Groq API keys inside
 * the APK — anything in the APK can be extracted. This Worker holds the keys
 * server-side (as Cloudflare secrets) and forwards requests, so the keys never
 * reach the client.
 *
 * Every request must carry a valid Firebase ID token (Authorization: Bearer
 * <token>) for the MealTime project, so the Worker is not an open relay that
 * anyone who finds the URL could use to burn your quota.
 *
 * Routes (mirror the old Firebase function so the app change is just a base URL):
 *   GET  /spoonacular/<path>?<query>   -> api.spoonacular.com/<path>  (+ apiKey)
 *   POST /gemini                       -> Gemini :generateContent     (+ ?key=)
 *   POST /groq                         -> Groq chat/completions        (+ Bearer)
 *
 * Secrets (set with `wrangler secret put <NAME>`):
 *   SPOONACULAR_API_KEY, GEMINI_API_KEY, GROQ_API_KEY
 * Vars (in wrangler.toml, not secret):
 *   FIREBASE_PROJECT_ID
 */

const GEMINI_MODEL = "gemini-2.0-flash";
const GROQ_MODEL = "llama-3.3-70b-versatile";

// ── Play Billing entitlement ───────────────────────────────────────────────
const ANDROID_PACKAGE_NAME = "com.kartik.mealtime";

// ── Per-user Gemini quota (Upstash Redis when configured, KV fallback) ────
// Caps the *paid* upstream (Gemini) per uid per UTC-day so a runaway user can't
// drain the AI budget below the subscription's net revenue. Tunable via wrangler
// vars (GEMINI_FREE_DAILY_QUOTA / GEMINI_PREMIUM_DAILY_QUOTA) so the limits can
// be changed without a code deploy. Defaults are the safety floor if vars are
// unset. Counter expires 60h after write so it dies on its own next day.
//
// Strict enforcement uses Upstash Redis atomic INCR (set UPSTASH_REDIS_REST_URL
// + UPSTASH_REDIS_REST_TOKEN as secrets). Without those secrets the worker falls
// back to KV read-modify-write, which is race-prone under concurrent fire — a
// scripted abuser can burst past the cap before the next KV read catches up.
// The cap is THE budget-protection mechanism (worst-case cost per user maxing
// every day = cap × 30 × $0.005), so strict enforcement is recommended here.
const GEMINI_FREE_DAILY_QUOTA_DEFAULT = 10;
const GEMINI_PREMIUM_DAILY_QUOTA_DEFAULT = 30;
const QUOTA_KV_TTL_SECONDS = 60 * 60 * 60;

// ── Per-uid request rate limit (KV-backed, fixed window) ───────────────────
// Protects the Spoonacular point quota and Groq's provider rate limit from a
// single abuser scripting direct calls. Per-uid per-UTC-minute fixed window —
// best-effort against KV's eventual consistency (a small burst may slip past,
// which is acceptable for an abuse cap). Failures fail-open so a KV blip
// doesn't lock everyone out. Tune via RATE_LIMIT_PER_MINUTE in wrangler.toml.
const RATE_LIMIT_PER_MINUTE_DEFAULT = 60;
const RATE_LIMIT_KV_TTL_SECONDS = 120;

// ── Shared response cache (cross-user, "first user pays") ──────────────────
// Spoonacular GETs → edge Cache API (free, unmetered, per-edge). KV is reserved
// for low-volume cross-edge data because the free plan caps writes at 1k/day —
// a hot Spoonacular endpoint would burn that budget in an hour. Cache API has
// no daily write cap, so it's the right home for the high-volume cache.
//
// Gemini structured-generation responses → KV (low volume — premium-gated and
// per-user-daily-capped — so the cross-edge hit rate is worth the write cost).
const SPOON_CACHE_TTL_SECONDS = 7 * 24 * 60 * 60;     // recipe data barely changes
const GEMINI_CACHE_TTL_SECONDS = 30 * 24 * 60 * 60;   // generated recipes are reusable

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url);

    // RTDN push from Pub/Sub arrives WITHOUT a Firebase token — it's authenticated by a
    // shared secret instead, so it must be handled before the Firebase auth gate below.
    if (url.pathname === "/billing/rtdn") return handleRtdn(request, url, env);

    // ── Auth gate: require a valid Firebase ID token ─────────────────────────
    const header = request.headers.get("Authorization") || "";
    const match = header.match(/^Bearer (.+)$/);
    if (!match) {
      return json(401, { error: "Missing Authorization: Bearer <Firebase ID token>" });
    }
    const claims = await verifyFirebaseToken(match[1], env.FIREBASE_PROJECT_ID);
    if (!claims) {
      return json(401, { error: "Invalid or expired ID token" });
    }

    // Per-uid rate limit, applied to every authenticated route. A single abuser scripting
    // direct calls can't burn shared Spoonacular points or Groq quota for everyone else.
    const rl = await checkAndIncrementRateLimit(env, claims.sub);
    if (!rl.allowed) {
      return new Response(
        JSON.stringify({ error: "rate_limit_exceeded", limit: rl.limit, used: rl.used }),
        {
          status: 429,
          headers: {
            "Content-Type": "application/json",
            "Retry-After": String(rl.retryAfter),
          },
        }
      );
    }

    // ── Route ────────────────────────────────────────────────────────────────
    if (url.pathname.startsWith("/spoonacular")) return handleSpoonacular(request, url, env, ctx);
    if (url.pathname === "/gemini") return handleGemini(request, env, ctx, claims);
    if (url.pathname === "/groq") return handleGroq(request, env, claims);
    if (url.pathname === "/billing/verify") return handleVerifyPurchase(request, env, claims);
    return json(404, { error: "Not found" });
  },
};

// ── Upstream handlers ────────────────────────────────────────────────────────

async function handleSpoonacular(request, url, env, ctx) {
  if (request.method !== "GET") return json(405, { error: "Spoonacular proxy is GET only" });

  const path = url.pathname.replace(/^\/spoonacular/, "");
  const upstream = new URL(`https://api.spoonacular.com${path}`);
  for (const [k, v] of url.searchParams.entries()) upstream.searchParams.set(k, v);
  upstream.searchParams.set("apiKey", env.SPOONACULAR_API_KEY);

  // Cache key is derived from the normalised *incoming* params (sort + trim + lowercase),
  // NOT the upstream URL — that one carries apiKey, which must never leak into the key
  // and would also defeat sharing. "Pasta " and "pasta" collapse to the same entry.
  const normQuery = [...url.searchParams.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([k, v]) => `${k}=${v.trim().toLowerCase()}`)
    .join("&");
  const cacheHash = await sha256(`${path.toLowerCase()}?${normQuery}`);
  // Synthetic cache key — any URL works as a Cache API key; the host is just a namespace.
  const cacheKey = new Request(`https://cache.mealtime.invalid/spoonacular/${cacheHash}`, {
    method: "GET",
  });

  const cache = caches.default;
  const cached = await cache.match(cacheKey).catch(() => null);
  if (cached) {
    const body = await cached.text();
    return new Response(body, {
      status: cached.status,
      headers: { "Content-Type": "application/json", "X-Cache": "HIT" },
    });
  }

  try {
    const upstreamRes = await fetch(upstream, { method: "GET" });
    const body = await upstreamRes.text();
    // Only cache successes — never serve a cached 429/500 for days.
    if (upstreamRes.ok) {
      const cacheable = new Response(body, {
        status: upstreamRes.status,
        headers: {
          "Content-Type": "application/json",
          "Cache-Control": `public, max-age=${SPOON_CACHE_TTL_SECONDS}`,
        },
      });
      // ctx.waitUntil lets the write finish after we've already returned to the user.
      ctx.waitUntil(cache.put(cacheKey, cacheable));
    }
    return new Response(body, {
      status: upstreamRes.status,
      headers: { "Content-Type": "application/json", "X-Cache": "MISS" },
    });
  } catch (e) {
    return json(502, { error: "Spoonacular upstream failed", detail: String(e) });
  }
}

async function handleGemini(request, env, ctx, claims) {
  if (request.method !== "POST") return json(405, { error: "Gemini proxy is POST only" });

  const body = await request.text(); // forward the GenerateContentRequest untouched

  // Premium gate: structured generation (responseMimeType=application/json) is the paid
  // recipe-generation path. Plain chat omits responseMimeType and stays free. The premium
  // signal is the server-set `premium` custom claim carried in the (already-verified) token.
  let isStructuredGeneration = false;
  try {
    isStructuredGeneration =
      JSON.parse(body)?.generationConfig?.responseMimeType === "application/json";
  } catch {
    /* unparseable body → treat as free chat; the upstream will reject if malformed */
  }
  const isPremium = claims.premium === true;
  if (isStructuredGeneration && !isPremium) {
    return json(402, { error: "premium_required" });
  }

  // Only structured-output calls are cacheable. Chat/recommendations are personalised
  // and conversational — never cache them. Structured prompts ARE a pure function of
  // (query + dietary prefs), so identical bodies across users can safely share a result.
  // Cache lookup runs BEFORE the daily quota check so a cache hit doesn't burn a slot
  // (we didn't actually call Gemini).
  const cacheKey = isStructuredGeneration ? `cache:gemini:${await sha256(body)}` : null;
  if (cacheKey) {
    const cached = await readKvCache(env, cacheKey);
    if (cached) {
      return new Response(cached.body, {
        status: cached.status,
        headers: { "Content-Type": "application/json", "X-Cache": "HIT" },
      });
    }
  }

  // Per-user daily cap (run AFTER the premium gate AND cache so neither paywall hits
  // nor cache hits burn quota slots). The check + increment are best-effort against KV's
  // eventual consistency — a small burst may race past the limit, which is acceptable
  // for an abuse cap (we're not enforcing a hard SLA).
  const quota = await checkAndIncrementGeminiQuota(env, claims.sub, isPremium);
  if (!quota.allowed) {
    return json(429, {
      error: "user_quota_exceeded",
      limit: quota.limit,
      used: quota.used,
      tier: isPremium ? "premium" : "free",
    });
  }

  const upstreamUrl =
    `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent` +
    `?key=${env.GEMINI_API_KEY}`;

  try {
    const upstream = await fetch(upstreamUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body,
    });
    const upstreamBody = await upstream.text();
    if (cacheKey && upstream.ok) {
      // Best-effort write; finishes after we return.
      ctx.waitUntil(writeKvCache(env, cacheKey, upstreamBody, upstream.status, GEMINI_CACHE_TTL_SECONDS));
    }
    return new Response(upstreamBody, {
      status: upstream.status,
      headers: {
        "Content-Type": "application/json",
        "X-Cache": cacheKey ? "MISS" : "SKIP",
      },
    });
  } catch (e) {
    return json(502, { error: "Gemini upstream failed", detail: String(e) });
  }
}

/**
 * Atomically increments today's Gemini call count for [uid] and rejects if it crosses
 * the tier limit. When UPSTASH_REDIS_REST_URL / _TOKEN are set, uses Redis INCR + EXPIRE
 * (strictly consistent — no race even under heavy concurrent fire). Otherwise falls
 * back to the KV read-modify-write path, which is race-prone but keeps the worker
 * functional pre-migration. Per-day key rotates at UTC midnight.
 */
async function checkAndIncrementGeminiQuota(env, uid, isPremium) {
  if (!uid) return { allowed: true, used: 0, limit: 0 }; // shouldn't happen post-auth
  const limit = isPremium
    ? parseInt(env.GEMINI_PREMIUM_DAILY_QUOTA, 10) || GEMINI_PREMIUM_DAILY_QUOTA_DEFAULT
    : parseInt(env.GEMINI_FREE_DAILY_QUOTA, 10) || GEMINI_FREE_DAILY_QUOTA_DEFAULT;
  const day = new Date().toISOString().slice(0, 10); // YYYY-MM-DD (UTC)
  const key = `quota:gemini:${uid}:${day}`;

  if (env.UPSTASH_REDIS_REST_URL && env.UPSTASH_REDIS_REST_TOKEN) {
    try {
      const used = await upstashIncrWithExpiry(env, key, QUOTA_KV_TTL_SECONDS);
      // INCR runs unconditionally — over-cap requests still bump the counter, which is
      // fine: once you're over, you're over for the day. The counter expires at TTL.
      if (used > limit) return { allowed: false, used, limit };
      return { allowed: true, used, limit };
    } catch (e) {
      // Fail-open during an Upstash outage so a Redis blip doesn't lock everyone out.
      // The KV race-path is intentionally NOT a fallback: an attacker who saturated
      // Upstash would re-enable the very race we deployed Upstash to close.
      console.error("Upstash quota incr failed, allowing:", String(e));
      return { allowed: true, used: 0, limit };
    }
  }

  // Legacy KV path — race-prone, kept so the worker still functions if Upstash secrets
  // aren't set. Under heavy concurrent fire a few calls may slip past the cap.
  const raw = await env.BILLING_KV.get(key);
  const used = raw ? parseInt(raw, 10) || 0 : 0;
  if (used >= limit) return { allowed: false, used, limit };
  await env.BILLING_KV.put(key, String(used + 1), { expirationTtl: QUOTA_KV_TTL_SECONDS });
  return { allowed: true, used: used + 1, limit };
}

/**
 * Atomic INCR + EXPIRE against Upstash Redis REST, pipelined in one round-trip. Returns
 * the new count. EXPIRE is set on every call (Redis ignores it for keys with TTL already
 * set, and it costs nothing) so we don't need a separate "is this a new key" check —
 * keeps the call site simple at the cost of one extra command per request.
 */
async function upstashIncrWithExpiry(env, key, ttlSeconds) {
  const res = await fetch(`${env.UPSTASH_REDIS_REST_URL}/pipeline`, {
    method: "POST",
    headers: {
      Authorization: `Bearer ${env.UPSTASH_REDIS_REST_TOKEN}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify([
      ["INCR", key],
      ["EXPIRE", key, String(ttlSeconds)],
    ]),
  });
  if (!res.ok) throw new Error(`Upstash ${res.status}: ${await res.text().catch(() => "")}`);
  const data = await res.json();
  const count = data?.[0]?.result;
  if (typeof count !== "number") throw new Error(`Bad Upstash response: ${JSON.stringify(data)}`);
  return count;
}

async function handleGroq(request, env, claims) {
  if (request.method !== "POST") return json(405, { error: "Groq proxy is POST only" });

  let incoming;
  try {
    incoming = await request.json();
  } catch {
    return json(400, { error: "Body must be JSON" });
  }

  // Premium gate, mirroring /gemini: structured output (response_format = json_object /
  // json_schema, OpenAI-compatible) is the paid recipe-generation path. Plain chat omits
  // response_format and stays free. Without this gate the router's Gemini-failure
  // fallback path would be a paywall bypass — a non-premium user could craft a direct
  // /groq call with response_format set and get premium AI generation for free.
  const rfType = incoming?.response_format?.type;
  const isStructuredGeneration = rfType === "json_object" || rfType === "json_schema";
  const isPremium = claims.premium === true;
  if (isStructuredGeneration && !isPremium) {
    return json(402, { error: "premium_required" });
  }

  try {
    const upstream = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${env.GROQ_API_KEY}`,
      },
      // Server injects the model; app sends { messages, temperature, max_tokens, ... }
      body: JSON.stringify({ model: GROQ_MODEL, ...incoming }),
    });
    return passthrough(upstream);
  } catch (e) {
    return json(502, { error: "Groq upstream failed", detail: String(e) });
  }
}

// ── Firebase ID token verification (RS256, no dependencies) ───────────────────
//
// Verifies the token's signature against Google's public keys and checks the
// standard claims (issuer, audience, expiry). Google's keys are cached in the
// isolate per their Cache-Control max-age to avoid refetching on every request.

let jwkCache = { keys: null, expiresAt: 0 };

async function getGoogleKeys() {
  const now = Date.now();
  if (jwkCache.keys && now < jwkCache.expiresAt) return jwkCache.keys;

  const res = await fetch(
    "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com"
  );
  const data = await res.json();
  const cc = res.headers.get("cache-control") || "";
  const m = cc.match(/max-age=(\d+)/);
  const maxAgeMs = (m ? parseInt(m[1], 10) : 3600) * 1000;
  jwkCache = { keys: data.keys, expiresAt: now + maxAgeMs };
  return data.keys;
}

async function verifyFirebaseToken(token, projectId) {
  const parts = token.split(".");
  if (parts.length !== 3) return null;
  const [h64, p64, s64] = parts;

  let header, payload;
  try {
    header = JSON.parse(b64urlToString(h64));
    payload = JSON.parse(b64urlToString(p64));
  } catch {
    return null;
  }
  if (header.alg !== "RS256" || !header.kid) return null;

  const keys = await getGoogleKeys();
  const jwk = keys?.find((k) => k.kid === header.kid);
  if (!jwk) return null;

  let valid = false;
  try {
    const key = await crypto.subtle.importKey(
      "jwk",
      jwk,
      { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
      false,
      ["verify"]
    );
    valid = await crypto.subtle.verify(
      "RSASSA-PKCS1-v1_5",
      key,
      b64urlToBytes(s64),
      new TextEncoder().encode(`${h64}.${p64}`)
    );
  } catch {
    return null;
  }
  if (!valid) return null;

  const now = Math.floor(Date.now() / 1000);
  if (payload.exp <= now) return null;
  if (payload.iat > now + 300) return null; // tolerate small clock skew
  if (payload.aud !== projectId) return null;
  if (payload.iss !== `https://securetoken.google.com/${projectId}`) return null;
  if (!payload.sub) return null;

  return payload;
}

// ── Small helpers ─────────────────────────────────────────────────────────────

function b64urlToBytes(b64url) {
  const b64 = b64url.replace(/-/g, "+").replace(/_/g, "/");
  const pad = b64.length % 4 ? "=".repeat(4 - (b64.length % 4)) : "";
  const bin = atob(b64 + pad);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes;
}

function b64urlToString(b64url) {
  return new TextDecoder().decode(b64urlToBytes(b64url));
}

function json(status, obj) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

async function passthrough(upstream) {
  const body = await upstream.text();
  return new Response(body, {
    status: upstream.status,
    headers: { "Content-Type": "application/json" },
  });
}

// ── Shared response cache helpers ──────────────────────────────────────────
// Spoonacular cache lives in the edge Cache API (handled inline in handleSpoonacular).
// Gemini structured-output cache uses KV so the hit is shared across edges.

/** Reads a cached { body, status } envelope by [key]. Never throws — a KV blip must not
 *  break the request path; we just treat it as a miss and fall through to upstream. */
async function readKvCache(env, key) {
  try {
    const raw = await env.BILLING_KV.get(key);
    if (!raw) return null;
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

/** Best-effort write; called via ctx.waitUntil so a slow KV write never delays the
 *  user's response. Failures are swallowed for the same reason as readKvCache. */
async function writeKvCache(env, key, body, status, ttlSeconds) {
  try {
    await env.BILLING_KV.put(
      key,
      JSON.stringify({ body, status }),
      { expirationTtl: ttlSeconds }
    );
  } catch {
    /* best-effort */
  }
}

/** Per-uid per-UTC-minute fixed-window rate limit. KV reads/writes are eventually
 *  consistent — under heavy concurrent fire a few calls may slip past the cap, which
 *  is acceptable for an abuse cap (we're not enforcing a hard SLA). Fails OPEN: if KV
 *  is unavailable we let the request through rather than locking everyone out. */
async function checkAndIncrementRateLimit(env, uid) {
  if (!uid) return { allowed: true, used: 0, limit: 0 };
  const limit =
    parseInt(env.RATE_LIMIT_PER_MINUTE, 10) || RATE_LIMIT_PER_MINUTE_DEFAULT;
  const nowSec = Math.floor(Date.now() / 1000);
  const minute = Math.floor(nowSec / 60);
  const key = `rl:${uid}:${minute}`;
  try {
    const raw = await env.BILLING_KV.get(key);
    const used = raw ? parseInt(raw, 10) || 0 : 0;
    if (used >= limit) {
      return { allowed: false, used, limit, retryAfter: 60 - (nowSec % 60) };
    }
    await env.BILLING_KV.put(key, String(used + 1), {
      expirationTtl: RATE_LIMIT_KV_TTL_SECONDS,
    });
    return { allowed: true, used: used + 1, limit };
  } catch {
    return { allowed: true, used: 0, limit }; // fail-open on KV outage
  }
}

/** SHA-256 hex digest — used to build stable cache keys from request bodies / URLs. */
async function sha256(s) {
  const buf = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(s));
  const bytes = new Uint8Array(buf);
  let hex = "";
  for (let i = 0; i < bytes.length; i++) hex += bytes[i].toString(16).padStart(2, "0");
  return hex;
}

// ── Play Billing: verification + entitlement (card-free, no Firebase Functions) ──
//
// Runs entirely on the Worker so it works on Cloudflare's free plan:
//   - mints a Google service-account access token (RS256 JWT → token endpoint),
//   - validates the subscription with the Play Developer API,
//   - grants premium by setting the `premium` Firebase custom claim via the Identity
//     Toolkit REST API (the same field firebase-admin's setCustomUserClaims writes),
//   - keeps a purchaseToken→uid map in KV so RTDN can reconcile renewals/cancels.
//
// Secrets (wrangler secret put): GCP_SA_CLIENT_EMAIL, GCP_SA_PRIVATE_KEY, RTDN_SECRET.
// Binding: BILLING_KV (KV namespace). The SA must be granted Play Console API access
// AND the Firebase Authentication Admin role. See README.

/** POST /billing/verify — body { productId, purchaseToken }. Firebase-authed (uid = sub). */
async function handleVerifyPurchase(request, env, claims) {
  if (request.method !== "POST") return json(405, { error: "verify is POST only" });
  const uid = claims.sub;
  if (!uid) return json(401, { error: "No uid in token" });

  let body;
  try {
    body = await request.json();
  } catch {
    return json(400, { error: "Body must be JSON" });
  }
  const purchaseToken = String(body.purchaseToken || "");
  if (!purchaseToken) return json(400, { error: "purchaseToken is required" });

  let sub;
  try {
    sub = await verifySubscription(env, purchaseToken);
  } catch (e) {
    return json(502, { error: "Play verification failed", detail: String(e) });
  }

  if (!sub || !isSubscriptionActive(sub)) {
    await setPremiumClaim(env, uid, false).catch(() => {});
    return json(402, { error: "subscription_not_active" });
  }

  // Surface claim-set failures: if Identity Toolkit rejects (missing IAM, malformed uid,
  // transient 5xx), returning 200 would tell the app premium is granted while it isn't.
  const claimOk = await setPremiumClaim(env, uid, true);
  if (!claimOk) return json(502, { error: "Failed to set premium claim" });
  await env.BILLING_KV.put(`token:${purchaseToken}`, uid);
  return json(200, { premium: true });
}

/**
 * POST /billing/rtdn — Pub/Sub push for Real-time Developer Notifications. Authenticated
 * by a shared secret (?key=RTDN_SECRET), NOT a Firebase token. Always 204s so Pub/Sub
 * treats the message as delivered.
 *
 * We trust Google's notificationType (the actual state transition) instead of re-querying
 * and mirroring whatever the Play API returns at that moment. In test mode, 5-min monthly
 * cycles cause the API to briefly report transient non-active states between renewals,
 * which the old "verify-then-mirror" logic would treat as "subscription ended" and wipe
 * a valid premium claim. The notification itself is unambiguous — use it directly.
 */
async function handleRtdn(request, url, env) {
  if (request.method !== "POST") return json(405, { error: "rtdn is POST only" });
  if (!env.RTDN_SECRET || url.searchParams.get("key") !== env.RTDN_SECRET) {
    return json(403, { error: "Forbidden" });
  }

  let notification;
  try {
    const envelope = await request.json();
    notification = JSON.parse(b64urlToString(envelope.message.data));
  } catch {
    return new Response(null, { status: 204 }); // ack malformed/test pings
  }

  const subNotif = notification?.subscriptionNotification;
  const purchaseToken = subNotif?.purchaseToken;
  if (!purchaseToken) return new Response(null, { status: 204 });

  const uid = await env.BILLING_KV.get(`token:${purchaseToken}`);
  if (!uid) return new Response(null, { status: 204 }); // unknown token

  const intent = rtdnIntent(subNotif.notificationType);
  if (intent === "noop") return new Response(null, { status: 204 });

  try {
    if (intent === "promote") {
      await setPremiumClaim(env, uid, true);
    } else if (intent === "demote") {
      await setPremiumClaim(env, uid, false);
    } else if (intent === "verify") {
      // CANCELED: user opted out but the paid period may not have expired yet. Re-query
      // and only DEMOTE if Play confirms a definitively-inactive state. Ambiguous/transient
      // responses (PENDING, UNSPECIFIED, network noise) leave the claim alone.
      const sub = await verifySubscription(env, purchaseToken);
      if (sub && isDefinitelyInactive(sub)) {
        await setPremiumClaim(env, uid, false);
      }
    }
  } catch (e) {
    console.error("RTDN handler error:", String(e), "intent:", intent, "uid:", uid);
    /* transient — leave claim as-is; a later notification reconciles */
  }
  return new Response(null, { status: 204 });
}

/**
 * Maps Play's subscription notificationType to an entitlement intent.
 *   promote: clearly entitled    → set premium=true
 *   demote:  clearly not entitled → set premium=false
 *   verify:  ambiguous            → re-query Play, demote only on confirmed inactive
 *   noop:    irrelevant to entitlement (price-change, deferral, pending-cancel, unknown)
 *
 * Codes per https://developer.android.com/google/play/billing/rtdn-reference#sub
 */
function rtdnIntent(type) {
  switch (type) {
    case 1:  // SUBSCRIPTION_RECOVERED
    case 2:  // SUBSCRIPTION_RENEWED
    case 4:  // SUBSCRIPTION_PURCHASED
    case 6:  // SUBSCRIPTION_IN_GRACE_PERIOD
    case 7:  // SUBSCRIPTION_RESTARTED
      return "promote";
    case 5:  // SUBSCRIPTION_ON_HOLD
    case 10: // SUBSCRIPTION_PAUSED
    case 12: // SUBSCRIPTION_REVOKED
    case 13: // SUBSCRIPTION_EXPIRED
      return "demote";
    case 3:  // SUBSCRIPTION_CANCELED (still active until expiry; re-check)
      return "verify";
    default: // PRICE_CHANGE_CONFIRMED (8), DEFERRED (9), PAUSE_SCHEDULE_CHANGED (11),
             // PENDING_PURCHASE_CANCELED (20), unknown future codes
      return "noop";
  }
}

/** True if the subscription currently entitles the user (active, in grace, or
 *  cancelled-but-not-yet-expired). Used by handleVerifyPurchase. */
function isSubscriptionActive(sub) {
  if (!sub) return false;
  const state = sub.subscriptionState;
  if (state === "SUBSCRIPTION_STATE_ACTIVE" || state === "SUBSCRIPTION_STATE_IN_GRACE_PERIOD") {
    return true;
  }
  const expiry = sub.lineItems?.find((li) => li.expiryTime)?.expiryTime;
  return Boolean(expiry && new Date(expiry).getTime() > Date.now());
}

/** True ONLY for definitively non-entitled states (EXPIRED, or expiry already past).
 *  Transient/ambiguous responses (PENDING, UNSPECIFIED) return false so the caller
 *  can leave the claim alone instead of wiping a valid entitlement. */
function isDefinitelyInactive(sub) {
  if (!sub) return false;
  if (sub.subscriptionState === "SUBSCRIPTION_STATE_EXPIRED") return true;
  const expiry = sub.lineItems?.find((li) => li.expiryTime)?.expiryTime;
  return Boolean(expiry && new Date(expiry).getTime() <= Date.now());
}

async function verifySubscription(env, purchaseToken) {
  const token = await getGoogleAccessToken(env);
  const url =
    `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/` +
    `${ANDROID_PACKAGE_NAME}/purchases/subscriptionsv2/tokens/${encodeURIComponent(purchaseToken)}`;
  const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
  if (!res.ok) return null;
  return res.json();
}

/** Sets/clears the `premium` custom claim via Identity Toolkit accounts:update.
 *  Logs the upstream body on failure so misconfigurations (missing IAM, bad uid)
 *  show up in `wrangler tail` instead of failing silently. */
async function setPremiumClaim(env, uid, premium) {
  const token = await getGoogleAccessToken(env);
  const url = `https://identitytoolkit.googleapis.com/v1/projects/${env.FIREBASE_PROJECT_ID}/accounts:update`;
  const res = await fetch(url, {
    method: "POST",
    headers: { Authorization: `Bearer ${token}`, "Content-Type": "application/json" },
    body: JSON.stringify({
      localId: uid,
      customAttributes: JSON.stringify(premium ? { premium: true } : {}),
    }),
  });
  if (!res.ok) {
    const body = await res.text().catch(() => "<unreadable>");
    console.error(`setPremiumClaim ${res.status}: ${body} (uid=${uid}, premium=${premium})`);
  }
  return res.ok;
}

// ── Google service-account OAuth (JWT bearer grant) ──────────────────────────
// Mints an access token scoped for the Play Developer API + Identity Toolkit. Cached
// in the isolate until shortly before expiry to avoid a token exchange per request.

let googleTokenCache = { token: null, expiresAt: 0 };

async function getGoogleAccessToken(env) {
  const now = Date.now();
  if (googleTokenCache.token && now < googleTokenCache.expiresAt) return googleTokenCache.token;

  const key = await importServiceAccountKey(env.GCP_SA_PRIVATE_KEY);
  const iat = Math.floor(now / 1000);
  const header = strToB64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const payload = strToB64url(
    JSON.stringify({
      iss: env.GCP_SA_CLIENT_EMAIL,
      scope:
        "https://www.googleapis.com/auth/androidpublisher " +
        "https://www.googleapis.com/auth/identitytoolkit",
      aud: "https://oauth2.googleapis.com/token",
      iat,
      exp: iat + 3600,
    })
  );
  const unsigned = `${header}.${payload}`;
  const assertion = `${unsigned}.${await signRs256(unsigned, key)}`;

  const res = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  if (!res.ok) throw new Error(`Google token exchange failed: ${res.status}`);
  const data = await res.json();
  googleTokenCache = {
    token: data.access_token,
    expiresAt: now + (data.expires_in - 60) * 1000, // refresh 60s early
  };
  return googleTokenCache.token;
}

async function importServiceAccountKey(pem) {
  // Tolerate keys pasted with literal "\n" instead of real newlines.
  const normalized = pem.replace(/\\n/g, "\n");
  const b64 = normalized
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\s+/g, "");
  const der = Uint8Array.from(atob(b64), (c) => c.charCodeAt(0));
  return crypto.subtle.importKey(
    "pkcs8",
    der.buffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
}

async function signRs256(data, key) {
  const sig = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(data)
  );
  return bytesToB64url(new Uint8Array(sig));
}

function bytesToB64url(bytes) {
  let bin = "";
  for (const b of bytes) bin += String.fromCharCode(b);
  return btoa(bin).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function strToB64url(str) {
  return bytesToB64url(new TextEncoder().encode(str));
}
