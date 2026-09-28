@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package uk.co.scrying.ui.observe

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.scrying.*
import uk.co.scrying.ui.components.PrivacyNote
import uk.co.scrying.ui.components.ScryingCard

@Composable fun FieldReadingSheet(state: ScryingUiState, vm: ScryingViewModel, dismiss: () -> Unit) {
    var place by remember { mutableStateOf("Home") }
    var note by remember { mutableStateOf("") }
    var microphone by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = dismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (state.instrument.running) "Field reading in progress" else "Take a field reading", style = MaterialTheme.typography.titleLarge)
            Text(if (state.instrument.running) state.instrument.status else "Measure this phone's available sensors for a short, local observation session.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!state.instrument.running) {
                OutlinedTextField(place, { place = it }, label = { Text("Place or context") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text("Relative sound"); Text("Uses microphone only during this session; not an audio recording.", style = MaterialTheme.typography.labelSmall) }; Switch(microphone, { microphone = it }) }
                Button(onClick = { vm.startInstrumentSession(microphone) }, modifier = Modifier.fillMaxWidth()) { Text("Start local reading") }
            } else {
                LiveInstrumentGrid(state.instrument)
                OutlinedTextField(note, { note = it }, label = { Text("Optional note") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { vm.finishInstrumentSession(place, note); dismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Save reading") }
            }
            PrivacyNote("Readings stay on this phone. Nothing is shared unless you choose a separate aggregate contribution.")
        }
    }
}

@Composable fun LiveInstrumentGrid(instrument: InstrumentState) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    InstrumentLine("Motion", instrument.accelerationMs2?.let { "%.2f m/s²".format(it) } ?: "Waiting for sensor")
    InstrumentLine("Magnetic field", instrument.magneticUt?.let { "%.1f µT".format(it) } ?: "Unavailable")
    InstrumentLine("Light", instrument.lightLux?.let { "%.0f lux".format(it) } ?: "Unavailable")
    InstrumentLine("Pressure", instrument.pressureHpa?.let { "%.1f hPa".format(it) } ?: "Unavailable")
    InstrumentLine("Relative sound", instrument.relativeSoundDbfs?.let { "%.1f dBFS".format(it) } ?: if (instrument.microphoneEnabled) "Listening" else "Not enabled")
}
@Composable private fun InstrumentLine(label: String, value: String) = ScryingCard { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, fontWeight = FontWeight.SemiBold); Text(value) } }
