// app/src/main/java/com/MaFiSoft/BuyPal/ui/theme/GruenTheme.kt
// Stand: 2025-07-09_12:16:00, Codezeilen: ~70 (Neues Theme für Grün)

package com.MaFiSoft.BuyPal.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val GruenHellColorScheme = lightColorScheme(
    primary = GruenHellPrimary,
    onPrimary = GruenHellOnPrimary,
    primaryContainer = GruenHellPrimaryContainer,
    onPrimaryContainer = GruenHellOnPrimaryContainer,
    secondary = GruenHellSecondary,
    onSecondary = GruenHellOnSecondary,
    secondaryContainer = GruenHellSecondaryContainer,
    onSecondaryContainer = GruenHellOnSecondaryContainer,
    tertiary = GruenHellTertiary,
    onTertiary = GruenHellOnTertiary,
    tertiaryContainer = GruenHellTertiaryContainer,
    onTertiaryContainer = GruenHellOnTertiaryContainer,
    error = GruenHellError,
    onError = GruenHellOnError,
    errorContainer = GruenHellErrorContainer,
    onErrorContainer = GruenHellOnErrorContainer,
    background = GruenHellBackground,
    onBackground = GruenHellOnBackground,
    surface = GruenHellSurface,
    onSurface = GruenHellOnSurface,
    surfaceVariant = GruenHellSurfaceVariant,
    onSurfaceVariant = GruenHellOnSurfaceVariant,
    outline = GruenHellOutline,
    inverseOnSurface = GruenHellInverseOnSurface,
    inverseSurface = GruenHellInverseSurface,
    inversePrimary = GruenHellInversePrimary,
    surfaceTint = GruenHellSurfaceTint,
    outlineVariant = GruenHellOutlineVariant,
    scrim = GruenHellScrim
)

private val GruenDunkelColorScheme = darkColorScheme(
    primary = GruenDunkelPrimary,
    onPrimary = GruenDunkelOnPrimary,
    primaryContainer = GruenDunkelPrimaryContainer,
    onPrimaryContainer = GruenDunkelOnPrimaryContainer,
    secondary = GruenDunkelSecondary,
    onSecondary = GruenDunkelOnSecondary,
    secondaryContainer = GruenDunkelSecondaryContainer,
    onSecondaryContainer = GruenDunkelOnSecondaryContainer,
    tertiary = GruenDunkelTertiary,
    onTertiary = GruenDunkelOnTertiary,
    tertiaryContainer = GruenDunkelTertiaryContainer,
    onTertiaryContainer = GruenDunkelOnTertiaryContainer,
    error = GruenDunkelError,
    onError = GruenDunkelOnError,
    errorContainer = GruenDunkelErrorContainer,
    onErrorContainer = GruenDunkelOnErrorContainer,
    background = GruenDunkelBackground,
    onBackground = GruenDunkelOnBackground,
    surface = GruenDunkelSurface,
    onSurface = GruenDunkelOnSurface,
    surfaceVariant = GruenDunkelSurfaceVariant,
    onSurfaceVariant = GruenDunkelOnSurfaceVariant,
    outline = GruenDunkelOutline,
    inverseOnSurface = GruenDunkelInverseOnSurface,
    inverseSurface = GruenDunkelInverseSurface,
    inversePrimary = GruenDunkelInversePrimary,
    surfaceTint = GruenDunkelSurfaceTint,
    outlineVariant = GruenDunkelOutlineVariant,
    scrim = GruenDunkelScrim
)

@Composable
fun GruenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> GruenDunkelColorScheme
        else -> GruenHellColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
