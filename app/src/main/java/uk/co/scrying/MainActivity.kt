@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package uk.co.scrying

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

private val ScryingBlue = Color(0xFF2457D6)
private val ScryingTeal = Color(0xFF006C66)
private val ScryingInk = Color(0xFF172033)

class MainActivity : ComponentActivity() {
    private lateinit var scanner: ScannerRepository
    private lateinit var store: LocalTechnologyStore
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); scanner = ScannerRepository(this); store = LocalTechnologyStore(this)
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
    val observed by scanner.observations.collectAsState(); val stored by store.nodes.collectAsState(initial = emptyList()); val scope = rememberCoroutineScope()
    var demoEnabled by remember { mutableStateOf(true) }; var destination by remember { mutableIntStateOf(0) }; var goal by remember { mutableStateOf("Build a home camera system") }; var plan by remember { mutableStateOf<ProjectPlan?>(null) }; var scanning by remember { mutableStateOf(false) }
    LaunchedEffect(observed) { observed.forEach { scope.launch { store.save(it) } } }
    val allNodes = (stored + observed).distinctBy { it.id }; val owned = allNodes.filter { it.ownership == OwnershipState.MINE || it.ownership == OwnershipState.PERMISSION_GRANTED }; val resources = (if (demoEnabled) demoResources() else emptyList()) + owned
    MaterialTheme(colorScheme = lightColorScheme(primary = ScryingBlue, secondary = ScryingTeal, surface = Color(0xFFF9F9FF), onSurface = ScryingInk)) {
        Scaffold(topBar = { TopAppBar(title = { Column { Text("SCRYING", fontWeight = FontWeight.Black); Text("Local technology intelligence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }) }, bottomBar = { NavigationBar { listOf("Home", "Nearby", "My tech", "Build").forEachIndexed { i, label -> NavigationBarItem(destination == i, { destination = i }, { Text(listOf("◉", "⌁", "▣", "✦")[i]) }, label = { Text(label) }) } } }) { padding ->
            Box(Modifier.padding(padding)) { when (destination) {
                0 -> HomeScreen(allNodes, resources, scanning, requestPermissions, { scanner.scanBle(); scanner.scanWifi(); scanning = true }, { scanner.stop(); scanning = false }, { destination = it })
                1 -> NearbyScreen(allNodes) { node -> scope.launch { store.save(node.copy(ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE)) } }
                2 -> ResourcesScreen(resources, demoEnabled, { demoEnabled = !demoEnabled }) { name, category, caps -> scope.launch { store.save(TechnologyNode(friendlyName = name, category = category, ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE, capabilities = caps)) } }
                else -> BuildScreen(goal, { goal = it }, plan) { plan = ProjectCompiler.compile(goal, resources.filter { it.ownership == OwnershipState.MINE || it.ownership == OwnershipState.PERMISSION_GRANTED }) }
            } }
        }
    }
}

@Composable private fun HomeScreen(nodes: List<TechnologyNode>, resources: List<TechnologyNode>, scanning: Boolean, requestPermissions: () -> Unit, onScan: () -> Unit, onStop: () -> Unit, go: (Int) -> Unit) {
    val changes = EnvironmentChangeDetector.detect(nodes)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Make more of what you already own.", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Observe nearby technology, confirm what is yours, then build practical systems from it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(if (scanning) "Scanning nearby signals" else "Ready to observe", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); if (scanning) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp) }
            Text(if (scanning) "BLE scanning remains active until you stop it. Wi-Fi results follow Android’s scan schedule." else "Scrying only performs passive, permission-gated observations.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(requestPermissions) { Text("Enable sensing") }; if (scanning) OutlinedButton(onStop) { Text("Stop") } else Button(onScan) { Text("Scan now") } }
        } }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) { MetricCard("Nearby", nodes.size.toString(), "passive observations", Modifier.weight(1f)) { go(1) }; MetricCard("Available", resources.size.toString(), "authorised resources", Modifier.weight(1f)) { go(2) } }
        SectionTitle("What changed")
        if (changes.isEmpty()) EmptyCard("No evidence-based changes yet", "Run a scan to start building a local history.") else changes.take(3).forEach { ChangeCard(it) }
        SectionTitle("This phone can contribute")
        Card(shape = RoundedCornerShape(18.dp)) { Text("BLE sensor  •  Wi-Fi observer  •  Location sensor when permitted\n\nMagnetometer, Wi-Fi RTT, and other capabilities will appear only when supported and enabled.", Modifier.padding(16.dp)) }
    }
}
@Composable private fun MetricCard(label: String, value: String, detail: String, modifier: Modifier, click: () -> Unit) = Card(modifier.clickable(onClick = click), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(label, fontWeight = FontWeight.SemiBold); Text(detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
@Composable private fun EmptyCard(title: String, body: String) = Card(shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun ChangeCard(change: EnvironmentChange) = Card(shape = RoundedCornerShape(18.dp)) { ListItem(overlineContent = { Text(change.type.name.replace('_', ' ')) }, headlineContent = { Text(change.node.friendlyName) }, supportingContent = { Text(change.explanation) }) }

@Composable private fun NearbyScreen(nodes: List<TechnologyNode>, onMakeMine: (TechnologyNode) -> Unit) { Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) { Spacer(Modifier.height(12.dp)); Text("Nearby technology", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Observed does not mean available to use.", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(12.dp)); if (nodes.isEmpty()) EmptyCard("Nothing observed yet", "Enable sensing, then start a scan from Home.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(nodes, key = { it.id }) { NodeCard(it, onMakeMine) } } } }
@Composable private fun NodeCard(node: TechnologyNode, onMakeMine: (TechnologyNode) -> Unit) = Card(shape = RoundedCornerShape(18.dp)) { ListItem(overlineContent = { Text(if (node.ownership == OwnershipState.MINE) "AUTHORISED RESOURCE" else "PASSIVE OBSERVATION") }, headlineContent = { Text(node.friendlyName, maxLines = 1, overflow = TextOverflow.Ellipsis) }, supportingContent = { Text("Seen ${node.observations}×  •  Signal ${node.rssi ?: "unknown"}") }, trailingContent = { if (node.ownership != OwnershipState.MINE) TextButton({ onMakeMine(node) }) { Text("This is mine") } }) }

@Composable private fun ResourcesScreen(resources: List<TechnologyNode>, demoEnabled: Boolean, toggleDemo: () -> Unit, add: (String, TechnologyCategory, Set<String>) -> Unit) { var showAdd by remember { mutableStateOf(false) }; Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) { Spacer(Modifier.height(12.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("My Technology", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Only equipment you own or can use.", color = MaterialTheme.colorScheme.onSurfaceVariant) }; FilledTonalButton({ showAdd = true }) { Text("Add hardware") } }; Card(Modifier.padding(vertical = 12.dp), shape = RoundedCornerShape(16.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Demo mode", fontWeight = FontWeight.SemiBold); Text("Sample resources for testing the compiler", style = MaterialTheme.typography.labelSmall) }; Switch(demoEnabled, { toggleDemo() }) } }; if (resources.isEmpty()) EmptyCard("Your Resource Pool is empty", "Add equipment manually, or mark a nearby device as yours.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(resources, key = { it.id }) { r -> Card(shape = RoundedCornerShape(18.dp)) { ListItem(overlineContent = { Text(r.category.name) }, headlineContent = { Text(r.friendlyName) }, supportingContent = { Text(r.capabilities.joinToString().ifBlank { "Capabilities not profiled" }) }) } } } }; if (showAdd) AddHardwareDialog({ showAdd = false }) { n,c,p -> add(n,c,p); showAdd = false } }
@Composable private fun AddHardwareDialog(dismiss: () -> Unit, add: (String, TechnologyCategory, Set<String>) -> Unit) { var name by remember { mutableStateOf("") }; var capabilities by remember { mutableStateOf("") }; var category by remember { mutableStateOf(TechnologyCategory.COMPUTER) }; AlertDialog(onDismissRequest = dismiss, title = { Text("Add hardware") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("This adds an authorised item to your local Resource Pool.", style = MaterialTheme.typography.bodySmall); OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(TechnologyCategory.COMPUTER, TechnologyCategory.PHONE, TechnologyCategory.ROUTER).forEach { type -> FilterChip(category == type, { category = type }, label = { Text(type.name.lowercase()) }) } }; OutlinedTextField(capabilities, { capabilities = it }, label = { Text("Capabilities, comma separated") }) } }, confirmButton = { Button(enabled = name.isNotBlank(), onClick = { add(name.trim(), category, capabilities.split(',').map(String::trim).filter(String::isNotEmpty).toSet()) }) { Text("Add") } }, dismissButton = { TextButton(dismiss) { Text("Cancel") } }) }

@Composable private fun BuildScreen(goal: String, updateGoal: (String) -> Unit, plan: ProjectPlan?, compile: () -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text("Build something", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Scrying considers only authorised resources. It never buys, connects to, or deploys anything automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedTextField(goal, updateGoal, label = { Text("What would you like to build?") }, supportingText = { Text("Try “Build a local AI server” or “Create a home camera system”.") }, modifier = Modifier.fillMaxWidth(), minLines = 2); Button(compile, modifier = Modifier.fillMaxWidth()) { Text("Create local plan") }; plan?.let { BuildPlanCard(it) } ?: EmptyCard("No plan yet", "Describe a goal and Scrying will match your available equipment.") } }
@Composable private fun BuildPlanCard(plan: ProjectPlan) = Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Proposed plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(plan.explanation); Text("Assignments", fontWeight = FontWeight.Bold); plan.assignments.forEach { Text("• $it") }; if (plan.gaps.isNotEmpty()) { Text("Still needed", fontWeight = FontWeight.Bold); plan.gaps.forEach { Text("• $it") } } } }
private fun demoResources() = listOf(TechnologyNode(friendlyName = "DEMO Workstation", category = TechnologyCategory.COMPUTER, ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE, capabilities = setOf("Linux", "16 GB RAM", "Ethernet")), TechnologyNode(friendlyName = "DEMO Old Android", category = TechnologyCategory.PHONE, ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE, capabilities = setOf("Wi-Fi", "Camera", "BLE")), TechnologyNode(friendlyName = "DEMO Router", category = TechnologyCategory.ROUTER, ownership = OwnershipState.MINE, availability = AvailabilityState.AVAILABLE_RESOURCE, capabilities = setOf("Wi-Fi", "Ethernet")))
