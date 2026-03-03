package com.zephyrus.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyrus.app.domain.model.Location

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onLocationSelected: (Location) -> Unit,
    onCurrentLocationSelected: () -> Unit = {},
    onDismiss: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Search Location") },
            actions = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            },
        )

        // Search input
        SearchBar(
            query = uiState.query,
            onQueryChange = { viewModel.onQueryChanged(it) },
            onSearch = { },
            active = false,
            onActiveChange = { },
            placeholder = { Text("City, ZIP code, or airport code") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) { }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Search results
            if (uiState.isSearching) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (uiState.searchResults.isNotEmpty()) {
                item {
                    Text(
                        "Search Results",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(uiState.searchResults) { location ->
                    ListItem(
                        headlineContent = { Text(location.name) },
                        supportingContent = {
                            val subtitle = listOfNotNull(
                                location.admin1.takeIf { it.isNotEmpty() },
                                location.country.takeIf { it.isNotEmpty() },
                            ).joinToString(", ")
                            if (subtitle.isNotEmpty()) Text(subtitle)
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.saveLocation(location) }) {
                                Icon(Icons.Filled.BookmarkBorder, contentDescription = "Save")
                            }
                        },
                        modifier = Modifier.clickable { onLocationSelected(location) },
                    )
                }
            }

            // Saved locations — always shown, with "Current Location" pinned at top
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Saved Locations",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Current Location") },
                    supportingContent = { Text("Use device GPS") },
                    leadingContent = {
                        Icon(Icons.Filled.MyLocation, contentDescription = "Current location")
                    },
                    modifier = Modifier.clickable { onCurrentLocationSelected() },
                )
            }
            if (uiState.savedLocations.isNotEmpty()) {
                items(uiState.savedLocations) { location ->
                    ListItem(
                        headlineContent = { Text(location.name) },
                        supportingContent = {
                            val subtitle = location.admin1.takeIf { it.isNotEmpty() }
                            if (subtitle != null) Text(subtitle)
                        },
                        leadingContent = {
                            Icon(Icons.Filled.Bookmark, contentDescription = "Saved")
                        },
                        trailingContent = {
                            IconButton(onClick = { viewModel.deleteLocation(location.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        },
                        modifier = Modifier.clickable { onLocationSelected(location) },
                    )
                }
            }

            // Error message
            if (uiState.error != null) {
                item {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}
