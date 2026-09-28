package uk.co.scrying.ui.pulse

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.scrying.*
import uk.co.scrying.ui.components.*
import uk.co.scrying.ui.contribute.ContributeSheet

@Composable
fun PulseScreen(modifier: Modifier, padding: PaddingValues, state: ScryingUiState, vm: ScryingViewModel) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var controls by remember { mutableStateOf(false) }
    val all = (state.nodes + state.observed).distinctBy { it.id }
    val changes = EnvironmentChangeDetector.detect(all)
    val filtered = all.filter { node ->
        (query.isBlank() || node.friendlyName.contains(query, true)) && when (filter) {
            "BLE" -> node.category != TechnologyCategory.NETWORK
            "Wi-Fi" -> node.category == TechnologyCategory.NETWORK
            "Authorised" -> node.ownership in setOf(OwnershipState.MINE, OwnershipState.PERMISSION_GRANTED)
            "New" -> node.firstSeen >= System.currentTimeMillis() - 86_400_000
            "Stale" -> node.lastSeen < System.currentTimeMillis() - 86_400_000
            else -> true
        }
    }
    Box(modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("SCRYING", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("${if (state.bleActive) "BLE on" else "BLE off"} · ${if (state.wifiActive) "Wi-Fi on" else "Wi-Fi off"} · ${all.size} observed · ${all.count { it.ownership == OwnershipState.MINE }} authorised", style = MaterialTheme.typography.labelSmall)
                }
                StatusChip(if (state.bleActive || state.wifiActive) "Scanning" else "Local", if (state.bleActive || state.wifiActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary, onClick = { controls = true })
            }
            if (changes.isNotEmpty()) AssistChip(onClick = {}, label = { Text("What changed: ${changes.first().explanation}") })
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), singleLine = true, label = { Text("Search nearby technology") })
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf("All", "BLE", "Wi-Fi", "Authorised", "New", "Stale").forEachIndexed { index, item ->
                    SegmentedButton(filter == item, { filter = item }, SegmentedButtonDefaults.itemShape(index, 6)) { Text(item, style = MaterialTheme.typography.labelSmall) }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) EmptyState("No observations", "Start a passive scan to see nearby BLE and Wi-Fi observations.")
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 88.dp)) { items(filtered, key = { it.id }) { ObservationRow(it, vm::markMine) } }
            PrivacyNote()
        }
        ScanFab(state.bleActive || state.wifiActive) { if (state.bleActive || state.wifiActive) vm.stopScan() else controls = true }
        if (controls) ContributeSheet(state, vm) { controls = false }
    }
}

@Composable private fun ObservationRow(node: TechnologyNode, markMine: (TechnologyNode) -> Unit) = ScryingCard {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(if (node.category == TechnologyCategory.NETWORK) Icons.Filled.Router else Icons.Filled.Bluetooth, contentDescription = if (node.category == TechnologyCategory.NETWORK) "Wi-Fi observation" else "Bluetooth observation")
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(node.friendlyName, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OwnershipBadge(node.ownership); SignalBars(node.rssi); Text(node.rssi?.let { "$it dBm" } ?: "RSSI unknown", style = MaterialTheme.typography.labelSmall) }
            Text("Seen ${node.observations}× · ${relative(node.lastSeen)}", style = MaterialTheme.typography.labelSmall)
        }
        if (node.ownership == OwnershipState.OBSERVED) TextButton(onClick = { markMine(node) }) { Text("Mark as mine") }
    }
}

private fun relative(time: Long): String { val seconds = ((System.currentTimeMillis() - time) / 1_000).coerceAtLeast(0); return when { seconds < 60 -> "last ${seconds}s ago"; seconds < 3_600 -> "last ${seconds / 60}m ago"; else -> "last ${seconds / 3_600}h ago" } }
