package com.rozetka.reditlite.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun SwipeBackWrapper(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
)
