package uk.co.scrying.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.co.scrying.ScryingViewModel
import uk.co.scrying.ui.atlas.AtlasScreen
import uk.co.scrying.ui.circle.CircleScreen
import uk.co.scrying.ui.forge.ForgeScreen
import uk.co.scrying.ui.onboarding.FirstRun
import uk.co.scrying.ui.pulse.PulseScreen
import uk.co.scrying.ui.theme.ScryingTheme

enum class Destination(val label: String) { Pulse("Pulse"), Atlas("Atlas"), Circle("Circle"), Forge("Forge") }

@Composable fun ScryingApp(vm: ScryingViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (state.firstRun) { ScryingTheme { FirstRun(state.profile, vm::completeOnboarding) }; return }
    var destination by remember { mutableStateOf(Destination.Pulse) }
    ScryingTheme {
        Scaffold(
            bottomBar = { NavigationBar { Destination.entries.forEach { item -> val icon = when (item) { Destination.Pulse -> Icons.Filled.Search; Destination.Atlas -> Icons.Filled.Map; Destination.Circle -> Icons.Filled.Groups; Destination.Forge -> Icons.Filled.Build }; NavigationBarItem(selected = destination == item, onClick = { destination = item }, icon = { Icon(icon, contentDescription = item.label) }, label = { Text(item.label) }) } } }
        ) { padding -> when (destination) {
            Destination.Pulse -> PulseScreen(Modifier, padding, state, vm)
            Destination.Atlas -> AtlasScreen(Modifier, padding, state, vm)
            Destination.Circle -> CircleScreen(Modifier, padding, state, vm)
            Destination.Forge -> ForgeScreen(Modifier, padding, state, vm, onGoCircle = { destination = Destination.Circle }, onContribute = { destination = Destination.Pulse })
        } }
    }
}
