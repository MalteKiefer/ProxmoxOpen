package de.kiefer_networks.proxmoxopen.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.kiefer_networks.proxmoxopen.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // App header
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("PX", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Text("ProxMoxOpen", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text("v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Text("Native Android client for Proxmox VE", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                }
            }

            // Developer
            SectionTitle("Developer")
            InfoCard {
                InfoRow(Icons.Outlined.Person, "Malte Kiefer", subtitle = "Lead Developer")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Outlined.Language, "kiefer-networks.de", "Website") { openUrl("https://kiefer-networks.de") }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Outlined.Email, "malte.kiefer@kiefer-networks.de", "Email") { openUrl("mailto:malte@kiefer-networks.de") }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Outlined.Code, "github.com/MalteKiefer", "GitHub") { openUrl("https://github.com/MalteKiefer") }
            }

            // Donate
            SectionTitle("Support Development")
            InfoCard {
                LinkRow(Icons.Outlined.Favorite, "Liberapay", "Donate via Liberapay") { openUrl("https://liberapay.com/beli3ver") }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Outlined.Favorite, "PayPal", "Donate via PayPal") { openUrl("https://paypal.me/maltekiefer1987") }
            }
            Text(
                "Your donation helps keep this project free, open-source, and independent. Thank you!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Project info
            SectionTitle("Project")
            InfoCard {
                InfoRow(Icons.Outlined.Shield, "GPL-3.0-or-later", subtitle = "License")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Outlined.Code, "Source Code", "github.com/MalteKiefer/ProxmoxOpen") { openUrl("https://github.com/MalteKiefer/ProxmoxOpen") }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                InfoRow(Icons.Outlined.Info, "Kotlin + Jetpack Compose", subtitle = "Built with")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                InfoRow(Icons.Outlined.Info, "Material 3", subtitle = "Design system")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                LinkRow(Icons.Outlined.Code, "Termux Terminal Emulator", "Terminal rendering (GPL-3.0)") { openUrl("https://github.com/termux/termux-app") }
            }

            // Build info
            SectionTitle("Build")
            InfoCard {
                InfoRow(Icons.Outlined.Info, BuildConfig.VERSION_NAME, subtitle = "Version")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                InfoRow(Icons.Outlined.Info, BuildConfig.VERSION_CODE.toString(), subtitle = "Build number")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                InfoRow(Icons.Outlined.Info, BuildConfig.APPLICATION_ID, subtitle = "Package")
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                InfoRow(Icons.Outlined.Info, if (BuildConfig.DEBUG) "Debug" else "Release", subtitle = "Build type")
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column { content() }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, value: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 16.dp)) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, value: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
