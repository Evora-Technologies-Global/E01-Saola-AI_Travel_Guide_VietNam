package com.evora.technologies.saola.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.evora.technologies.saola.core.designsystem.component.DefaultSystemBarIcons
import com.evora.technologies.saola.domain.model.ThemePreference

private val LightColors = lightColorScheme(
    primary = LacquerRed,
    onPrimary = Color.White,
    primaryContainer = LacquerContainer,
    onPrimaryContainer = LacquerRedDark,
    secondary = TempleGold,
    onSecondary = Color.White,
    secondaryContainer = TempleGoldContainer,
    onSecondaryContainer = TempleGoldContainerDark,
    tertiary = Jade,
    onTertiary = Color.White,
    tertiaryContainer = JadeContainer,
    onTertiaryContainer = JadeContainerDark,
    background = SandLightest,
    onBackground = InkDarkest,
    surface = SandLightest,
    onSurface = InkDarkest,
    surfaceVariant = SandLight,
    onSurfaceVariant = InkSoft,
    surfaceContainer = SandLight,
    surfaceContainerHigh = SandMid,
    outline = Color(0xFF857468),
    outlineVariant = SandMid,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = LacquerContainerDark,
    onPrimaryContainer = LacquerContainer,
    secondary = TempleGoldLight,
    onSecondary = Color(0xFF3D2E00),
    secondaryContainer = TempleGoldContainerDark,
    onSecondaryContainer = TempleGoldContainer,
    tertiary = Color(0xFF9DDCC9),
    onTertiary = Color(0xFF003730),
    tertiaryContainer = JadeContainerDark,
    onTertiaryContainer = JadeContainer,
    background = InkDarkest,
    onBackground = SandLight,
    surface = InkDarkest,
    onSurface = SandLight,
    surfaceVariant = InkMid,
    onSurfaceVariant = Color(0xFFD5C3B7),
    surfaceContainer = InkDark,
    surfaceContainerHigh = InkMid,
    outline = Color(0xFF9E8D82),
    outlineVariant = InkSoft,
    error = ErrorRedDark,
    onError = Color(0xFF690005),
    errorContainer = ErrorContainerDark,
    onErrorContainer = ErrorContainerLight,
)

/**
 * Two schemes, both hand-built, and no wallpaper palette on any Android version.
 *
 * The lacquer-and-gold colours are part of what the app *is* — the passport's red
 * bar, the sovereignty seal and the category accents are drawn in them outright —
 * so the only thing the traveller chooses here is light or dark. A wallpaper-derived
 * scheme would wash those accents out and make the same screen look like a different
 * app on the next phone.
 */
@Composable
fun SaolaTheme(
    themePreference: ThemePreference = ThemePreference.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val colorScheme = if (darkTheme) DarkColors else LightColors

    // The status bar is transparent and every page runs under it, so its icons are read
    // against the scheme's own background and have to be told which one that is. Here rather
    // than in `MainActivity` or `SaolaViewController` because this is the one place the
    // preference is resolved into an answer — a host that repeated the `when` above would be
    // a second answer to the same question, and iOS and Android would drift apart on it.
    // Screens that paint their own background regardless of the theme pin over this; see
    // `PinSystemBarIcons`.
    DefaultSystemBarIcons(darkIcons = !darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SaolaTypography,
        shapes = SaolaShapes,
        content = content,
    )
}
