package com.rozetka.presentation.ui.screen.feed.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mikepenz.markdown.m3.Markdown
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.VoteDirection
import com.rozetka.presentation.generated.resources.*
import com.rozetka.presentation.ui.components.ShareText
import com.rozetka.presentation.ui.components.VideoPlayer
import org.jetbrains.compose.resources.stringResource

fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
    background(brush)
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    onPostClick: () -> Unit,
    onVote: (Int) -> Unit,
    onSaveClick: () -> Unit,
    onMediaClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onSubredditClick: (String) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    var shareTextData by remember { mutableStateOf<String?>(null) }

    shareTextData?.let { text ->
        ShareText(text = text, title = "Share Post")
        LaunchedEffect(text) {
            shareTextData = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            onClick = onPostClick
        ) {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 0.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "r/${post.subreddit}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onSubredditClick(post.subreddit) }
                        )
                        Text(
                            text = " • ",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.by_author, post.author),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable { onUserClick(post.author) }
                        )
                    }
                    IconButton(onClick = onSaveClick, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = if (post.isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = null,
                            tint = if (post.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val decodedTitle = remember(post.title) {
                    post.title.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                }

                Markdown(
                    content = decodedTitle,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        val mediaUrl = post.mediaUrl
        val videoUrl = post.videoUrl
        val galleryUrls = post.galleryUrls

        if (post.isVideo || mediaUrl != null || !galleryUrls.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                if (post.isVideo && videoUrl != null) {
                    var isMuted by remember { mutableStateOf(true) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f/9f)
                            .background(Color.Black)
                            .clickable { onMediaClick(videoUrl) },
                        contentAlignment = Alignment.Center
                    ) {
                        VideoPlayer(
                            url = videoUrl,
                            isMuted = isMuted,
                            modifier = Modifier.fillMaxSize(),
                            autoPlay = true
                        )

                        IconButton(
                            onClick = { isMuted = !isMuted },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(0.5f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else if (!galleryUrls.isNullOrEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { galleryUrls.size })
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(16f/9f)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(galleryUrls[page])
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().clickable { onMediaClick(galleryUrls[page]) },
                                loading = { Box(Modifier.fillMaxSize().shimmerEffect()) },
                                error = {
                                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.BrokenImage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            )
                        }
                        if (galleryUrls.size > 1) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape,
                                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
                            ) {
                                Text(
                                    text = "${pagerState.currentPage + 1} / ${galleryUrls.size}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                } else if (mediaUrl != null) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current).data(mediaUrl).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f/9f)
                            .clickable { onMediaClick(mediaUrl) },
                        loading = { Box(Modifier.fillMaxSize().shimmerEffect()) },
                        error = {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.BrokenImage, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 28.dp, bottomEnd = 28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            onClick = onPostClick
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(36.dp).padding(horizontal = 4.dp)
                    ) {
                        IconButton(
                            onClick = { onVote(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowUp,
                                null,
                                tint = if (post.voteStatus == VoteDirection.UP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "${post.score}",
                            style = MaterialTheme.typography.labelLarge,
                            color = when(post.voteStatus) {
                                VoteDirection.UP -> MaterialTheme.colorScheme.primary
                                VoteDirection.DOWN -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        IconButton(
                            onClick = { onVote(-1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Outlined.KeyboardArrowDown,
                                null,
                                tint = if (post.voteStatus == VoteDirection.DOWN) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.clip(CircleShape).clickable { onPostClick() }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(36.dp).padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${post.commentsCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.clip(CircleShape).clickable { shareTextData = post.postUrl }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.height(36.dp).padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}