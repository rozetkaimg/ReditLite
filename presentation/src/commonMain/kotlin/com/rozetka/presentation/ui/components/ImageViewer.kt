package com.rozetka.presentation.ui.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ImageViewer(
    images: List<String>,
    startIndex: Int = 0,
    onDismiss: () -> Unit,
    onPageChanged: ((Int) -> Unit)? = null,
    showImageNumber: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    MediaViewer(
        mediaItems = images,
        startIndex = startIndex,
        onDismiss = onDismiss,
        onPageChanged = onPageChanged,
        showImageNumber = showImageNumber,
        sharedTransitionScope = sharedTransitionScope,
        animatedVisibilityScope = animatedVisibilityScope
    )
}