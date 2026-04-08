package com.rozetka.presentation.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberZoomState(): ZoomState {
    return remember { ZoomState() }
}

class ZoomState {
    val scale = Animatable(1f)
    val offset = Animatable(Offset.Zero, Offset.VectorConverter)

    suspend fun reset(scope: CoroutineScope) {
        scope.launch {
            scale.animateTo(1f)
        }
        scope.launch {
            offset.animateTo(Offset.Zero)
        }
    }

    fun resetInstant(scope: CoroutineScope) {
        scope.launch {
            scale.snapTo(1f)
            offset.snapTo(Offset.Zero)
        }
    }
}

@Composable
fun rememberDismissRootState(): DismissRootState {
    return remember { DismissRootState() }
}

class DismissRootState {
    val offsetY = Animatable(0f)
    val scale = Animatable(1f)
    val backgroundAlpha = Animatable(0f)

    suspend fun reset(scope: CoroutineScope) {
        scope.launch {
            offsetY.animateTo(0f)
        }
        scope.launch {
            scale.animateTo(1f)
        }
        scope.launch {
            backgroundAlpha.animateTo(1f)
        }
    }

    fun resetInstant(scope: CoroutineScope) {
        scope.launch {
            offsetY.snapTo(0f)
            scale.snapTo(1f)
            backgroundAlpha.snapTo(1f)
        }
    }
}
