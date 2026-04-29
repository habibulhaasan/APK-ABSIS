
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
// ui/theme/Theme.kt
// ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
package com.absis.capitalsync.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary          = PrimaryBlue,
    onPrimary        = Color.White,
    primaryContainer = PrimaryLight,
    secondary        = PrimaryDark,
    onSecondary      = Color.White,
    background       = AppBackground,
    surface          = Color.White,
    onBackground     = PrimaryDark,
    onSurface        = PrimaryDark,
    outline          = BorderGray,
    error            = DangerRed,
)

@Composable
fun CapitalSyncTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography  = Typography(),   // default M3 typography
        content     = content
    )
}