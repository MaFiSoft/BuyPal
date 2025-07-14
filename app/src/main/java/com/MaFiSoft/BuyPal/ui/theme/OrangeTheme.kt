// app/src/main/java/com/MaFiSoft/BuyPal/ui/theme/OrangeTheme.kt
// Stand: 2025-07-09_12:16:00, Codezeilen: ~70 (Neues Theme für Orange)

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

private val OrangeHellColorScheme = lightColorScheme(
    primary = OrangeHellPrimary,
    onPrimary = OrangeHellOnPrimary,
    primaryContainer = OrangeHellPrimaryContainer,
    onPrimaryContainer = OrangeHellOnPrimaryContainer,
    secondary = OrangeHellSecondary,
    onSecondary = OrangeHellOnSecondary,
    secondaryContainer = OrangeHellSecondaryContainer,
    onSecondaryContainer = OrangeHellOnSecondaryContainer,
    tertiary = OrangeHellTertiary,
    onTertiary = OrangeHellOnTertiary,
    tertiaryContainer = OrangeHellTertiaryContainer,
    onTertiaryContainer = OrangeHellOnTertiaryContainer,
    error = OrangeHellError,
    onError = OrangeHellOnError,
    errorContainer = OrangeHellErrorContainer,
    onErrorContainer = OrangeHellOnErrorContainer,
    background = OrangeHellBackground,
    onBackground = OrangeHellOnBackground,
    surface = OrangeHellSurface,
    onSurface = OrangeHellOnSurface,
    surfaceVariant = OrangeHellSurfaceVariant,
    onSurfaceVariant = OrangeHellOnSurfaceVariant,
    outline = OrangeHellOutline,
    inverseOnSurface = OrangeHellInverseOnSurface,
    inverseSurface = OrangeHellInverseSurface,
    inversePrimary = OrangeHellInversePrimary,
    surfaceTint = OrangeHellSurfaceTint,
    outlineVariant = OrangeHellOutlineVariant,
    scrim = OrangeHellScrim
)

private val OrangeDunkelColorScheme = darkColorScheme(
    primary = OrangeDunkelPrimary,
    onPrimary = OrangeDunkelOnPrimary,
    primaryContainer = OrangeDunkelPrimaryContainer,
    onPrimaryContainer = OrangeDunkelOnPrimaryContainer,
    secondary = OrangeDunkelSecondary,
    onSecondary = OrangeDunkelOnSecondary,
    secondaryContainer = OrangeDunkelSecondaryContainer,
    onSecondaryContainer = OrangeDunkelOnSecondaryContainer,
    tertiary = OrangeDunkelTertiary,
    onTertiary = OrangeDunkelOnTertiary,
    tertiaryContainer = OrangeDunkelTertiaryContainer,
    onTertiaryContainer = OrangeDunkelOnTertiaryContainer,
    error = OrangeDunkelError,
    onError = OrangeDunkelOnError,
    errorContainer = OrangeDunkelErrorContainer,
    onErrorContainer = OrangeDunkelOnErrorContainer,
    background = OrangeDunkelBackground,
    onBackground = OrangeDunkelOnBackground,
    surface = OrangeDunkelSurface,
    onSurface = OrangeDunkelOnSurface,
    surfaceVariant = OrangeDunkelSurfaceVariant,
    onSurfaceVariant = OrangeDunkelOnSurfaceVariant,
    outline = OrangeDunkelOutline,
    inverseOnSurface = OrangeDunkelInverseOnSurface,
    inverseSurface = OrangeDunkelInverseSurface,
    inversePrimary = OrangeDunkelInversePrimary,
    surfaceTint = OrangeDunkelSurfaceTint,
    outlineVariant = OrangeDunkelOutlineVariant,
    scrim = OrangeDunkelScrim
)

@Composable
fun OrangeTheme(
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

        darkTheme -> OrangeDunkelColorScheme
        else -> OrangeHellColorScheme
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
