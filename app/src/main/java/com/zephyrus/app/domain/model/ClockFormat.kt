package com.zephyrus.app.domain.model

enum class ClockFormat {
    TWELVE_HOUR,
    TWENTY_FOUR_HOUR;

    fun label(): String = when (this) {
        TWELVE_HOUR -> "12-hour"
        TWENTY_FOUR_HOUR -> "24-hour"
    }
}
