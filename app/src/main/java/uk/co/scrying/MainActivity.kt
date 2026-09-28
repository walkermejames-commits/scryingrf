@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package uk.co.scrying

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.MarkerOptions

private val ScryingBlue = Color(0xFF2457D6)
private val ScryingTeal = Color(0xFF006C66)

class MainActivity : ComponentActivity() {
    private lateinit var scanner: ScannerRepository; private lateinit var store: LocalTechnologyStore; private lateinit var survey: SurveyRepository; private lateinit var publicHub: PublicHubRepository
    private val permissionRequest = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); MapLibre.getInstance(this); scanner = ScannerRepository(this); store = LocalTechnologyStore(this); survey = SurveyRepository(this); publicHub = PublicHubRepository(this); setContent { ScryingApp(scanner, store, survey, publicHub, ::requestSensingPermissions, ::profile) } }
    private fun profile() = DeviceProfiler(this).profile()
    private fun requestSensingPermissions() {
        val needed = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) permissionRequest.launch(needed.toTypedArray())
    }
    override fun onDestroy() { scanner.stop(); survey.stop(); super.onDestroy() }
}

@Composable private fun ScryingApp(scanner: ScannerRepository, store: LocalTechnologyStore, survey: SurveyRepository, publicHub: PublicHubRepository, requestPermissions: () -> Unit, profileProvider: () -> DeviceProfile) {
    val scope = rememberCoroutineScope(); val observed by scanner.observations.collectAsState(); val nodes by store.nodes.collectAsState(emptyList()); val people by store.people.collectAsState(emptyList()); val agreements by store.agreements.collectAsState(emptyList()); val surveyPoints by survey.points.collectAsState(); val surveyStatus by survey.status.collectAsState()
    var profile by remember { mutableStateOf(profileProvider()) }; var tab by remember { mutableIntStateOf(0) }; var active by remember { mutableStateOf(setOf(ContributionType.DEVICE_HEALTH)) }; var goal by remember { mutableStateOf("Build a connected workshop monitor") }; var plan by remember { mutableStateOf<ProjectPlan?>(null) }
    LaunchedEffect(observed) { observed.forEach { scope.launch { store.save(it) } } }
    val allNodes = (nodes + observed).distinctBy { it.id }; val resources = allNodes.filter { it.ownership == OwnershipState.MINE || it.ownership == OwnershipState.PERMISSION_GRANTED }
    MaterialTheme(colorScheme = lightColorScheme(primary = ScryingBlue, secondary = ScryingTeal)) {
        Scaffold(topBar = { TopAppBar(title = { Column { Text("SCRYING", fontWeight = FontWeight.Black); Text("People, devices, and shared systems", style = MaterialTheme.typography.labelSmall) } }) }, bottomBar = { NavigationBar { listOf("Home", "Contribute", "Map", "People", "Nearby", "Build").forEachIndexed { index, text -> NavigationBarItem(tab == index, { tab = index }, icon = { Text(listOf("⌂", "◉", "⌖", "♧", "⌁", "✦")[index]) }, label = { Text(text) }) } } }) { pad ->
            Box(Modifier.padding(pad)) { when (tab) {
                0 -> Home(profile, people, resources, active, { profile = profileProvider() }, { tab = it })
                1 -> Contributions(profile, active, requestPermissions, { type, enabled -> active = if (enabled) active + type else active - type; when (type) { ContributionType.BLE_OBSERVER -> if (enabled) scanner.scanBle() else scanner.stop(); ContributionType.WIFI_OBSERVER -> if (enabled) scanner.scanWifi(); else -> Unit } })
                2 -> SurveyMap(surveyPoints, surveyStatus, active, publicHub, requestPermissions, survey::start, survey::stop)
                3 -> PeopleScreen(people, resources, agreements, { person -> scope.launch { store.save(person) } }, { agreement -> scope.launch { store.save(agreement) } }, { tab = 5 })
                4 -> NearbyScreen(allNodes, { node -> scope.launch { store.save(node.copy(ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE)) } }, requestPermissions, { scanner.scanBle(); scanner.scanWifi() })
                else -> BuildScreen(goal, { goal = it }, plan) { plan = ProjectCompiler.compile(goal, resources) }
            } }
        }
    }
}

