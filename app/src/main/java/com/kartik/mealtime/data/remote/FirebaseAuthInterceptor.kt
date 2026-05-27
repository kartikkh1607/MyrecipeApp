package com.kartik.mealtime.data.remote

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the current user's Firebase ID token as `Authorization: Bearer <token>`
 * to every request. The Cloudflare Worker proxy (server/cloudflare-worker) requires
 * this so it isn't an open relay — the proxy holds the real Spoonacular/Gemini/Groq
 * keys and only serves requests from signed-in MealTime users.
 *
 * `getIdToken(false)` returns the cached token unless it's expired, so this adds no
 * network round-trip on the common path. We block with [Tasks.await] because OkHttp
 * interceptors run on a background dispatcher thread (never the main thread).
 *
 * If there's no signed-in user or the token can't be fetched, the request proceeds
 * without the header and the proxy responds 401 — which the callers already surface
 * as a friendly error.
 */
class FirebaseAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val user = FirebaseAuth.getInstance().currentUser
            ?: return chain.proceed(request)

        val token = runCatching { Tasks.await(user.getIdToken(false)).token }.getOrNull()
        val authed = if (token != null) {
            request.newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            request
        }
        return chain.proceed(authed)
    }
}
