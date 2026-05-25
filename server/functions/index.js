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

initializeApp();
setGlobalOptions({ region: "us-central1", maxInstances: 10 });

const SPOONACULAR_API_KEY = defineSecret("SPOONACULAR_API_KEY");
const GEMINI_API_KEY = defineSecret("GEMINI_API_KEY");
const GROQ_API_KEY = defineSecret("GROQ_API_KEY");

const GEMINI_MODEL = "gemini-2.0-flash";
const GROQ_MODEL = "llama-3.3-70b-versatile";

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

    try {
      const upstream = await fetch(url, { method: "GET" });
      const body = await upstream.text();
      res.status(upstream.status).type("application/json").send(body);
    } catch (e) {
      res.status(502).json({ error: "Spoonacular upstream failed", detail: String(e) });
    }
  }
);

/** Gemini chat proxy. POST /gemini  with the GenerateContentRequest JSON body. */
export const gemini = onRequest({ secrets: [GEMINI_API_KEY] }, async (req, res) => {
  if (!(await requireAuth(req, res))) return;
  const url =
    `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent` +
    `?key=${GEMINI_API_KEY.value()}`;
  try {
    const upstream = await fetch(url, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(req.body),
    });
    const body = await upstream.text();
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
