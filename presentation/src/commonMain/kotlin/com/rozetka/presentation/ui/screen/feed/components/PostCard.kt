package com.rozetka.presentation.ui.screen.feed.components

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rozetka.domain.model.Post
import com.rozetka.presentation.ui.components.VideoPlayer

import org.jetbrains.compose.resources.stringResource
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.*

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PostCard(
    post: Post,
    onPostClick: () -> Unit,
    onVote: (Int) -> Unit,
    onSaveClick: () -> Unit,
    onMediaClick: (String) -> Unit,
    onSubredditClick: (String) -> Unit = {},
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedVisibilityScope: AnimatedVisibilityScope? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {onPostClick()}
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "r/${post.subreddit} • ${stringResource(Res.string.by_author, post.author)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { onSubredditClick(post.subreddit) }
                )

                IconButton(
                    onClick = onSaveClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (post.isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (post.isSaved) stringResource(Res.string.unsave) else stringResource(Res.string.save),
                        tint = if (post.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = post.title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            val videoUrl = post.videoUrl
            val mediaUrl = post.mediaUrl

            if (post.isVideo && videoUrl != null) {
                var isMuted by remember { mutableStateOf(true) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 250.dp, max = 500.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    VideoPlayer(
                        url = videoUrl,
                        isMuted = isMuted,
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onMediaClick(videoUrl) }
                    )

                    IconButton(
                        onClick = { isMuted = !isMuted },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Outlined.VolumeOff else Icons.AutoMirrored.Outlined.VolumeUp,
                            contentDescription = if (isMuted) stringResource(Res.string.unmute) else stringResource(Res.string.mute),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else if (mediaUrl != null) {
                val imageModifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onMediaClick(mediaUrl) }

                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        AsyncImage(
                            model = mediaUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = imageModifier
                                .sharedElement(
                                    rememberSharedContentState(key = "image_$mediaUrl"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                        )
                    }
                } else {
                    AsyncImage(
                        model = mediaUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = imageModifier
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowUp,
                            contentDescription = stringResource(Res.string.upvote),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onVote(1) }
                        )

                        Text(
                            text = "${post.score}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowDown,
                            contentDescription = stringResource(Res.string.downvote),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable { onVote(-1) }
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onPostClick() }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChatBubbleOutline,
                            contentDescription = stringResource(Res.string.comments),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${post.commentsCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}