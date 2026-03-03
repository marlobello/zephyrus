package com.zephyrus.app.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    fun label(): String = when (this) {
        SYSTEM -> "System"
        LIGHT -> "Light"
        DARK -> "Dark"
    }
}
