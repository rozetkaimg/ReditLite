package com.rozetka.presentation.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MediaViewer(
    mediaItems: List<String>,
    startIndex: Int = 0,
    onDismiss: () -> Unit,
    onPageChanged: ((Int) -> Unit)? = null,
    showImageNumber: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    BackHandler(onBack = onDismiss)

    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { mediaItems.size })

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged?.invoke(pagerState.currentPage)
        scale = 1f
        offset = Offset.Zero
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = scale == 1f
        ) { page ->
            val url = mediaItems[page]
            val isVideo = url.contains(".mp4") || url.contains(".m3u8") || url.contains("video")
            val isCurrentPage = pagerState.currentPage == page

            if (isVideo) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCurrentPage) {
                        VideoPlayer(
                            url = url,
                            isMuted = false,
                            autoPlay = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                if (scale > 1f) {
                                    offset += pan
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val contentModifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )

                    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                        with(sharedTransitionScope) {
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Fit,
                                modifier = contentModifier
                                    .sharedElement(
                                        rememberSharedContentState(key = "image_$url"),
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                            )
                        }
                    } else {
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = contentModifier
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .padding(top = 16.dp, start = 16.dp)
                .statusBarsPadding()
                .align(Alignment.TopStart)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        if (showImageNumber && mediaItems.size > 1) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 32.dp)
                    .navigationBarsPadding()
                    .align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${mediaItems.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}