package com.rozetka.reditlite.ui.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
actual fun SwipeBackWrapper(
    modifier: Modifier,
    onBack: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectHorizontalDragGestures { change, dragAmount ->
                // Check if the drag started from the left edge (e.g., first 50dp)
                // and is a swipe to the right
                if (change.position.x < 50.dp.toPx() && dragAmount > 20) {
                    onBack()
                }
            }
        }
    ) {
        content()
    }
}
