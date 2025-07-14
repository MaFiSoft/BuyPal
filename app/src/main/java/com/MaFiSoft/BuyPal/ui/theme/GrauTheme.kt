// app/src/main/java/com/MaFiSoft/BuyPal/ui/theme/GrauTheme.kt
// Stand: 2025-07-09_12:16:00, Codezeilen: ~70 (Neues Theme für Grau)

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

private val GrauHellColorScheme = lightColorScheme(
    primary = GrauHellPrimary,
    onPrimary = GrauHellOnPrimary,
    primaryContainer = GrauHellPrimaryContainer,
    onPrimaryContainer = GrauHellOnPrimaryContainer,
    secondary = GrauHellSecondary,
    onSecondary = GrauHellOnSecondary,
    secondaryContainer = GrauHellSecondaryContainer,
    onSecondaryContainer = GrauHellOnSecondaryContainer,
    tertiary = GrauHellTertiary,
    onTertiary = GrauHellOnTertiary,
    tertiaryContainer = GrauHellTertiaryContainer,
    onTertiaryContainer = GrauHellOnTertiaryContainer,
    error = GrauHellError,
    onError = GrauHellOnError,
    errorContainer = GrauHellErrorContainer,
    onErrorContainer = GrauHellOnErrorContainer,
    background = GrauHellBackground,
    onBackground = GrauHellOnBackground,
    surface = GrauHellSurface,
    onSurface = GrauHellOnSurface,
    surfaceVariant = GrauHellSurfaceVariant,
    onSurfaceVariant = GrauHellOnSurfaceVariant,
    outline = GrauHellOutline,
    inverseOnSurface = GrauHellInverseOnSurface,
    inverseSurface = GrauHellInverseSurface,
    inversePrimary = GrauHellInversePrimary,
    surfaceTint = GrauHellSurfaceTint,
    outlineVariant = GrauHellOutlineVariant,
    scrim = GrauHellScrim
)

private val GrauDunkelColorScheme = darkColorScheme(
    primary = GrauDunkelPrimary,
    onPrimary = GrauDunkelOnPrimary,
    primaryContainer = GrauDunkelPrimaryContainer,
    onPrimaryContainer = GrauDunkelOnPrimaryContainer,
    secondary = GrauDunkelSecondary,
    onSecondary = GrauDunkelOnSecondary,
    secondaryContainer = GrauDunkelSecondaryContainer,
    onSecondaryContainer = GrauDunkelOnSecondaryContainer,
    tertiary = GrauDunkelTertiary,
    onTertiary = GrauDunkelOnTertiary,
    tertiaryContainer = GrauDunkelTertiaryContainer,
    onTertiaryContainer = GrauDunkelOnTertiaryContainer,
    error = GrauDunkelError,
    onError = GrauDunkelOnError,
    errorContainer = GrauDunkelErrorContainer,
    onErrorContainer = GrauDunkelOnErrorContainer,
    background = GrauDunkelBackground,
    onBackground = GrauDunkelOnBackground,
    surface = GrauDunkelSurface,
    onSurface = GrauDunkelOnSurface,
    surfaceVariant = GrauDunkelSurfaceVariant,
    onSurfaceVariant = GrauDunkelOnSurfaceVariant,
    outline = GrauDunkelOutline,
    inverseOnSurface = GrauDunkelInverseOnSurface,
    inverseSurface = GrauDunkelInverseSurface,
    inversePrimary = GrauDunkelInversePrimary,
    surfaceTint = GrauDunkelSurfaceTint,
    outlineVariant = GrauDunkelOutlineVariant,
    scrim = GrauDunkelScrim
)

@Composable
fun GrauTheme(
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

        darkTheme -> GrauDunkelColorScheme
        else -> GrauHellColorScheme
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
