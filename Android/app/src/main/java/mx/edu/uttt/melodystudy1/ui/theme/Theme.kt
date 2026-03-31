package mx.edu.uttt.melodystudy1.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val AppColorScheme = lightColorScheme(
    primary = PurpleMain,
    secondary = BlueSoft,
    background = BackgroundSoft,
    surface = BackgroundSoft
)

@Composable
fun MelodyStudyTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}