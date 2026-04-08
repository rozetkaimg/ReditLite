package com.rozetka.reditlite.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun SwipeBackWrapper(
    modifier: Modifier,
    onBack: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    // Android handles back button/gestures natively
    content()
}
