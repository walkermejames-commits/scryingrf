@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package uk.co.scrying.ui.atlas

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import uk.co.scrying.*
import uk.co.scrying.ui.components.PrivacyNote
import uk.co.scrying.ui.components.ScryingCard

@Composable fun AtlasScreen(modifier: Modifier, padding: PaddingValues, state: ScryingUiState, vm: ScryingViewModel) {
    var community by remember { mutableStateOf(false) }
    Box(modifier.fillMaxSize().padding(padding)) {
        SurveyMap(state.surveyPoints)
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface.copy(alpha = .88f)) {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Atlas", style = MaterialTheme.typography.titleLarge)
                Text("Basemap: demo", style = MaterialTheme.typography.labelSmall)
            }
        }
        Surface(Modifier.align(androidx.compose.ui.Alignment.BottomCenter).fillMaxWidth(), shape = MaterialTheme.shapes.medium, tonalElevation = 8.dp) {
            Column(Modifier.padding(16.dp)) {
                Text("${state.surveyPoints.size} points · ${state.surveyPoints.lastOrNull()?.let { "±${it.accuracyMetres.toInt()} m" } ?: "No fix"}", style = MaterialTheme.typography.labelSmall)
                Text(state.surveyStatus, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = vm::startSurvey) { Text("Start survey") }
                    OutlinedButton(onClick = vm::stopSurvey) { Text("Stop") }
                    TextButton(onClick = { community = true }) { Text("Community context") }
                }
            }
        }
        if (community) CommunitySheet(state, vm) { community = false }
    }
}
@Composable private fun SurveyMap(points: List<SurveyPoint>) { val lifecycle = LocalLifecycleOwner.current; val context = LocalContext.current; val mapView = remember { MapView(context) }; DisposableEffect(lifecycle, mapView) { mapView.onCreate(null); val observer = object : DefaultLifecycleObserver { override fun onStart(owner: LifecycleOwner) = mapView.onStart(); override fun onResume(owner: LifecycleOwner) = mapView.onResume(); override fun onPause(owner: LifecycleOwner) = mapView.onPause(); override fun onStop(owner: LifecycleOwner) = mapView.onStop(); override fun onDestroy(owner: LifecycleOwner) = mapView.onDestroy() }; lifecycle.lifecycle.addObserver(observer); onDispose { lifecycle.lifecycle.removeObserver(observer); mapView.onPause(); mapView.onStop(); mapView.onDestroy() } }; AndroidView(factory = { mapView }, update = { view -> view.getMapAsync { map -> if (map.style == null) map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")); map.style?.let { map.clear(); points.forEach { point -> map.addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)).title("Survey fix ±${point.accuracyMetres.toInt()} m")) }; points.lastOrNull()?.let { point -> map.cameraPosition = CameraPosition.Builder().target(LatLng(point.latitude, point.longitude)).zoom(15.0).build() } } } }, modifier = Modifier.fillMaxSize()) }
@Composable private fun CommunitySheet(state: ScryingUiState, vm: ScryingViewModel, dismiss: () -> Unit) { val enabled by vm.hub.enabled.collectAsState(); val status by vm.hub.status.collectAsState(); val insights by vm.hub.insights.collectAsState(); val scope = rememberCoroutineScope(); var category by remember { mutableStateOf(PublicReportCategory.WIFI_POSTURE) }; val point = state.surveyPoints.lastOrNull(); ModalBottomSheet(onDismissRequest = dismiss) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Community context", style = MaterialTheme.typography.titleLarge); Text("Optional aggregate-only sharing. A report includes a coarse cell, six-hour bucket, and rotating token—never raw coordinates or radio identifiers.", style = MaterialTheme.typography.bodySmall); Row { Text("Enable public sharing", Modifier.weight(1f)); Switch(enabled, vm.hub::setEnabled) }; if (enabled) { PublicReportCategory.entries.forEach { choice -> FilterChip(category == choice, { category = choice }, label = { Text(choice.label) }) }; Button(enabled = point != null && ((category == PublicReportCategory.BLE_PATTERN && state.bleActive) || (category == PublicReportCategory.WIFI_POSTURE && state.wifiActive)), onClick = { point?.let { scope.launch { vm.hub.submit(category, it) } } }) { Text("Share aggregate report") }; OutlinedButton(enabled = point != null, onClick = { point?.let { scope.launch { vm.hub.loadInsights(it) } } }) { Text("Check public context") }; Text(status, style = MaterialTheme.typography.labelSmall); Text(insights, style = MaterialTheme.typography.labelSmall) }; PrivacyNote("Sharing stays off until you enable it and choose Share.") } } }
