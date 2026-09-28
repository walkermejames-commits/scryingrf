@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package uk.co.scrying.ui.contribute

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.co.scrying.ScryingUiState
import uk.co.scrying.ScryingViewModel
import uk.co.scrying.ui.components.PrivacyNote

@Composable fun ContributeSheet(state: ScryingUiState, vm: ScryingViewModel, dismiss: () -> Unit) = ModalBottomSheet(onDismissRequest = dismiss) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text("Sensing controls", style = MaterialTheme.typography.titleLarge); Text("Choose what this phone contributes into Pulse. Scans are passive and stop explicitly.", color = MaterialTheme.colorScheme.onSurfaceVariant); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("BLE observer"); Text(if (state.bleActive) "Running" else "Off", style = MaterialTheme.typography.labelSmall) }; Switch(state.bleActive, onCheckedChange = { if (it) vm.requestBleScan() else vm.stopScan() }) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Wi-Fi observer"); Text(if (state.wifiActive) "Running" else "Off", style = MaterialTheme.typography.labelSmall) }; Switch(state.wifiActive, onCheckedChange = { if (it) vm.requestWifiScan() else vm.stopScan() }) }; TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text("Done") }; PrivacyNote() } }
