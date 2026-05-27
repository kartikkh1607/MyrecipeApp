/**
 * MealTime API proxy — Firebase Cloud Functions (2nd gen).
 *
 * WHY THIS EXISTS
 * ---------------
 * The Android app currently ships the Spoonacular, Gemini, and Groq API keys
 * inside BuildConfig (see app/proguard-rules.pro — it admits the keys are
 * extractable). A determined attacker WILL pull them from the APK and burn your
 * quota / run up a bill. The only real fix is to keep the keys server-side and
 * have the app call THIS proxy instead of the third-party APIs directly.
 *
 * The proxy:
 *   1. Requires a valid Firebase Auth ID token (Authorization: Bearer <token>),
 *      so it is not an open relay anyone on the internet can abuse.
 *   2. Injects the real API key server-side and forwards the request.
 *   3. Returns the upstream response untouched.
 *
 * Keys are stored as Cloud Functions secrets (NOT in source). See README.md.
 */

import { onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { setGlobalOptions } from "firebase-functions/v2";
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { createHash } from "node:crypto";

initializeApp();
setGlobalOptions({ region: "us-central1", maxInstances: 10 });

const SPOONACULAR_API_KEY = defineSecret("SPOONACULAR_API_KEY");
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const GROQ_API_KEY = defineSecret("GROQ_API_KEY");

const GEMINI_MODEL = "gemini-2.0-flash";
const GROQ_MODEL = "llama-3.3-70b-versatile";

// ── Shared response cache ──────────────────────────────────────────────────
// The first signed-in user to make a given request pays the upstream API call;
// the response is stored in Firestore and served to every subsequent user for
// the cost of one Firestore read. This is the big quota/cost saver — Spoonacular
// has a HARD daily point quota, and identical queries ("pasta", "chicken") are
// extremely common across a recipe app's user base.
//
// One collection, one doc per unique request. `expireAt` drives a Firestore TTL
// policy (set up once in the console — see README) that auto-deletes stale docs.
const db = getFirestore();
const CACHE_COLLECTION = "apiCache";
const SPOON_TTL_MS = 7 * 24 * 60 * 60 * 1000;    // recipe data barely changes
const GEMINI_TTL_MS = 30 * 24 * 60 * 60 * 1000;  // generated recipes are reusable

const sha = (s) => createHash("sha256").update(s).digest("hex");

/** Returns the cached entry, or null on miss/expiry/error. Never throws — the
 *  cache must not be able to break the request path. */
async function readCache(docId) {
  try {
    const snap = await db.collection(CACHE_COLLECTION).doc(docId).get();
    if (!snap.exists) return null;
    const d = snap.data();
    // Honour expiry ourselves in case the TTL sweep hasn't run yet.
    if (d.expireAt && d.expireAt.toMillis() < Date.now()) return null;
    return d;
  } catch (_) {
    return null;
  }
}

/** Best-effort write; failures are swallowed so a cache hiccup never 500s a
 *  request that already succeeded upstream. */
async function writeCache(docId, body, status, ttlMs) {
  try {
    await db.collection(CACHE_COLLECTION).doc(docId).set({
      body,
      status,
      createdAt: Timestamp.now(),
      expireAt: Timestamp.fromMillis(Date.now() + ttlMs),
    });
  } catch (_) {
    /* best-effort */
  }
}

/** Rejects the request unless it carries a verifiable Firebase ID token. */
async function requireAuth(req, res) {
  const header = req.get("Authorization") || "";
  const match = header.match(/^Bearer (.+)$/);
  if (!match) {
    res.status(401).json({ error: "Missing Authorization: Bearer <Firebase ID token>" });
    return null;
  }
  try {
    return await getAuth().verifyIdToken(match[1]);
  } catch (e) {
    res.status(401).json({ error: "Invalid or expired ID token" });
    return null;
  }
}

/**
 * Spoonacular proxy. The app calls:  GET /spoonacular/<spoonacular-path>?<query>
 * and we forward to https://api.spoonacular.com/<spoonacular-path> with apiKey added.
 * Example: /spoonacular/recipes/complexSearch?query=pasta&number=20
 */
export const spoonacular = onRequest(
  { secrets: [SPOONACULAR_API_KEY] },
  async (req, res) => {
    if (!(await requireAuth(req, res))) return;

    // Strip the leading "/spoonacular" the platform may or may not include.
    const path = req.path.replace(/^\/spoonacular/, "");
    const url = new URL(`https://api.spoonacular.com${path}`);
    for (const [k, v] of Object.entries(req.query)) url.searchParams.set(k, v);
    url.searchParams.set("apiKey", SPOONACULAR_API_KEY.value());

    // Cache key: normalised path + query (sorted, trimmed, lowercased) so
    // "Pasta " and "pasta" collapse to one entry. apiKey is added AFTER and is
    // not part of req.query, so it never leaks into the key.
    const normQuery = Object.entries(req.query)
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([k, v]) => `${k}=${String(v).trim().toLowerCase()}`)
      .join("&");
    const docId = "spoon_" + sha(`${path.toLowerCase()}?${normQuery}`);

    const cached = await readCache(docId);
    if (cached) {
      res.set("X-Cache", "HIT");
      return res.status(cached.status).type("application/json").send(cached.body);
    }

    try {
      const upstream = await fetch(url, { method: "GET" });
      const body = await upstream.text();
      // Only cache successes — never serve a cached 429/500 for days.
      if (upstream.ok) await writeCache(docId, body, upstream.status, SPOON_TTL_MS);
      res.set("X-Cache", "MISS");
      res.status(upstream.status).type("application/json").send(body);
    } catch (e) {
      res.status(502).json({ error: "Spoonacular upstream failed", detail: String(e) });
    }
  }
);