@Composable private fun Home(profile: DeviceProfile, people: List<Person>, resources: List<TechnologyNode>, active: Set<ContributionType>, refresh: () -> Unit, go: (Int) -> Unit) = Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text("Your connected workspace", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Start with this phone. Add people and authorised devices before creating a shared system.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(profile.deviceName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(profile.summary); Text("${profile.capabilities.count { it.state == CapabilityState.AVAILABLE }} capabilities available", style = MaterialTheme.typography.labelLarge); OutlinedButton(refresh) { Text("Refresh device profile") } } }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { Summary("People", people.size, Modifier.weight(1f)) { go(3) }; Summary("Devices", resources.size + 1, Modifier.weight(1f)) { go(3) }; Summary("Active", active.size, Modifier.weight(1f)) { go(1) } }
    Text("Start here", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    ActionCard("1. Review this phone", "See real hardware, sensor, permission, and connectivity states.") { go(1) }
    ActionCard("2. Add people", "Record who owns or is permitted to use devices.") { go(3) }
    ActionCard("3. Run an active survey", "Map this phone's own location fixes before choosing whether to share an aggregate.") { go(2) }
    ActionCard("4. Build a shared system", "Use only explicitly authorised resources.") { go(5) }
}
@Composable private fun Summary(label: String, count: Int, modifier: Modifier, tap: () -> Unit) = Card(onClick = tap, modifier = modifier) { Column(Modifier.padding(12.dp)) { Text(count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelMedium) } }
@Composable private fun ActionCard(title: String, body: String, action: () -> Unit) = Card(onClick = action, shape = RoundedCornerShape(18.dp)) { ListItem(headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) }, supportingContent = { Text(body) }, trailingContent = { Text("›", style = MaterialTheme.typography.headlineMedium) }) }

@Composable private fun Contributions(profile: DeviceProfile, active: Set<ContributionType>, requestPermissions: () -> Unit, toggle: (ContributionType, Boolean) -> Unit) = Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("This device can contribute", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Each contribution is local and opt-in. Starting an observer never grants access to another device.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    ContributionCard("Device health", "Battery and connection status are available locally.", ContributionType.DEVICE_HEALTH, active, toggle, true)
    ContributionCard("BLE observer", "Passively records BLE adverts on this phone after Android permission is granted.", ContributionType.BLE_OBSERVER, active, toggle, profile.capabilities.any { it.title == "Bluetooth / BLE" && it.state != CapabilityState.UNSUPPORTED })
    ContributionCard("Wi-Fi observer", "Uses Android-provided Wi-Fi scan results, subject to OS restrictions and permission.", ContributionType.WIFI_OBSERVER, active, toggle, profile.capabilities.any { it.title == "Network" })
    Button(requestPermissions, modifier = Modifier.fillMaxWidth()) { Text("Review sensing permissions") }
    Text("Hardware profile", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    profile.capabilities.forEach { CapabilityCard(it) }
}
@Composable private fun ContributionCard(title: String, detail: String, type: ContributionType, active: Set<ContributionType>, toggle: (ContributionType, Boolean) -> Unit, supported: Boolean) = Card(shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(if (supported) detail else "Unavailable on this device", style = MaterialTheme.typography.bodySmall) }; Switch(checked = type in active, enabled = supported && type != ContributionType.DEVICE_HEALTH, onCheckedChange = { toggle(type, it) }) } }
@Composable private fun CapabilityCard(cap: DeviceCapability) = Card(shape = RoundedCornerShape(16.dp)) { ListItem(overlineContent = { Text(cap.state.name.replace('_', ' ')) }, headlineContent = { Text(cap.title) }, supportingContent = { Text(cap.detail) }) }

