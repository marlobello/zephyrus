package com.zephyrus.app.domain.model

data class Location(
    val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String = "",
    val admin1: String = "",
    val isDeviceLocation: Boolean = false,
)
