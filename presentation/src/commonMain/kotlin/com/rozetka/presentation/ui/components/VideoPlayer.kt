package com.rozetka.presentation.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    isMuted: Boolean = true,
    autoPlay: Boolean = true
)