@Composable private fun SurveyMap(points: List<SurveyPoint>, status: String, active: Set<ContributionType>, hub: PublicHubRepository, permissions: () -> Unit, start: () -> Unit, stop: () -> Unit) {
    val scope = rememberCoroutineScope()
    val publicSharing by hub.enabled.collectAsState()
    val hubStatus by hub.status.collectAsState()
    val insights by hub.insights.collectAsState()
    var category by remember { mutableStateOf(PublicReportCategory.WIFI_POSTURE) }
    val latest = points.lastOrNull()
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Active survey map", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(status, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(permissions) { Text("Location permission") }
            Button(start) { Text("Start survey") }
            OutlinedButton(stop) { Text("Stop") }
        }
        Text("${points.size} points · points with accuracy worse than 500 m are discarded. RSSI is never shown as a precise distance.", style = MaterialTheme.typography.bodySmall)
        AndroidView(
            factory = { context -> MapView(context).apply {
                onCreate(null)
                getMapAsync { map -> map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) }
            } },
            update = { view -> view.getMapAsync { map -> map.style?.let {
                map.clear()
                points.forEach { point -> map.addMarker(MarkerOptions().position(LatLng(point.latitude, point.longitude)).title("±${point.accuracyMetres.toInt()} m")) }
                points.lastOrNull()?.let { point -> map.cameraPosition = CameraPosition.Builder().target(LatLng(point.latitude, point.longitude)).zoom(15.0).build() }
            } } },
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Public community context", fontWeight = FontWeight.Bold)
                Text("Optional aggregate-only sharing. It never uploads precise coordinates, network names, radio identifiers, contacts, audio, or images.", style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Enable public sharing", Modifier.weight(1f)); Switch(publicSharing, hub::setEnabled) }
                if (publicSharing) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { PublicReportCategory.entries.forEach { choice -> FilterChip(choice == category, { category = choice }, label = { Text(choice.label) }) } }
                    Text("Your report uses an approximately 0.1° grid cell, six-hour bucket, and rotating token. It appears only in aggregates of five or more contributors.", style = MaterialTheme.typography.bodySmall)
                    if (category.contribution !in active) Text("Start the matching local observer in Contribute before sharing its aggregate summary.", style = MaterialTheme.typography.bodySmall)
                    Button(enabled = latest != null && category.contribution in active, onClick = { latest?.let { point -> scope.launch { hub.submit(category, point) } } }, modifier = Modifier.fillMaxWidth()) { Text("Share latest aggregate report") }
                    OutlinedButton(enabled = latest != null, onClick = { latest?.let { point -> scope.launch { hub.loadInsights(point) } } }, modifier = Modifier.fillMaxWidth()) { Text("Check public context") }
                    Text(hubStatus, style = MaterialTheme.typography.bodySmall)
                    Text(insights, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable private fun PeopleScreen(people: List<Person>, resources: List<TechnologyNode>, agreements: List<SharingAgreement>, save: (Person) -> Unit, saveAgreement: (SharingAgreement) -> Unit, build: () -> Unit) { var adding by remember { mutableStateOf(false) }; var selected by remember { mutableStateOf<Person?>(null) }; Column(Modifier.fillMaxSize().padding(20.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("People & authorised devices", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Choose exactly what each invited person may receive after pairing.", color = MaterialTheme.colorScheme.onSurfaceVariant) }; FilledTonalButton({ adding = true }) { Text("Add person") } }; Spacer(Modifier.height(12.dp)); if (people.isEmpty()) Empty("No people added", "Add yourself, household members, or collaborators before pairing devices.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) { items(people, key = { it.id }) { p -> val count = agreements.count { it.personId == p.id && it.enabled }; Card(onClick = { selected = p }, shape = RoundedCornerShape(18.dp)) { ListItem(headlineContent = { Text(p.displayName) }, supportingContent = { Text(p.note.ifBlank { "No note added" }) }, trailingContent = { Text("$count shared") }) } } }; Text("Authorised devices: ${resources.size}"); Button(build, modifier = Modifier.fillMaxWidth()) { Text("Plan a shared system") } }; if (adding) AddPerson({ adding = false }) { save(it); adding = false }; selected?.let { person -> SharingDialog(person, agreements.filter { it.personId == person.id }, { selected = null }, saveAgreement) } }
@Composable private fun AddPerson(dismiss: () -> Unit, save: (Person) -> Unit) { var name by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }; AlertDialog(dismiss, title = { Text("Add a person") }, text = { Column { Text("This is a local record. It does not contact or track anyone.", style = MaterialTheme.typography.bodySmall); OutlinedTextField(name, { name = it }, label = { Text("Name") }); OutlinedTextField(note, { note = it }, label = { Text("Permission or relationship note") }) } }, confirmButton = { Button(enabled = name.isNotBlank(), onClick = { save(Person(displayName = name.trim(), note = note.trim())) }) { Text("Save") } }, dismissButton = { TextButton(dismiss) { Text("Cancel") } }) }
@Composable private fun SharingDialog(person: Person, existing: List<SharingAgreement>, dismiss: () -> Unit, save: (SharingAgreement) -> Unit) { AlertDialog(onDismissRequest = dismiss, title = { Text("Sharing with ${person.displayName}") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("These local consent settings are revocable at any time. Data is not transmitted until a future authenticated pairing is approved.", style = MaterialTheme.typography.bodySmall); listOf(ContributionType.DEVICE_HEALTH, ContributionType.BLE_OBSERVER, ContributionType.WIFI_OBSERVER).forEach { type -> val enabled = existing.firstOrNull { it.contribution == type }?.enabled ?: false; Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(type.name.replace('_', ' ').lowercase(), modifier = Modifier.weight(1f)); Switch(enabled, { save(SharingAgreement(person.id, type, it)) }) } } } }, confirmButton = { TextButton(dismiss) { Text("Done") } }) }

