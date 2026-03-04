package com.zephyrus.app.util

import com.zephyrus.app.domain.model.MoonPhaseData
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Calculates moon phase using the synodic lunar cycle (~29.53 days).
 * Used as a fallback when the moon phase API is unavailable.
 */
object MoonPhaseCalculator {

    private const val LUNAR_CYCLE_DAYS = 29.53058770576
    private val KNOWN_NEW_MOON: LocalDate = LocalDate.of(2000, 1, 6)

    private fun calculate(date: LocalDate = LocalDate.now()): Double {
        val daysSince = ChronoUnit.DAYS.between(KNOWN_NEW_MOON, date).toDouble()
        val phase = (daysSince % LUNAR_CYCLE_DAYS) / LUNAR_CYCLE_DAYS
        return if (phase < 0) phase + 1.0 else phase
    }

    fun toMoonPhaseData(date: LocalDate = LocalDate.now()): MoonPhaseData {
        val phase = calculate(date)
        return MoonPhaseData(
            phaseName = phaseName(phase),
            illumination = illumination(phase),
            emoji = emoji(phase),
        )
    }

    private fun emoji(phase: Double): String = when {
        phase < 0.0625 -> "🌑"
        phase < 0.1875 -> "🌒"
        phase < 0.3125 -> "🌓"
        phase < 0.4375 -> "🌔"
        phase < 0.5625 -> "🌕"
        phase < 0.6875 -> "🌖"
        phase < 0.8125 -> "🌗"
        phase < 0.9375 -> "🌘"
        else -> "🌑"
    }

    private fun phaseName(phase: Double): String = when {
        phase < 0.0625 -> "New Moon"
        phase < 0.1875 -> "Waxing Crescent"
        phase < 0.3125 -> "First Quarter"
        phase < 0.4375 -> "Waxing Gibbous"
        phase < 0.5625 -> "Full Moon"
        phase < 0.6875 -> "Waning Gibbous"
        phase < 0.8125 -> "Last Quarter"
        phase < 0.9375 -> "Waning Crescent"
        else -> "New Moon"
    }

    private fun illumination(phase: Double): Double {
        return (1.0 - abs(2.0 * phase - 1.0)) * 100.0
    }
}
