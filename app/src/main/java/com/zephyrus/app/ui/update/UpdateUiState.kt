package com.zephyrus.app.ui.update

import com.zephyrus.app.domain.model.UpdateInfo

/**
 * UI state for the update feature, used by both the About screen and the Current screen banner.
 */
sealed interface UpdateUiState {
    /** No check performed yet, or throttled. */
    data object Idle : UpdateUiState

    /** Actively checking GitHub for a new release. */
    data object Checking : UpdateUiState

    /** App is up to date. */
    data object UpToDate : UpdateUiState

    /** A newer version is available. */
    data class UpdateAvailable(val info: UpdateInfo) : UpdateUiState

    /** APK download is in progress. */
    data object Downloading : UpdateUiState

    /** Check failed. */
    data class Error(val message: String) : UpdateUiState
}
