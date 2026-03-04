package com.zephyrus.app.util

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Calculates moon phase using the synodic lunar cycle (~29.53 days).
 * Returns a value from 0.0 to 1.0:
 *   0.00 = New Moon
 *   0.25 = First Quarter
 *   0.50 = Full Moon
 *   0.75 = Last Quarter
 */
object MoonPhaseCalculator {

    private const val LUNAR_CYCLE_DAYS = 29.53058770576
    private val KNOWN_NEW_MOON: LocalDate = LocalDate.of(2000, 1, 6)

    fun calculate(dateString: String): Double {
        val date = try {
            LocalDate.parse(dateString)
        } catch (_: Exception) {
            LocalDate.now()
        }
        return calculate(date)
    }

    fun calculate(date: LocalDate = LocalDate.now()): Double {
        val daysSince = ChronoUnit.DAYS.between(KNOWN_NEW_MOON, date).toDouble()
        val phase = (daysSince % LUNAR_CYCLE_DAYS) / LUNAR_CYCLE_DAYS
        return if (phase < 0) phase + 1.0 else phase
    }
}
