package uk.co.scrying.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Light = lightColorScheme(primary = ScryingBlue, onPrimary = androidx.compose.ui.graphics.Color.White, secondary = ScryingTeal, tertiary = ScryingTeal, error = androidx.compose.ui.graphics.Color(0xFFB3261E), background = androidx.compose.ui.graphics.Color(0xFFF8F9FC), surface = androidx.compose.ui.graphics.Color.White, outline = Observed)
private val Dark = darkColorScheme(primary = SignalMint, onPrimary = Night, secondary = SignalMint, tertiary = Color(0xFF91D7FF), error = Color(0xFFFFB4AB), background = Night, surface = NightSurface, surfaceVariant = NightSurfaceHigh, outline = Observed)

@Composable fun ScryingTheme(dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) } else if (dark) Dark else Light
    MaterialTheme(colorScheme = scheme, typography = ScryingTypography, shapes = ScryingShapes, content = content)
}
