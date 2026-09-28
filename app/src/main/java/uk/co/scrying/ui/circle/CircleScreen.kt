@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package uk.co.scrying.ui.circle

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.scrying.*
import uk.co.scrying.ui.components.*

@Composable fun CircleScreen(modifier: Modifier, padding: PaddingValues, state: ScryingUiState, vm: ScryingViewModel) {
    var section by remember { mutableStateOf("People") }; var addPerson by remember { mutableStateOf(false) }; var addDevice by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Text("Circle", style = MaterialTheme.typography.titleLarge); Text("People, this phone, and equipment you are allowed to use.", style = MaterialTheme.typography.bodySmall)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(vertical = 12.dp)) { listOf("People", "My kit").forEachIndexed { index, item -> SegmentedButton(section == item, { section = item }, SegmentedButtonDefaults.itemShape(index, 2)) { Text(item) } } }
        if (section == "People") PeopleSection(state, vm, { addPerson = true }) else KitSection(state, vm, { addDevice = true })
    }
    if (addPerson) AddPerson { person -> vm.savePerson(person); addPerson = false }
    if (addDevice) AddDevice { name, category -> vm.addAuthorisedDevice(name, category); addDevice = false }
}

@Composable private fun PeopleSection(state: ScryingUiState, vm: ScryingViewModel, add: () -> Unit) = LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item { ScryingCard { Text("People are local labels", style = MaterialTheme.typography.titleMedium); Text("Adding someone does not contact, track, or share with them. Sharing settings only prepare a future, explicitly approved pairing.", style = MaterialTheme.typography.bodySmall); Button(onClick = add, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) { Text("Add local person") } } }
    if (state.people.isEmpty()) item { EmptyState("No local people", "Add yourself, household members, or collaborators so future permissions have a clear human owner.") }
    items(state.people, key = { it.id }) { person -> PersonCard(person, state.agreements.filter { it.personId == person.id }, vm) }
}
@Composable private fun PersonCard(person: Person, agreements: List<SharingAgreement>, vm: ScryingViewModel) { var open by remember { mutableStateOf(false) }; ScryingCard { Row(verticalAlignment = Alignment.CenterVertically) { Surface(Modifier.size(44.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.secondaryContainer) { Box(contentAlignment = Alignment.Center) { Text(person.displayName.take(2).uppercase()) } }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(person.displayName, fontWeight = FontWeight.SemiBold); Text(person.note.ifBlank { "No note" }, style = MaterialTheme.typography.labelSmall); Text("${agreements.count { it.enabled }} future share permissions enabled", style = MaterialTheme.typography.labelSmall) }; TextButton({ open = true }) { Text("Manage") } } }; if (open) ModalBottomSheet(onDismissRequest = { open = false }) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("${person.displayName}'s future permissions", style = MaterialTheme.typography.titleLarge); Text("No data moves between people or phones in this build. These are clear, revocable local permissions for a future pairing flow.", style = MaterialTheme.typography.bodySmall); listOf(ContributionType.DEVICE_HEALTH to "Device health", ContributionType.BLE_OBSERVER to "Signal observations", ContributionType.WIFI_OBSERVER to "Wi-Fi observations").forEach { (type, label) -> val enabled = agreements.firstOrNull { it.contribution == type }?.enabled ?: false; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Switch(enabled, { vm.saveAgreement(SharingAgreement(person.id, type, it)) }) } }; TextButton(onClick = { vm.deletePerson(person); open = false }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete local person") } } } }
@Composable private fun KitSection(state: ScryingUiState, vm: ScryingViewModel, add: () -> Unit) = LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    item { ScryingCard { Text("This phone", style = MaterialTheme.typography.titleLarge); Text(state.profile.summary); CapabilityGrid(state.profile.capabilities); TextButton(vm::refreshProfile) { Text("Refresh hardware profile") } } }
    item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("Authorised equipment", style = MaterialTheme.typography.titleMedium); FilledTonalButton(add) { Text("Add equipment") } } }
    val equipment = state.nodes.filter { it.ownership in setOf(OwnershipState.MINE, OwnershipState.PERMISSION_GRANTED) }
    if (equipment.isEmpty()) item { EmptyState("No authorised equipment", "Add a desktop, router, camera, or sensor you own or are allowed to use. Forge will use only these items.") }
    items(equipment, key = { it.id }) { node -> ScryingCard { Text(node.friendlyName, fontWeight = FontWeight.SemiBold); OwnershipBadge(node.ownership); Text(node.category.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall) } }
}
@Composable private fun CapabilityGrid(capabilities: List<DeviceCapability>) = Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { capabilities.take(8).forEach { capability -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(capability.title, style = MaterialTheme.typography.bodySmall); Text(capability.state.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, color = when (capability.state) { CapabilityState.AVAILABLE -> MaterialTheme.colorScheme.secondary; CapabilityState.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.primary; CapabilityState.UNSUPPORTED -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurfaceVariant }, style = MaterialTheme.typography.labelSmall) } } }
@Composable private fun AddPerson(save: (Person) -> Unit) { var name by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = {}, title = { Text("Add local person") }, text = { Column { OutlinedTextField(name, { name = it }, label = { Text("Name") }); OutlinedTextField(note, { note = it }, label = { Text("Context or permission note") }) } }, confirmButton = { Button(enabled = name.isNotBlank(), onClick = { save(Person(displayName = name.trim(), note = note.trim())) }) { Text("Save") } }) }
@Composable private fun AddDevice(save: (String, TechnologyCategory) -> Unit) { var name by remember { mutableStateOf("") }; var category by remember { mutableStateOf(TechnologyCategory.COMPUTER) }; AlertDialog(onDismissRequest = {}, title = { Text("Add authorised equipment") }, text = { Column { Text("Only add equipment you own or are allowed to use.", style = MaterialTheme.typography.bodySmall); OutlinedTextField(name, { name = it }, label = { Text("Name") }); TechnologyCategory.entries.take(6).forEach { item -> FilterChip(category == item, { category = item }, label = { Text(item.name.lowercase().replaceFirstChar { it.uppercase() }) }) } } }, confirmButton = { Button(enabled = name.isNotBlank(), onClick = { save(name.trim(), category) }) { Text("Add authorised equipment") } }) }
