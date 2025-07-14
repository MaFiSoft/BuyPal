// app/src/main/java/com/MaFiSoft/BuyPal/ui/theme/BlauTheme.kt
// Stand: 2025-07-09_12:16:00, Codezeilen: ~70 (Umbenannt und Farbbezug angepasst)

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

private val BlauHellColorScheme = lightColorScheme(
    primary = BlauHellPrimary,
    onPrimary = BlauHellOnPrimary,
    primaryContainer = BlauHellPrimaryContainer,
    onPrimaryContainer = BlauHellOnPrimaryContainer,
    secondary = BlauHellSecondary,
    onSecondary = BlauHellOnSecondary,
    secondaryContainer = BlauHellSecondaryContainer,
    onSecondaryContainer = BlauHellOnSecondaryContainer,
    tertiary = BlauHellTertiary,
    onTertiary = BlauHellOnTertiary,
    tertiaryContainer = BlauHellTertiaryContainer,
    onTertiaryContainer = BlauHellOnTertiaryContainer,
    error = BlauHellError,
    onError = BlauHellOnError,
    errorContainer = BlauHellErrorContainer,
    onErrorContainer = BlauHellOnErrorContainer,
    background = BlauHellBackground,
    onBackground = BlauHellOnBackground,
    surface = BlauHellSurface,
    onSurface = BlauHellOnSurface,
    surfaceVariant = BlauHellSurfaceVariant,
    onSurfaceVariant = BlauHellOnSurfaceVariant,
    outline = BlauHellOutline,
    inverseOnSurface = BlauHellInverseOnSurface,
    inverseSurface = BlauHellInverseSurface,
    inversePrimary = BlauHellInversePrimary,
    surfaceTint = BlauHellSurfaceTint,
    outlineVariant = BlauHellOutlineVariant,
    scrim = BlauHellScrim
)

private val BlauDunkelColorScheme = darkColorScheme(
    primary = BlauDunkelPrimary,
    onPrimary = BlauDunkelOnPrimary,
    primaryContainer = BlauDunkelPrimaryContainer,
    onPrimaryContainer = BlauDunkelOnPrimaryContainer,
    secondary = BlauDunkelSecondary,
    onSecondary = BlauDunkelOnSecondary,
    secondaryContainer = BlauDunkelSecondaryContainer,
    onSecondaryContainer = BlauDunkelOnSecondaryContainer,
    tertiary = BlauDunkelTertiary,
    onTertiary = BlauDunkelOnTertiary,
    tertiaryContainer = BlauDunkelTertiaryContainer,
    onTertiaryContainer = BlauDunkelOnTertiaryContainer,
    error = BlauDunkelError,
    onError = BlauDunkelOnError,
    errorContainer = BlauDunkelErrorContainer,
    onErrorContainer = BlauDunkelOnErrorContainer,
    background = BlauDunkelBackground,
    onBackground = BlauDunkelOnBackground,
    surface = BlauDunkelSurface,
    onSurface = BlauDunkelOnSurface,
    surfaceVariant = BlauDunkelSurfaceVariant,
    onSurfaceVariant = BlauDunkelOnSurfaceVariant,
    outline = BlauDunkelOutline,
    inverseOnSurface = BlauDunkelInverseOnSurface,
    inverseSurface = BlauDunkelInverseSurface,
    inversePrimary = BlauDunkelInversePrimary,
    surfaceTint = BlauDunkelSurfaceTint,
    outlineVariant = BlauDunkelOutlineVariant,
    scrim = BlauDunkelScrim
)

@Composable
fun BlauTheme(
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

        darkTheme -> BlauDunkelColorScheme
        else -> BlauHellColorScheme
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
