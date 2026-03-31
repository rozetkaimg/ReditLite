package com.rozetka.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment

@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    isMuted: Boolean,
    autoPlay: Boolean
) {
    // Basic placeholder for iOS, can be implemented with AVPlayer/UIKitView
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text("Video Player not implemented for iOS yet")
    }
}
