package com.zephyrus.app.ui.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zephyrus.app.BuildConfig
import com.zephyrus.app.R
import com.zephyrus.app.ui.update.UpdateUiState
import com.zephyrus.app.ui.update.UpdateViewModel
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onDismiss: () -> Unit,
    updateViewModel: UpdateViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("About") },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Image(
                painter = painterResource(R.mipmap.ic_launcher_foreground),
                contentDescription = "Zephyrus",
                modifier = Modifier.size(120.dp),
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Zephyrus",
                style = MaterialTheme.typography.headlineMedium,
            )

            Text(
                text = "A simple weather app",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Update section
            UpdateSection(
                state = updateState,
                onCheckNow = { updateViewModel.checkNow() },
                onDownload = { url -> updateViewModel.downloadAndInstall(url) },
            )

            Spacer(modifier = Modifier.height(32.dp))

            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/marlobello/zephyrus".toUri()))
            }) {
                Text("Source Code on GitHub")
            }

            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://github.com/marlobello/zephyrus/blob/main/LICENSE".toUri()))
            }) {
                Text("MIT License")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Weather Data",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Powered by Open-Meteo.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://open-meteo.com/".toUri()))
            }) {
                Text("open-meteo.com")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Radar Data",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "Powered by RainViewer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.rainviewer.com/".toUri()))
            }) {
                Text("rainviewer.com")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Map Tiles",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "© OpenStreetMap contributors",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://www.openstreetmap.org/copyright".toUri()))
            }) {
                Text("openstreetmap.org")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Moon Phase Data",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "U.S. Naval Observatory",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, "https://aa.usno.navy.mil/".toUri()))
            }) {
                Text("aa.usno.navy.mil")
            }
        }
    }
}

@Composable
private fun UpdateSection(
    state: UpdateUiState,
    onCheckNow: () -> Unit,
    onDownload: (String) -> Unit,
) {
    when (state) {
        is UpdateUiState.Idle -> {
            OutlinedButton(onClick = onCheckNow) {
                Text("Check for Updates")
            }
        }

        is UpdateUiState.Checking -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Checking for updates…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        is UpdateUiState.UpToDate -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text("You're up to date!", style = MaterialTheme.typography.bodyMedium)
            }
        }

        is UpdateUiState.UpdateAvailable -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Filled.NewReleases,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "v${state.info.version} Available",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    if (state.info.releaseNotes.isNotBlank()) {
                        Text(
                            text = state.info.releaseNotes.lines().take(5).joinToString("\n"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    Button(onClick = { onDownload(state.info.apkUrl) }) {
                        Icon(
                            Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download & Install")
                    }
                }
            }
        }

        is UpdateUiState.Downloading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Downloading update…", style = MaterialTheme.typography.bodyMedium)
            }
        }

        is UpdateUiState.Error -> {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Update check failed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = onCheckNow) {
                    Text("Retry")
                }
            }
        }
    }
}
