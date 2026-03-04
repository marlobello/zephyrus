package com.zephyrus.app.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Maps exceptions to user-friendly error messages, distinguishing
 * network issues from server errors and other failures.
 */
object ErrorMessages {
    fun forWeather(e: Throwable): String = when {
        e.isNoInternet() -> "No internet connection. Check your network and try again."
        e.isTimeout() -> "Request timed out. The weather service may be slow — try again."
        else -> "Unable to load weather data. Tap Retry to try again."
    }

    fun forForecast(e: Throwable): String = when {
        e.isNoInternet() -> "No internet connection. Check your network and try again."
        e.isTimeout() -> "Request timed out. The weather service may be slow — try again."
        else -> "Unable to load forecast data. Tap Retry to try again."
    }

    fun forSearch(e: Throwable): String = when {
        e.isNoInternet() -> "No internet connection. Check your network and try again."
        e.isTimeout() -> "Search timed out. Try again."
        else -> "Search failed. Please try again."
    }

    fun forMap(e: Throwable): String = when {
        e.isNoInternet() -> "No internet connection."
        e.isTimeout() -> "Map data request timed out."
        else -> "Unable to load map data."
    }

    fun forLocation(e: Throwable): String = when {
        e.isNoInternet() -> "No internet connection. Unable to determine your location."
        else -> "Unable to determine your location."
    }
}

private fun Throwable.isNoInternet(): Boolean =
    this is UnknownHostException ||
        (this is IOException && this !is SocketTimeoutException) ||
        cause?.isNoInternet() == true

private fun Throwable.isTimeout(): Boolean =
    this is SocketTimeoutException ||
        cause?.isTimeout() == true
