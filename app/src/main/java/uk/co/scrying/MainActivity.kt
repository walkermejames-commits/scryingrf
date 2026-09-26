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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var scanner: ScannerRepository
    private lateinit var store: LocalTechnologyStore
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanner = ScannerRepository(this)
        store = LocalTechnologyStore(this)
        setContent { ScryingApp(scanner, store, ::requestSensingPermissions) }
    }
    private fun requestSensingPermissions() {
        val needed = listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) permissions.launch(needed.toTypedArray())
    }
    override fun onDestroy() { scanner.stop(); super.onDestroy() }
}

@Composable
private fun ScryingApp(scanner: ScannerRepository, store: LocalTechnologyStore, requestPermissions: () -> Unit) {
    val observed by scanner.observations.collectAsState()
    val stored by store.nodes.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var demoEnabled by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf(0) }
    var goal by remember { mutableStateOf("Build a home camera system") }
    var plan by remember { mutableStateOf<ProjectPlan?>(null) }
    LaunchedEffect(observed) { observed.forEach { scope.launch { store.save(it) } } }
    val allNodes = (stored + observed).distinctBy { it.id }
    val resources = (if (demoEnabled) demoResources() else emptyList()) + allNodes.filter { it.ownership == OwnershipState.MINE || it.ownership == OwnershipState.PERMISSION_GRANTED }
    MaterialTheme {
        Scaffold(bottomBar = { NavigationBar { listOf("Home", "Environment", "Resources", "Build").forEachIndexed { i, label -> NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = {}, label = { Text(label) }) } } }) { pad ->
            Column(Modifier.padding(pad).padding(16.dp)) {
                when (tab) {
                    0 -> Home(scanner, requestPermissions, allNodes)
                    1 -> Environment(allNodes) { node -> scope.launch { store.save(node.copy(ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE)) } }
                    2 -> Resources(resources, { name, category, capabilities -> scope.launch { store.save(TechnologyNode(friendlyName = name, category = category, ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE, capabilities = capabilities)) } }, demoEnabled) { demoEnabled = !demoEnabled }
                    3 -> Build(goal, { goal = it }, plan) { plan = ProjectCompiler.compile(goal, resources.filter { it.ownership == OwnershipState.MINE || it.ownership == OwnershipState.PERMISSION_GRANTED }) }
                }
            }
        }
    }
}

@Composable private fun Home(scanner: ScannerRepository, permissions: () -> Unit, observed: List<TechnologyNode>) {
    Text("SCRYING", style = MaterialTheme.typography.headlineLarge); Text("Local-first technology environment intelligence")
    Spacer(Modifier.height(12.dp)); Row { Button(onClick = permissions) { Text("Enable sensing") }; Spacer(Modifier.width(8.dp)); Button(onClick = { scanner.scanBle(); scanner.scanWifi() }) { Text("Scan now") } }
    Spacer(Modifier.height(16.dp)); Text("This phone can contribute: BLE sensor, Wi-Fi sensor, location sensor (permission dependent).")
    Text("${observed.size} observed devices in this session. Observed is not authorised for use.")
    val changes = EnvironmentChangeDetector.detect(observed)
    if (changes.isNotEmpty()) Text("What changed: ${changes.size} new observation${if (changes.size == 1) "" else "s"} in the last 24 hours.")
}
@Composable private fun Environment(observed: List<TechnologyNode>, makeMine: (TechnologyNode) -> Unit) {
    Text("What’s here?", style = MaterialTheme.typography.headlineMedium); Text("Passive observations only. RSSI is not a distance measurement.")
    LazyColumn { items(observed) { d -> ListItem(headlineContent = { Text(d.friendlyName) }, supportingContent = { Text("Observed ${d.observations} times · RSSI ${d.rssi ?: "unknown"}") }, trailingContent = { TextButton(onClick = { makeMine(d) }) { Text("This is mine") } }); HorizontalDivider() } }
}
@Composable private fun Resources(resources: List<TechnologyNode>, add: (String, TechnologyCategory, Set<String>) -> Unit, demoEnabled: Boolean, toggleDemo: () -> Unit) {
    var name by remember { mutableStateOf("") }; var category by remember { mutableStateOf(TechnologyCategory.COMPUTER) }; var capabilities by remember { mutableStateOf("") }
    Text("My Technology", style = MaterialTheme.typography.headlineMedium); Text("Only explicitly authorised resources are listed.")
    Row { Text("Demo mode"); Switch(checked = demoEnabled, onCheckedChange = { toggleDemo() }) }
    OutlinedTextField(name, { name = it }, label = { Text("Add hardware name") }, modifier = Modifier.fillMaxWidth())
    Row { TechnologyCategory.entries.filter { it != TechnologyCategory.UNKNOWN }.take(4).forEach { type -> FilterChip(selected = category == type, onClick = { category = type }, label = { Text(type.name.lowercase()) }) } }
    OutlinedTextField(capabilities, { capabilities = it }, label = { Text("Capabilities, comma separated") }, modifier = Modifier.fillMaxWidth())
    Button(enabled = name.isNotBlank(), onClick = { add(name.trim(), category, capabilities.split(',').map(String::trim).filter(String::isNotEmpty).toSet()); name = ""; capabilities = "" }) { Text("Add hardware") }
    LazyColumn { items(resources) { d -> ListItem(headlineContent = { Text(d.friendlyName) }, supportingContent = { Text("${d.category} · ${d.capabilities.joinToString()}") }); HorizontalDivider() } }
}
@Composable private fun Build(goal: String, updateGoal: (String) -> Unit, plan: ProjectPlan?, compile: () -> Unit) { Text("Build something", style = MaterialTheme.typography.headlineMedium); OutlinedTextField(goal, updateGoal, label = { Text("Goal") }, modifier = Modifier.fillMaxWidth()); Button(onClick = compile) { Text("Create local plan") }; plan?.let { Spacer(Modifier.height(12.dp)); Text(it.explanation); Text("Assignments", style = MaterialTheme.typography.titleMedium); it.assignments.forEach { a -> Text("• $a") }; if (it.gaps.isNotEmpty()) { Text("Missing", style = MaterialTheme.typography.titleMedium); it.gaps.forEach { g -> Text("• $g") } } } }
private fun demoResources() = listOf(TechnologyNode(friendlyName = "DEMO Workstation", category = TechnologyCategory.COMPUTER, ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE, capabilities = setOf("Linux", "16 GB RAM", "Ethernet")), TechnologyNode(friendlyName = "DEMO Old Android", category = TechnologyCategory.PHONE, ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE, capabilities = setOf("Wi-Fi", "Camera", "BLE")), TechnologyNode(friendlyName = "DEMO Router", category = TechnologyCategory.ROUTER, ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE, capabilities = setOf("Wi-Fi", "Ethernet")))
