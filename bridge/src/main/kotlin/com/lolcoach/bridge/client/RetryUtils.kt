package com.lolcoach.bridge.client

import kotlinx.coroutines.delay

suspend fun <T> retryWithBackoff(
    maxRetries: Int = 5,
    initialDelayMs: Long = 1000,
    maxDelayMs: Long = 30000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    repeat(maxRetries - 1) {
        try {
            return block()
        } catch (e: Exception) {
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMs)
        }
    }
    return block()
}
