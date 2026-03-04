package com.zephyrus.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MoonPhaseResponse(
    val phase: String = "",
    val illumination: Double = 0.0,
    val emoji: String = "🌑",
)
