@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package uk.co.scrying.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import uk.co.scrying.ScryingViewModel
import uk.co.scrying.UpdateStatus
import uk.co.scrying.ui.atlas.AtlasScreen
import uk.co.scrying.ui.circle.CircleScreen
import uk.co.scrying.ui.forge.ForgeScreen
import uk.co.scrying.ui.onboarding.FirstRun
import uk.co.scrying.ui.pulse.PulseScreen
import uk.co.scrying.ui.theme.ScryingTheme

enum class Destination(val label: String) { Pulse("Pulse"), Atlas("Atlas"), Circle("Circle"), Forge("Forge") }

@Composable fun ScryingApp(vm: ScryingViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val update by vm.updateStatus.collectAsStateWithLifecycle()
    if (state.firstRun) { ScryingTheme { FirstRun(state.profile, vm::completeOnboarding) }; return }
    var destination by remember { mutableStateOf(Destination.Pulse) }
    var menuOpen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    ScryingTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text(if (destination == Destination.Pulse) "" else destination.label) }, actions = { Box { IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More options") }; DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) { DropdownMenuItem(text = { Text(if (update is UpdateStatus.Checking) "Checking for updates…" else "Check for updates") }, onClick = { menuOpen = false; vm.checkForUpdates() }) } } }) },
            bottomBar = { NavigationBar { Destination.entries.forEach { item -> val icon = when (item) { Destination.Pulse -> Icons.Filled.Search; Destination.Atlas -> Icons.Filled.Map; Destination.Circle -> Icons.Filled.Groups; Destination.Forge -> Icons.Filled.Build }; NavigationBarItem(selected = destination == item, onClick = { destination = item }, icon = { Icon(icon, contentDescription = item.label) }, label = { Text(item.label) }) } } }
        ) { padding -> when (destination) {
            Destination.Pulse -> PulseScreen(Modifier, padding, state, vm)
            Destination.Atlas -> AtlasScreen(Modifier, padding, state, vm)
            Destination.Circle -> CircleScreen(Modifier, padding, state, vm)
            Destination.Forge -> ForgeScreen(Modifier, padding, state, vm, onGoCircle = { destination = Destination.Circle }, onContribute = { destination = Destination.Pulse })
        } }
        if (update is UpdateStatus.Available) {
            val available = update as UpdateStatus.Available
            AlertDialog(onDismissRequest = vm::clearUpdateStatus, title = { Text("Scrying ${available.version} is ready") }, text = { Text("GitHub has a newer release. Android will download and ask you to confirm installation; Scrying cannot install it silently.") }, confirmButton = { Button(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(available.downloadUrl))); vm.clearUpdateStatus() }) { Text("Download update") } }, dismissButton = { TextButton(onClick = vm::clearUpdateStatus) { Text("Later") } })
        }
    }
}
