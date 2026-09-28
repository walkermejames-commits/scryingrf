package uk.co.scrying.ui.forge

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.co.scrying.*
import uk.co.scrying.ui.components.EmptyState
import uk.co.scrying.ui.components.PrivacyNote
import uk.co.scrying.ui.components.ScryingCard

@Composable fun ForgeScreen(modifier: Modifier, padding: PaddingValues, state: ScryingUiState, vm: ScryingViewModel, onGoCircle: () -> Unit, onContribute: () -> Unit) {
    var goal by remember { mutableStateOf("Build a connected workshop monitor") }; var plan by remember { mutableStateOf<ProjectPlan?>(null) }
    val resources = state.nodes.filter { it.ownership in setOf(OwnershipState.MINE, OwnershipState.PERMISSION_GRANTED) }
    Column(modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Forge", style = MaterialTheme.typography.titleLarge); Text("Turn your authorised kit into a practical local setup.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        ScryingCard { Text("Available to plan", style = MaterialTheme.typography.titleMedium); Text("${resources.size} authorised equipment item${if (resources.size == 1) "" else "s"}. Observed radios are intentionally excluded."); if (resources.isEmpty()) TextButton(onClick = onGoCircle) { Text("Add authorised equipment") } }
        OutlinedTextField(goal, { goal = it }, label = { Text("What should this system do?") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        Button(onClick = { plan = vm.createPlan(goal, resources) }, modifier = Modifier.fillMaxWidth()) { Text("Create local plan") }
        plan?.let { result -> PlanResult(result, onGoCircle, onContribute) } ?: EmptyState("Start with a specific outcome", "Try: “Record workshop environmental readings locally” or “Build a camera and storage setup”.")
        PrivacyNote("Plans are suggestions. Scrying never connects to or controls equipment automatically.")
    }
}
@Composable private fun PlanResult(plan: ProjectPlan, onGoCircle: () -> Unit, onContribute: () -> Unit) = Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(plan.explanation, style = MaterialTheme.typography.bodySmall)
    plan.assignments.forEach { assignment -> ScryingCard { Text("Assignment", style = MaterialTheme.typography.labelSmall); Text(assignment, fontWeight = FontWeight.SemiBold) } }
    plan.gaps.forEach { gap -> Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(gap, Modifier.weight(1f)); TextButton(onClick = if (gap.contains("observer", true)) onContribute else onGoCircle) { Text(if (gap.contains("observer", true)) "Open sensing" else "Add kit") } } } }
}
