package com.expenses.mobile.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Ocean80,
    onPrimary = AppText,
    primaryContainer = Ocean40,
    onPrimaryContainer = AppSurface,
    secondary = Indigo80,
    secondaryContainer = Indigo40,
    tertiary = Coral80,
    tertiaryContainer = Coral40,
    error = Expense40,
    background = Color(0xFF101820),
    onBackground = AppSurface,
    surface = Color(0xFF17212B),
    onSurface = AppSurface,
    surfaceVariant = Color(0xFF22303A),
    onSurfaceVariant = Color(0xFFC6D0DB),
    outline = Color(0xFF556372)
)

private val LightColorScheme = lightColorScheme(
    primary = Ocean40,
    onPrimary = AppSurface,
    primaryContainer = OceanContainer,
    onPrimaryContainer = Color(0xFF06323E),
    secondary = Indigo40,
    onSecondary = AppSurface,
    secondaryContainer = IndigoContainer,
    onSecondaryContainer = Color(0xFF17275E),
    tertiary = Coral40,
    onTertiary = AppSurface,
    tertiaryContainer = CoralContainer,
    onTertiaryContainer = Color(0xFF4A1E12),
    error = Expense40,
    background = AppBackground,
    onBackground = AppText,
    surface = AppSurface,
    onSurface = AppText,
    surfaceVariant = AppSurfaceVariant,
    onSurfaceVariant = AppTextMuted,
    outline = AppOutline
)

@Composable
fun GestoreSpeseAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
