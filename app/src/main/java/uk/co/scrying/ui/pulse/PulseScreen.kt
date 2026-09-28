package uk.co.scrying.ui.pulse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.scrying.*
import uk.co.scrying.ui.components.*
import uk.co.scrying.ui.contribute.ContributeSheet
import uk.co.scrying.ui.observe.FieldReadingSheet
import uk.co.scrying.ui.observe.LiveInstrumentGrid

@Composable fun PulseScreen(modifier: Modifier, padding: PaddingValues, state: ScryingUiState, vm: ScryingViewModel) {
    var mode by remember { mutableStateOf("Environment") }; var controls by remember { mutableStateOf(false) }; var reading by remember { mutableStateOf(false) }
    val all = (state.nodes + state.observed).distinctBy { it.id }; val authorised = all.count { it.ownership in setOf(OwnershipState.MINE, OwnershipState.PERMISSION_GRANTED) }
    Box(modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("SCRYING", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("Local environmental instrument", style = MaterialTheme.typography.labelSmall) }; StatusChip(if (state.instrument.running) "Reading" else if (state.bleActive || state.wifiActive) "Observing" else "Idle", if (state.instrument.running || state.bleActive || state.wifiActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary, onClick = { controls = true }) }
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 12.dp)) { listOf("Environment", "Signals", "History").forEachIndexed { index, item -> SegmentedButton(mode == item, { mode = item }, SegmentedButtonDefaults.itemShape(index, 3)) { Text(item) } } }
            when (mode) { "Environment" -> EnvironmentPanel(state, { reading = true }); "Signals" -> SignalsPanel(all, authorised, vm::markMine); else -> HistoryPanel(state.sessions, vm::shareSession) }
        }
        if (controls) ContributeSheet(state, vm) { controls = false }
        if (reading) FieldReadingSheet(state, vm) { reading = false }
    }
}

@Composable private fun EnvironmentPanel(state: ScryingUiState, openReading: () -> Unit) = LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
    item { ScryingCard { Text(if (state.instrument.running) "Reading this environment" else "Take a field reading", style = MaterialTheme.typography.titleLarge); Text(if (state.instrument.running) "Live local sensor data is visible below. Save it when the reading has settled." else "Start a short session, label the place, then compare future visits against its local baseline.", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = openReading, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Icon(Icons.Filled.Science, contentDescription = null); Spacer(Modifier.width(8.dp)); Text(if (state.instrument.running) "View live reading" else "Start field reading") } } }
    item { LiveInstrumentGrid(state.instrument) }
    item { Text("What this means", style = MaterialTheme.typography.titleMedium) }
    item { ScryingCard { Text("Evidence, not verdicts", fontWeight = FontWeight.SemiBold); Text("Magnetic readings describe magnetic field, not RF. Relative sound is not calibrated decibels. New radio observations are unlabelled broadcasts, not people or threats.", style = MaterialTheme.typography.bodySmall) } }
    item { PrivacyNote("All sessions are stored locally. Public sharing remains optional and aggregate-only.") }
}
@Composable private fun SignalsPanel(nodes: List<TechnologyNode>, authorised: Int, markMine: (TechnologyNode) -> Unit) = LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
    item { ScryingCard { Text("Signals in this observation", style = MaterialTheme.typography.titleLarge); Text("${nodes.size} observed · $authorised authorised. A signal is evidence of a broadcast, not an identity, distance, or intent.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    if (nodes.isEmpty()) item { EmptyState("No signals recorded", "Use sensing controls to begin a foreground BLE or Wi-Fi observation.") }
    items(nodes, key = { it.id }) { node -> ScryingCard { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (node.category == TechnologyCategory.NETWORK) Icons.Filled.Router else Icons.Filled.Bluetooth, contentDescription = "Observed signal"); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(node.friendlyName, fontWeight = FontWeight.SemiBold); Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { OwnershipBadge(node.ownership); SignalBars(node.rssi); Text(node.rssi?.let { "$it dBm" } ?: "RSSI unavailable", style = MaterialTheme.typography.labelSmall) }; Text("Seen ${node.observations}× · ${relative(node.lastSeen)}", style = MaterialTheme.typography.labelSmall) }; if (node.ownership == OwnershipState.OBSERVED) TextButton({ markMine(node) }) { Text("Mark as mine") } } } }
}
@Composable private fun HistoryPanel(sessions: List<EnvironmentalSession>, share: (EnvironmentalSession) -> Unit) = LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
    item { ScryingCard { Text("Local baselines", style = MaterialTheme.typography.titleLarge); Text("Saved sessions compare only with your prior readings at the same named place.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    if (sessions.isEmpty()) item { EmptyState("No saved readings", "Take a field reading at a named place to establish a local baseline.") }
    items(sessions, key = { it.id }) { session -> val prior = sessions.filter { it.id != session.id }; val findings = EnvironmentalAnalysis.analyse(session, prior); ScryingCard { Text(session.placeLabel, style = MaterialTheme.typography.titleMedium); Text("${session.samples} samples · ${relative(session.endedAt)}", style = MaterialTheme.typography.labelSmall); findings.take(3).forEach { finding -> Text("${finding.title}: ${finding.detail}", style = MaterialTheme.typography.bodySmall) }; TextButton(onClick = { share(session) }) { Text("Share safe summary") } } }
}
private fun relative(time: Long): String { val seconds = ((System.currentTimeMillis() - time) / 1_000).coerceAtLeast(0); return when { seconds < 60 -> "just now"; seconds < 3_600 -> "${seconds / 60} min ago"; seconds < 86_400 -> "${seconds / 3_600} h ago"; else -> "${seconds / 86_400} d ago" } }
