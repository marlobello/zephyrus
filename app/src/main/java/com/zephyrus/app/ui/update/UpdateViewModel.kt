package com.zephyrus.app.ui.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrus.app.data.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updateRepository: UpdateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    /** Silent check on launch — respects 24h throttle. */
    fun checkOnLaunch() {
        if (_uiState.value !is UpdateUiState.Idle) return
        viewModelScope.launch {
            val result = updateRepository.checkForUpdate(forceCheck = false)
            result.onSuccess { info ->
                if (info != null) {
                    _uiState.value = UpdateUiState.UpdateAvailable(info)
                }
                // If null (throttled or up-to-date), stay Idle — don't show anything
            }
            result.onFailure { e ->
                Timber.w(e, "Silent update check failed — ignoring")
                // Don't surface errors for silent checks
            }
        }
    }

    /** Manual check from About screen — always checks, shows result. */
    fun checkNow() {
        _uiState.value = UpdateUiState.Checking
        viewModelScope.launch {
            val result = updateRepository.checkForUpdate(forceCheck = true)
            result.onSuccess { info ->
                _uiState.value = if (info != null) {
                    UpdateUiState.UpdateAvailable(info)
                } else {
                    UpdateUiState.UpToDate
                }
            }
            result.onFailure { e ->
                _uiState.value = UpdateUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /** Download and install the APK. */
    fun downloadAndInstall(apkUrl: String) {
        _uiState.value = UpdateUiState.Downloading
        updateRepository.downloadAndInstall(apkUrl)
    }

    /** Dismiss the update banner (reset to Idle). */
    fun dismiss() {
        _uiState.value = UpdateUiState.Idle
    }
}
