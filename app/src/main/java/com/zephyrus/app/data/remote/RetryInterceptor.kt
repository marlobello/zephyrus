package com.zephyrus.app.data.remote

import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Coroutine-based exponential backoff retry for suspend functions.
 * Retries on any exception with increasing delays: 500ms → 1s → 2s → 4s (capped at maxDelayMs).
 */
suspend fun <T> withRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 500,
    maxDelayMs: Long = 8000,
    tag: String = "API",
    block: suspend () -> T,
): T {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null

    for (attempt in 0..maxRetries) {
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            if (attempt == maxRetries) break
            Timber.w("%s attempt %d/%d failed, retrying in %dms: %s",
                tag, attempt + 1, maxRetries + 1, currentDelay, e.message)
            delay(currentDelay)
            currentDelay = (currentDelay * 2).coerceAtMost(maxDelayMs)
        }
    }

    throw lastException!!
}