@Composable private fun NearbyScreen(nodes: List<TechnologyNode>, makeMine: (TechnologyNode) -> Unit, permissions: () -> Unit, scan: () -> Unit) = Column(Modifier.fillMaxSize().padding(20.dp)) { Text("Nearby technology", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Secondary context only. Observed equipment is never a shared resource until you explicitly confirm permission.", color = MaterialTheme.colorScheme.onSurfaceVariant); Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 12.dp)) { OutlinedButton(permissions) { Text("Permissions") }; Button(scan) { Text("Observe nearby") } }; if (nodes.isEmpty()) Empty("No observations", "Use Observe nearby after granting sensing permissions.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(nodes, key = { it.id }) { n -> Card(shape = RoundedCornerShape(18.dp)) { ListItem(overlineContent = { Text(if (n.ownership == OwnershipState.MINE) "AUTHORISED" else "OBSERVED") }, headlineContent = { Text(n.friendlyName) }, supportingContent = { Text("Seen ${n.observations} times · signal ${n.rssi ?: "unknown"}") }, trailingContent = { if (n.ownership != OwnershipState.MINE) TextButton({ makeMine(n) }) { Text("This is mine") } }) } } } }
@Composable private fun Empty(title: String, body: String) = Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable private fun BuildScreen(goal: String, edit: (String) -> Unit, plan: ProjectPlan?, compile: () -> Unit) = Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Shared system planner", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Plans are suggestions only. They use resources explicitly marked as yours or permitted.", color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedTextField(goal, edit, label = { Text("What do you want people and devices to do together?") }, modifier = Modifier.fillMaxWidth(), minLines = 2); Button(compile, modifier = Modifier.fillMaxWidth()) { Text("Create local plan") }; plan?.let { Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(18.dp)) { Text("Proposed plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(it.explanation); Text("Assignments", fontWeight = FontWeight.Bold); it.assignments.forEach { a -> Text("• $a") }; if (it.gaps.isNotEmpty()) { Text("Still needed", fontWeight = FontWeight.Bold); it.gaps.forEach { g -> Text("• $g") } } } } } }
