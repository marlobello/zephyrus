package com.zephyrus.app.ui.search

import com.zephyrus.app.domain.model.Location

data class SearchUiState(
    val query: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<Location> = emptyList(),
    val savedLocations: List<Location> = emptyList(),
    val error: String? = null,
)
