// app/src/main/java/com/MaFiSoft/BuyPal/ui/theme/TuerkisTheme.kt
// Stand: 2025-07-10_01:50:00, Codezeilen: ~100 (Türkis-Theme mit RGB 0, 239, 239 als Hauptfarbe und hellerem Container)

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
import androidx.compose.ui.graphics.Color // WICHTIG: Import fuer Color

// Definieren Sie die neuen Türkis-Farben basierend auf RGB(0, 239, 239)
val TuerkisMain = Color(0xFF00EFEF) // RGB (0, 239, 239) - Ihre gewünschte Hauptfarbe
val TuerkisLightContainer = Color(0xFF70EFEF) // RGB (112, 239, 239) - Heller fuer Container, Cards, Pill
val TuerkisDark = Color(0xFF009696) // RGB (0, 150, 150) - Dunkler fuer Dark Mode Primary oder Kontrast
val TuerkisVeryDark = Color(0xFF002020) // RGB (0, 32, 32) - Sehr dunkel fuer Dark Mode Hintergruende

// #################################################################################################
// ######################################### HELL THEME ############################################
// #################################################################################################
private val TuerkisHellColorScheme = lightColorScheme(
    primary = TuerkisMain, // Hauptfarbe fuer Icons, Buttons, Statusbar (0, 239, 239)
    onPrimary = Color(0xFFFFFFFF), // Schwarzer Text auf primärer Farbe => zu WEISS geändert
    primaryContainer = TuerkisLightContainer, // Hellerer Hintergrund fuer Container, Cards, Pill (112, 239, 239)
    onPrimaryContainer = Color(0xFF000000), // Schwarzer Text auf Container-Hintergrund

    secondary = TuerkisMain, // Sekundärfarbe (kann gleich der Primärfarbe sein oder anders)
    onSecondary = Color(0xFF000000), // Schwarzer Text auf sekundärer Farbe
    secondaryContainer = TuerkisLightContainer, // Hellerer Hintergrund fuer sekundäre Container
    onSecondaryContainer = Color(0xFF000000), // Schwarzer Text auf sekundärem Container-Hintergrund

    tertiary = Color(0xFF6200EE), // Beispiel, kann angepasst werden
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBB86FC),
    onTertiaryContainer = Color(0xFF000000),

    error = Color(0xFFB00020),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFCD8DF),
    onErrorContainer = Color(0xFF8C0000),

    background = Color(0xFFFFFFFF), // Reines Weiß als Hintergrund
    onBackground = Color(0xFF000000), // Schwarzer Text auf Hintergrund
    surface = Color(0xFFFFFFFF), // Reines Weiß als Oberflächenfarbe
    onSurface = Color(0xFF000000), // Schwarzer Text auf Oberflächenfarbe
    surfaceVariant = Color(0xFFBDFFF7), // Helles Grau fuer Varianten
    onSurfaceVariant = Color(0xFF424242), // Dunkleres Grau fuer Varianten-Text
    outline = Color(0xFF9E9E9E),
    inverseOnSurface = Color(0xFF212121),
    inverseSurface = Color(0xFFE0E0E0),
    inversePrimary = TuerkisDark, // Inverse Primärfarbe (dunkleres Türkis)
    surfaceTint = TuerkisMain,
    outlineVariant = Color(0xFF424242),
    scrim = Color(0xFF000000)
)

// #################################################################################################
// ######################################### DUNKEL THEME ##########################################
// #################################################################################################
private val TuerkisDunkelColorScheme = darkColorScheme(
    primary = TuerkisMain, // Helleres Türkis fuer Dark Mode Primary
    onPrimary = Color(0xFF000000), // Schwarzer Text auf primärer Farbe
    primaryContainer = TuerkisDark, // Dunklerer Hintergrund fuer Container
    onPrimaryContainer = Color(0xFFFFFFFF), // Weißer Text auf Container-Hintergrund

    secondary = TuerkisMain, // Sekundärfarbe
    onSecondary = Color(0xFF000000),
    secondaryContainer = TuerkisDark,
    onSecondaryContainer = Color(0xFFFFFFFF),

    tertiary = Color(0xFFBB86FC),
    onTertiary = Color(0xFF000000),
    tertiaryContainer = Color(0xFF6200EE),
    onTertiaryContainer = Color(0xFFFFFFFF),

    error = Color(0xFFCF6679),
    onError = Color(0xFF000000),
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color(0xFFFFFFFF),

    background = Color(0xFF121212), // Dunkles Grau als Hintergrund
    onBackground = Color(0xFFFFFFFF), // Weißer Text auf Hintergrund
    surface = Color(0xFF121212), // Dunkles Grau als Oberflächenfarbe
    onSurface = Color(0xFFFFFFFF), // Weißer Text auf Oberflächenfarbe
    surfaceVariant = Color(0xFF424242),
    onSurfaceVariant = Color(0xFFBDBDBD),
    outline = Color(0xFF9E9E9E),
    inverseOnSurface = Color(0xFFE0E0E0),
    inverseSurface = Color(0xFF212121),
    inversePrimary = TuerkisDark, // Inverse Primärfarbe
    surfaceTint = TuerkisDark,
    outlineVariant = Color(0xFF424242),
    scrim = Color(0xFF000000)
)


@Composable
fun TuerkisTheme(
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

        darkTheme -> TuerkisDunkelColorScheme
        else -> TuerkisHellColorScheme
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
        typography = Typography, // Stellen Sie sicher, dass Typography importiert ist (aus BuyPalTheme.kt oder einer separaten Typography.kt)
        content = content
    )
}
