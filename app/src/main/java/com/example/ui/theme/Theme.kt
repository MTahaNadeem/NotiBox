package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NotiBoxDarkColorScheme = darkColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = OceanBlueLight,
    onPrimaryContainer = Color.White,
    secondary = CyanAccent,
    onSecondary = Color.Black,
    secondaryContainer = CyanAccent,
    onSecondaryContainer = Color.Black,
    tertiary = TextTertiary,
    onTertiary = Color.White,
    background = AmoledBackground,
    onBackground = TextPrimary,
    surface = AmoledSurface,
    onSurface = TextPrimary,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OutlineBorder,
    outlineVariant = OutlineSubtle,
    error = DeleteRed,
    onError = Color.White
)

val NotiBoxLightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    primaryContainer = IndigoLight,
    onPrimaryContainer = Color.White,
    secondary = CyanAccent,
    onSecondary = Color.White,
    secondaryContainer = CyanAccent,
    onSecondaryContainer = Color.White,
    tertiary = LightTextTertiary,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutlineBorder,
    outlineVariant = LightOutlineSubtle,
    error = DeleteRed,
    onError = Color.White
)

@Composable
fun NotiBoxTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NotiBoxDarkColorScheme else NotiBoxLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

