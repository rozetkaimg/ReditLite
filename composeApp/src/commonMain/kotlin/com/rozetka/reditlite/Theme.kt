package com.rozetka.reditlite

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RedditOrange = Color(0xFFFF4500)
val RedditBlue = Color(0xFF0079D3)

private val RedditDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF5414),
    onPrimary = Color(0xFF4A1000),
    primaryContainer = Color(0xFF751A00),
    onPrimaryContainer = Color(0xFFFFDBCF),
    secondary = Color(0xFF63B4FF),
    onSecondary = Color(0xFF00325D),
    secondaryContainer = Color(0xFF004982),
    onSecondaryContainer = Color(0xFFD1E4FF),
    tertiary = Color(0xFFBCC7D3),
    onTertiary = Color(0xFF25313D),
    tertiaryContainer = Color(0xFF3B4754),
    onTertiaryContainer = Color(0xFFD7E8F7),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0F0F10),
    onBackground = Color(0xFFE1E2E3),
    surface = Color(0xFF0F0F10),
    onSurface = Color(0xFFE1E2E3),
    surfaceVariant = Color(0xFF272729),
    onSurfaceVariant = Color(0xFFC0C3C5),
    outline = Color(0xFF8B8E90),
    outlineVariant = Color(0xFF414345)
)

private val RedditLightColorScheme = lightColorScheme(
    primary = RedditOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDBCF),
    onPrimaryContainer = Color(0xFF3B0A00),
    secondary = Color(0xFF0061A9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1E4FF),
    onSecondaryContainer = Color(0xFF001D36),
    tertiary = Color(0xFF536471),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD7E8F7),
    onTertiaryContainer = Color(0xFF0F1F2B),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBFBFB),
    onBackground = Color(0xFF1C1C1C),
    surface = Color(0xFFFBFBFB),
    onSurface = Color(0xFF1C1C1C),
    surfaceVariant = Color(0xFFEDEFF1),
    onSurfaceVariant = Color(0xFF505457),
    outline = Color(0xFF818486),
    outlineVariant = Color(0xFFD7DADC)
)

@Composable
expect fun platformColorScheme(
    darkTheme: Boolean,
    dynamicColor: Boolean
): ColorScheme?

@Composable
fun ReditLiteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = platformColorScheme(darkTheme, dynamicColor)
        ?: if (darkTheme) RedditDarkColorScheme else RedditLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}