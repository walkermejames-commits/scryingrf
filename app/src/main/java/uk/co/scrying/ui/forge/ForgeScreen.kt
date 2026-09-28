package uk.co.scrying.ui.forge

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.scrying.*
import uk.co.scrying.ui.components.PrivacyNote
import uk.co.scrying.ui.components.ScryingCard

@Composable fun ForgeScreen(modifier: Modifier, padding: PaddingValues, state: ScryingUiState, vm: ScryingViewModel, onGoCircle: () -> Unit, onContribute: () -> Unit) { var goal by remember { mutableStateOf("Build a connected workshop monitor") }; var plan by remember { mutableStateOf<ProjectPlan?>(null) }; Column(modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Forge", style = MaterialTheme.typography.titleLarge); Text("Turn authorised kit into a local plan.", color = MaterialTheme.colorScheme.onSurfaceVariant); OutlinedTextField(goal, { goal = it }, label = { Text("What should this system do?") }, modifier = Modifier.fillMaxWidth(), minLines = 2); Button(onClick = { plan = vm.createPlan(goal, state.nodes.filter { it.ownership in setOf(OwnershipState.MINE, OwnershipState.PERMISSION_GRANTED) }) }, modifier = Modifier.fillMaxWidth()) { Text("Create plan") }; plan?.let { result -> Text(result.explanation); result.assignments.forEach { assignment -> ScryingCard { Text(assignment, fontWeight = FontWeight.SemiBold) } }; result.gaps.forEach { gap -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(gap, Modifier.weight(1f)); TextButton(onClick = if (gap.contains("observer", true)) onContribute else onGoCircle) { Text(if (gap.contains("observer", true)) "Open sensing" else "Add device") } } } } }; PrivacyNote("Plans are suggestions. Only authorised resources are included.") } }
