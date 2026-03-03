package com.zephyrus.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zephyrus.app.data.repository.LocationRepository
import com.zephyrus.app.domain.model.Location
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    init {
        observeSavedLocations()
    }

    private fun observeSavedLocations() {
        viewModelScope.launch {
            locationRepository.savedLocations.collect { locations ->
                _uiState.update { it.copy(savedLocations = locations) }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.length >= 2) {
            searchJob = viewModelScope.launch {
                delay(300) // debounce
                search(query)
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    private suspend fun search(query: String) {
        _uiState.update { it.copy(isSearching = true, error = null) }
        Timber.d("Searching for: %s", query)

        locationRepository.searchLocations(query)
            .onSuccess { results ->
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            }
            .onFailure { e ->
                Timber.e(e, "Search failed")
                _uiState.update { it.copy(error = "Search failed.", isSearching = false) }
            }
    }

    fun saveLocation(location: Location) {
        viewModelScope.launch {
            Timber.d("Saving location: %s", location.name)
            locationRepository.saveLocation(location)
        }
    }

    fun deleteLocation(locationId: Long) {
        viewModelScope.launch {
            locationRepository.deleteLocation(locationId)
        }
    }
}
