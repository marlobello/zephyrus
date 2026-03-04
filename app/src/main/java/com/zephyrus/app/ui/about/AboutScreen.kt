package com.zephyrus.app.ui.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zephyrus.app.R
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

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
