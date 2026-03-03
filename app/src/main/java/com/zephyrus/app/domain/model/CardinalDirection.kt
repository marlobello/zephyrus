package com.zephyrus.app.domain.model

enum class CardinalDirection(val label: String, val minDegree: Double, val maxDegree: Double) {
    N("N", 348.75, 11.25),
    NNE("NNE", 11.25, 33.75),
    NE("NE", 33.75, 56.25),
    ENE("ENE", 56.25, 78.75),
    E("E", 78.75, 101.25),
    ESE("ESE", 101.25, 123.75),
    SE("SE", 123.75, 146.25),
    SSE("SSE", 146.25, 168.75),
    S("S", 168.75, 191.25),
    SSW("SSW", 191.25, 213.75),
    SW("SW", 213.75, 236.25),
    WSW("WSW", 236.25, 258.75),
    W("W", 258.75, 281.25),
    WNW("WNW", 281.25, 303.75),
    NW("NW", 303.75, 326.25),
    NNW("NNW", 326.25, 348.75);

    companion object {
        fun fromDegrees(degrees: Int): CardinalDirection {
            val normalized = ((degrees % 360) + 360) % 360
            // N wraps around 0, so check it specially
            return if (normalized >= 348.75 || normalized < 11.25) {
                N
            } else {
                entries.first { it != N && normalized >= it.minDegree && normalized < it.maxDegree }
            }
        }
    }
}