/** Gemini chat proxy. POST /gemini  with the GenerateContentRequest JSON body. */
export const gemini = onRequest({ secrets: [GEMINI_API_KEY] }, async (req, res) => {
  const decoded = await requireAuth(req, res);
  if (!decoded) return;

  // Premium gate: structured generation (responseMimeType=application/json) is the
  // paid recipe-generation path. Plain chat has no responseMimeType and stays free.
  const isStructuredGeneration =
    req.body?.generationConfig?.responseMimeType === "application/json";
  if (isStructuredGeneration && decoded.premium !== true) {
    return res.status(402).json({ error: "premium_required" });
  }

  const url =
    `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent` +
    `?key=${GEMINI_API_KEY.value()}`;

  // Only structured-output calls (recipe generation / transform / meal plan) are
  // cacheable: they set responseMimeType=application/json and their prompt is a
  // pure function of (query + dietary prefs), so identical requests across users
  // can safely share one result. Chat and recommendations are personalised /
  // conversational — never cache them (docId stays null → straight passthrough).
  const isCacheable =
    req.body?.generationConfig?.responseMimeType === "application/json";
  const docId = isCacheable ? "gemini_" + sha(JSON.stringify(req.body)) : null;

  if (docId) {
    const cached = await readCache(docId);
    if (cached) {
      res.set("X-Cache", "HIT");
      return res.status(cached.status).type("application/json").send(cached.body);
    }
  }

  try {
    const upstream = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req.body),
    });
    const body = await upstream.text();
    if (docId && upstream.ok) await writeCache(docId, body, upstream.status, GEMINI_TTL_MS);
    res.set("X-Cache", docId ? "MISS" : "SKIP");
    res.status(upstream.status).type("application/json").send(body);
  } catch (e) {
    res.status(502).json({ error: "Gemini upstream failed", detail: String(e) });
  }
});

/** Groq fallback proxy. POST /groq with { messages, temperature, max_tokens }. */
export const groq = onRequest({ secrets: [GROQ_API_KEY] }, async (req, res) => {
  if (!(await requireAuth(req, res))) return;
  try {
    const upstream = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${GROQ_API_KEY.value()}`,
      },
      body: JSON.stringify({ model: GROQ_MODEL, ...req.body }),
    });
    const body = await upstream.text();
    res.status(upstream.status).type("application/json").send(body);
  } catch (e) {
    res.status(502).json({ error: "Groq upstream failed", detail: String(e) });
  }
});

// NOTE: Play Billing verification (verifyPurchase) + RTDN live in the Cloudflare Worker
// (server/cloudflare-worker), NOT here — Cloud Functions need the Blaze plan, which this
// project avoids. The `decoded.premium` gate in the gemini handler above still applies if
// this functions backend is ever used as the live proxy.
