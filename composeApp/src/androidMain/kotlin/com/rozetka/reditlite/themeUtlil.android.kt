package com.rozetka.reditlite

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import android.os.Build
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun getDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    val dynamicColor = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    return when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        else -> null
    }
}