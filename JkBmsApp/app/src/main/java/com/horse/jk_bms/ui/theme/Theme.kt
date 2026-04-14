package com.horse.jk_bms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun JkBmsTheme(
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> darkColorScheme(
            primary = Blue200,
            onPrimary = DarkBackground,
            primaryContainer = Blue700,
            onPrimaryContainer = Blue100,
            secondary = Green200,
            onSecondary = DarkBackground,
            secondaryContainer = Green700,
            tertiary = Orange200,
            background = DarkBackground,
            onBackground = DarkOnSurface,
            surface = DarkSurface,
            onSurface = DarkOnSurface,
            surfaceVariant = DarkSurfaceVariant,
            error = Red200,
            onError = DarkBackground,
        )
        else -> lightColorScheme(
            primary = Blue700,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            primaryContainer = Blue100,
            onPrimaryContainer = Blue700,
            secondary = Green700,
            onSecondary = androidx.compose.ui.graphics.Color.White,
            secondaryContainer = Green200,
            tertiary = Orange700,
            background = androidx.compose.ui.graphics.Color.White,
            onBackground = androidx.compose.ui.graphics.Color.Black,
            surface = androidx.compose.ui.graphics.Color.White,
            onSurface = androidx.compose.ui.graphics.Color.Black,
            error = Red700,
            onError = androidx.compose.ui.graphics.Color.White,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
