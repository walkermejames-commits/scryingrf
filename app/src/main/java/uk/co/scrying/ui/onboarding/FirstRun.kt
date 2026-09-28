package uk.co.scrying.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.scrying.DeviceProfile
import uk.co.scrying.ui.components.OwnershipBadge
import uk.co.scrying.ui.components.ScryingCard
import uk.co.scrying.OwnershipState

@Composable fun FirstRun(profile: DeviceProfile, finish: () -> Unit) { var page by remember { mutableIntStateOf(0) }; val title = listOf("This phone is node zero.", "Observed is not yours.", "You turn sensing on.")[page]; val body = listOf("${profile.summary}. It begins as your local instrument.", "Nearby equipment stays observed until you choose Mark as mine or have permission.", "BLE and Wi-Fi are off until you start them. Android requires location for Wi-Fi/BLE scans; Scrying hashes identifiers.")[page]; Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.SpaceBetween) { Column(verticalArrangement = Arrangement.spacedBy(18.dp)) { Text("SCRYING", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(title, style = MaterialTheme.typography.displaySmall); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant); if (page == 0) Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { profile.capabilities.take(4).forEach { AssistChip(onClick = {}, label = { Text(it.title) }) } }; if (page == 1) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OwnershipBadge(OwnershipState.OBSERVED); OwnershipBadge(OwnershipState.MINE) }; if (page == 2) ScryingCard { Text("BLE observer     Off"); Text("Wi-Fi observer    Off") } }; Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { TextButton(onClick = finish) { Text("Skip") }; Button(onClick = { if (page == 2) finish() else page++ }) { Text(if (page == 2) "Open Pulse" else "Continue") } } } }
