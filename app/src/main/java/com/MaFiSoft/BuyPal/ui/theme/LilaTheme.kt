// app/src/main/java/com/MaFiSoft/BuyPal/ui/theme/LilaTheme.kt
// Stand: 2025-07-09_12:16:00, Codezeilen: ~70 (Neues Theme für Lila)

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

private val LilaHellColorScheme = lightColorScheme(
    primary = LilaHellPrimary,
    onPrimary = LilaHellOnPrimary,
    primaryContainer = LilaHellPrimaryContainer,
    onPrimaryContainer = LilaHellOnPrimaryContainer,
    secondary = LilaHellSecondary,
    onSecondary = LilaHellOnSecondary,
    secondaryContainer = LilaHellSecondaryContainer,
    onSecondaryContainer = LilaHellOnSecondaryContainer,
    tertiary = LilaHellTertiary,
    onTertiary = LilaHellOnTertiary,
    tertiaryContainer = LilaHellTertiaryContainer,
    onTertiaryContainer = LilaHellOnTertiaryContainer,
    error = LilaHellError,
    onError = LilaHellOnError,
    errorContainer = LilaHellErrorContainer,
    onErrorContainer = LilaHellOnErrorContainer,
    background = LilaHellBackground,
    onBackground = LilaHellOnBackground,
    surface = LilaHellSurface,
    onSurface = LilaHellOnSurface,
    surfaceVariant = LilaHellSurfaceVariant,
    onSurfaceVariant = LilaHellOnSurfaceVariant,
    outline = LilaHellOutline,
    inverseOnSurface = LilaHellInverseOnSurface,
    inverseSurface = LilaHellInverseSurface,
    inversePrimary = LilaHellInversePrimary,
    surfaceTint = LilaHellSurfaceTint,
    outlineVariant = LilaHellOutlineVariant,
    scrim = LilaHellScrim
)

private val LilaDunkelColorScheme = darkColorScheme(
    primary = LilaDunkelPrimary,
    onPrimary = LilaDunkelOnPrimary,
    primaryContainer = LilaDunkelPrimaryContainer,
    onPrimaryContainer = LilaDunkelOnPrimaryContainer,
    secondary = LilaDunkelSecondary,
    onSecondary = LilaDunkelOnSecondary,
    secondaryContainer = LilaDunkelSecondaryContainer,
    onSecondaryContainer = LilaDunkelOnSecondaryContainer,
    tertiary = LilaDunkelTertiary,
    onTertiary = LilaDunkelOnTertiary,
    tertiaryContainer = LilaDunkelTertiaryContainer,
    onTertiaryContainer = LilaDunkelOnTertiaryContainer,
    error = LilaDunkelError,
    onError = LilaDunkelOnError,
    errorContainer = LilaDunkelErrorContainer,
    onErrorContainer = LilaDunkelOnErrorContainer,
    background = LilaDunkelBackground,
    onBackground = LilaDunkelOnBackground,
    surface = LilaDunkelSurface,
    onSurface = LilaDunkelOnSurface,
    surfaceVariant = LilaDunkelSurfaceVariant,
    onSurfaceVariant = LilaDunkelOnSurfaceVariant,
    outline = LilaDunkelOutline,
    inverseOnSurface = LilaDunkelInverseOnSurface,
    inverseSurface = LilaDunkelInverseSurface,
    inversePrimary = LilaDunkelInversePrimary,
    surfaceTint = LilaDunkelSurfaceTint,
    outlineVariant = LilaDunkelOutlineVariant,
    scrim = LilaDunkelScrim
)

@Composable
fun LilaTheme(
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

        darkTheme -> LilaDunkelColorScheme
        else -> LilaHellColorScheme
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
