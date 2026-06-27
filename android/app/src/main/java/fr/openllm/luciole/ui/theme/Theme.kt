package fr.openllm.luciole.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LucioleColors = lightColorScheme(
    primary = Bleu,
    onPrimary = Surface,
    primaryContainer = BleuClair,
    onPrimaryContainer = Bleu,
    secondary = Or,
    onSecondary = Surface,
    secondaryContainer = OrClair,
    onSecondaryContainer = OrTexte,
    tertiary = Vert,
    onTertiary = Surface,
    tertiaryContainer = VertClair,
    onTertiaryContainer = VertSombre,
    background = Fond,
    onBackground = Encre,
    surface = Surface,
    onSurface = Encre,
    surfaceVariant = BleuClair,
    onSurfaceVariant = TexteMuet,
    outline = Bordure,
    outlineVariant = Bordure,
)

@Composable
fun LucioleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LucioleColors,
        typography = LucioleTypography,
        content = content,
    )
}
