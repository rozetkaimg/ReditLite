package com.rozetka.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun MediaViewer(
    mediaItems: List<String>,
    startIndex: Int = 0,
    onDismiss: () -> Unit,
    onPageChanged: ((Int) -> Unit)? = null,
    showImageNumber: Boolean = true,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    isAlwaysVideo: Boolean = false,
    fileIds: List<Long> = emptyList(),
    captions: List<String?> = emptyList()
) {
    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, mediaItems.lastIndex.coerceAtLeast(0)),
        pageCount = { mediaItems.size }
    )
    
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged?.invoke(pagerState.currentPage)
    }

    BackHandler(onBack = onDismiss)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 1f))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { page -> 
                val id = fileIds.getOrNull(page) ?: 0L
                val path = mediaItems.getOrNull(page) ?: ""
                if (id != 0L) "msg_$id" else "path_$path"
            },
            userScrollEnabled = true,
            pageSpacing = 16.dp,
            beyondViewportPageCount = 1
        ) { page ->
            val scope = rememberCoroutineScope()
            val zoomState = rememberZoomState()
            val rootState = rememberDismissRootState()

            LaunchedEffect(Unit) {
                launch {
                    rootState.scale.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium))
                }
                launch {
                    rootState.backgroundAlpha.animateTo(1f, tween(150))
                }
            }

            val rawUrl = mediaItems.getOrNull(page) ?: return@HorizontalPager
            val url = rawUrl.replace("&amp;", "&")
            val isVideo = isAlwaysVideo || isVideoPath(url)
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = rootState.backgroundAlpha.value))
                    .graphicsLayer {
                        translationY = rootState.offsetY.value
                        scaleX = rootState.scale.value
                        scaleY = rootState.scale.value
                    }
            ) {
                MediaPage(
                    url = url,
                    isVideo = isVideo,
                    zoomState = zoomState,
                    rootState = rootState,
                    onDismiss = onDismiss,
                    showControls = showControls,
                    onToggleControls = { showControls = !showControls },
                    isActive = pagerState.currentPage == page,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }

                if (showImageNumber && mediaItems.size > 1) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 32.dp)
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

                val currentCaption = captions.getOrNull(pagerState.currentPage)
                if (!currentCaption.isNullOrBlank()) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Text(
                            text = currentCaption,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun MediaPage(
    url: String,
    isVideo: Boolean,
    zoomState: ZoomState,
    rootState: DismissRootState,
    onDismiss: () -> Unit,
    showControls: Boolean,
    onToggleControls: () -> Unit,
    isActive: Boolean,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    val scope = rememberCoroutineScope()
    var isMuted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isMuted = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        if (zoomState.scale.value > 1f) {
                            zoomState.onDoubleTap(scope, offset, 1f, IntSize(size.width, size.height))
                        } else {
                            zoomState.onDoubleTap(scope, offset, 3f, IntSize(size.width, size.height))
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectZoomAndDismissGestures(
                    zoomState = zoomState,
                    rootState = rootState,
                    screenHeightPx = size.height.toFloat(),
                    dismissThreshold = 300f,
                    dismissVelocityThreshold = 1000f,
                    onDismiss = onDismiss,
                    scope = scope
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (isVideo) {
            VideoPlayer(
                url = url,
                isMuted = isMuted,
                autoPlay = isActive,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoomState.scale.value
                        scaleY = zoomState.scale.value
                        translationX = zoomState.offsetX.value
                        translationY = zoomState.offsetY.value
                    }
            )
            
            if (showControls) {
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(0.5f), CircleShape)
                ) {
                    Icon(
                        if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        null,
                        tint = Color.White
                    )
                }
            }
        } else {
            var contentModifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = zoomState.scale.value
                    scaleY = zoomState.scale.value
                    translationX = zoomState.offsetX.value
                    translationY = zoomState.offsetY.value
                }

            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    contentModifier = contentModifier.sharedElement(
                        state = rememberSharedContentState(key = "media_$url"),
                        animatedVisibilityScope = animatedVisibilityScope
                    )
                }
            }

            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = contentModifier
            )

            if (url.lowercase().contains(".gif")) {
                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .padding(bottom = if (showControls) 48.dp else 0.dp)
                ) {
                    Text(
                        text = "GIF",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

private fun isVideoPath(path: String): Boolean {
    if (path.isBlank()) return false
    val lowerPath = path.lowercase()
    return lowerPath.contains(".mp4") || 
           lowerPath.contains(".m3u8") || 
           lowerPath.contains("video") || 
           lowerPath.contains("v.redd.it") || 
           lowerPath.contains(".mov") ||
           lowerPath.contains(".mkv") ||
           lowerPath.contains(".webm") ||
           lowerPath.contains(".avi") ||
           lowerPath.contains(".3gp") ||
           lowerPath.contains(".m4v") ||
           lowerPath.contains(".gifv")
}
