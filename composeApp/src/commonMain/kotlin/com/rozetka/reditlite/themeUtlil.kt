package com.rozetka.reditlite

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable

@Composable
expect fun getDynamicColorScheme(darkTheme: Boolean): ColorScheme?