package uk.co.scrying.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import uk.co.scrying.OwnershipState
import uk.co.scrying.ui.theme.Amber
import uk.co.scrying.ui.theme.Observed
import uk.co.scrying.ui.theme.ScryingTeal

@Composable fun ScryingCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Card(modifier, shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .7f)), content = { Column(Modifier.padding(16.dp), content = content) })
@Composable fun StatusChip(label: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit = {}) = AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Surface(Modifier.size(8.dp), shape = MaterialTheme.shapes.extraSmall, color = color) {} }, modifier = modifier)
@Composable fun OwnershipBadge(ownership: OwnershipState) { val (label, color) = when (ownership) { OwnershipState.MINE, OwnershipState.PERMISSION_GRANTED -> "Authorised" to ScryingTeal; OwnershipState.OBSERVED -> "Observed" to Observed; else -> "Unknown" to Amber }; AssistChip(onClick = {}, label = { Text(label) }, colors = AssistChipDefaults.assistChipColors(labelColor = color), modifier = Modifier.semantics { contentDescription = label }) }
@Composable fun SignalBars(rssi: Int?) { val level = when { rssi == null -> 0; rssi >= -55 -> 4; rssi >= -67 -> 3; rssi >= -78 -> 2; else -> 1 }; val description = when (level) { 4 -> "Signal strong"; 3 -> "Signal medium"; 2 -> "Signal low"; 1 -> "Signal weak"; else -> "Signal unknown" }; Row(Modifier.semantics { contentDescription = "$description${rssi?.let { ", $it dBm" } ?: ""}" }, verticalAlignment = Alignment.Bottom) { repeat(4) { index -> Surface(Modifier.width(4.dp).height((7 + index * 4).dp).padding(end = 1.dp), color = if (index < level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant) {} } } }
@Composable fun PrivacyNote(text: String = "Local by default. Observed devices are never made resources without your confirmation.") = Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
@Composable fun EmptyState(title: String, detail: String, action: (@Composable () -> Unit)? = null) = ScryingCard { Text(title, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(4.dp)); Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant); action?.let { Spacer(Modifier.height(12.dp)); it() } }
@Composable fun ScanFab(active: Boolean, onClick: () -> Unit) { val transition = rememberInfiniteTransition(label = "scan"); val scale by transition.animateFloat(1f, if (active) 1.16f else 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "halo"); val halo = MaterialTheme.colorScheme.primary.copy(alpha = .18f); Box(contentAlignment = Alignment.Center) { if (active) Canvas(Modifier.size(70.dp).scale(scale)) { drawCircle(halo, style = Stroke(4.dp.toPx())) }; FloatingActionButton(onClick = onClick, modifier = Modifier.size(56.dp)) { Icon(Icons.Filled.Search, contentDescription = if (active) "Stop scan" else "Start scan") } } }
