package com.kartik.mealtime.data.remote

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspending OkHttp call that propagates coroutine cancellation to the underlying
 * request. Cancelling the calling coroutine triggers [Call.cancel], so a user who
 * navigates away from a long AI generation stops paying for it immediately
 * instead of letting the request run to completion on its own thread.
 *
 * Shared by [GeminiAiService] and [GroqAiService] so both providers get the same
 * cancellation behaviour and we don't ship two copies of the wrapper.
 */
suspend fun OkHttpClient.await(request: Request): Response =
    suspendCancellableCoroutine { cont ->
        val call = newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (cont.isActive) cont.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                if (cont.isActive) cont.resume(response)
            }
        })
    }
