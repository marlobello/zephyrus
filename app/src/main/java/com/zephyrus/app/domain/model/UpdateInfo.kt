package com.zephyrus.app.domain.model

/**
 * Information about an available app update from GitHub Releases.
 */
data class UpdateInfo(
    val version: String,
    val releaseNotes: String,
    val apkUrl: String,
    val htmlUrl: String,
)
