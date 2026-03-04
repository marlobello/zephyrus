package com.zephyrus.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MoonPhaseResponse(
    val apiversion: String = "",
    val numphases: Int = 0,
    val phasedata: List<MoonPhaseEntry> = emptyList(),
)

@Serializable
data class MoonPhaseEntry(
    val day: Int = 0,
    val month: Int = 0,
    val year: Int = 0,
    val phase: String = "",
    val time: String = "",
)
