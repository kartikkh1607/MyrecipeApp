package com.kartik.mealtime.data.remote

import kotlinx.coroutines.delay
import retrofit2.HttpException
import java.io.IOException

/**
 * Suspending replacement for the old OkHttp-level RetryInterceptor.
 *
 * Retries [block] on transient errors — IOException (no network / dropped
 * connection) or HTTP 503 (service unavailable). Backoff is linear:
 * `initialBackoffMs` × (attempt + 1), so the default (1s/2s/3s) caps the
 * worst-case extra wall time at 6 s across three attempts.
 *
 * Why this lives in the repository layer rather than as an OkHttp interceptor:
 * the previous interceptor used [Thread.sleep] to wait between attempts, which
 * blocked an OkHttp dispatcher thread for the entire backoff window. Using
 * [delay] keeps the coroutine suspended without holding any thread — and a
 * structured-concurrency cancel propagates cleanly without the "100 ms chunk +
 * isCanceled poll" dance the interceptor needed.
 */
internal suspend fun <T> withApiRetry(
    maxRetries: Int = 3,
    initialBackoffMs: Long = 1_000L,
    block: suspend () -> T,
): T {
    var lastError: Throwable? = null
    repeat(maxRetries) { attempt ->
        try {
            return block()
        } catch (e: IOException) {
            lastError = e
        } catch (e: HttpException) {
            // Retrofit raises HttpException for non-2xx — only 503 is a candidate
            // for retry; everything else (400s, 5xx other than 503) is terminal
            // and re-thrown for the caller to map to a user-facing error.
            if (e.code() != 503) throw e
            lastError = e
        }
        if (attempt < maxRetries - 1) {
            delay(initialBackoffMs * (attempt + 1))
        }
    }
    throw lastError ?: IOException("Request failed after $maxRetries retries")
}
