package com.rozetka.presentation.ui.screen.postDetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.mikepenz.markdown.m3.Markdown
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.*
import com.rozetka.presentation.ui.components.VideoPlayer
import org.jetbrains.compose.resources.stringResource

@Composable
fun MediaContent(
    rawText: String?,
    mediaUrls: List<String> = emptyList(),
    onMediaClick: (String) -> Unit,
    onSubredditClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (rawText.isNullOrBlank() && mediaUrls.isEmpty()) return

    val text = remember(rawText) {
        rawText?.replace("&amp;", "&")?.replace("&lt;", "<")?.replace("&gt;", ">") ?: ""
    }

    val urlRegex = "(https?://[^\\s\\]\\)\"]+)".toRegex()
    val extractedMediaUrls = remember(text) {
        urlRegex.findAll(text).map { it.value }.filter { url ->
            val lower = url.lowercase()
            lower.contains(".jpg") || lower.contains(".png") || lower.contains(".jpeg") ||
                    lower.contains(".gif") || lower.contains(".webp") || lower.contains(".mp4") ||
                    lower.contains("preview.redd.it") || lower.contains("i.redd.it") || lower.contains("v.redd.it") || 
                    lower.contains("reddit.com/media") || lower.contains("external-preview.redd.it")
        }.toList()
    }

    val allMediaUrls = remember(extractedMediaUrls, mediaUrls) {
        (extractedMediaUrls + mediaUrls).distinct()
    }

    val redditGifRegex = "!\\[gif\\]\\(giphy\\|([^\\)]+)\\)".toRegex()
    val subredditRegex = "(https?://(?:www\\.)?reddit\\.com/r/([a-zA-Z0-9_]+)[/\\w-]*|(?<![\\w/])r/([a-zA-Z0-9_]+))".toRegex(RegexOption.IGNORE_CASE)
    val subreddits = remember(text) {
        subredditRegex.findAll(text).map {
            it.groupValues[2].takeIf { g -> g.isNotEmpty() } ?: it.groupValues[3]
        }.distinct().toList()
    }

    val displayBody = remember(text, allMediaUrls, subreddits) {
        var t = text
        allMediaUrls.forEach { url -> t = t.replace(url, "") }
        subredditRegex.findAll(text).forEach { match -> t = t.replace(match.value, "") }
        t = redditGifRegex.replace(t, "")
        t.replace("()", "").replace("[]", "").trim()
    }

    Column(modifier = modifier) {
        if (displayBody.isNotBlank()) {
            Markdown(
                content = displayBody,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (subreddits.isNotEmpty()) {
            subreddits.forEach { subreddit ->
                SubredditLinkCard(
                    subreddit = subreddit,
                    onClick = { onSubredditClick(subreddit) }
                )
            }
        }

        if (allMediaUrls.isNotEmpty()) {
            allMediaUrls.forEach { url ->
                val isVideo = url.lowercase().let { 
                    it.contains(".mp4") || it.contains(".gifv") || it.contains(".webm") || 
                    it.contains("v.redd.it") || it.contains("format=mp4") 
                }
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .clickable { onMediaClick(url) }
                ) {
                    if (isVideo) {
                        VideoPlayer(
                            url = url,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            isMuted = true,
                            autoPlay = true
                        )
                    } else {
                        val isGif = url.lowercase().contains(".gif")
                        Box {
                            SubcomposeAsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 350.dp),
                                contentScale = ContentScale.FillWidth,
                                loading = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                error = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(100.dp)
                                            .background(MaterialTheme.colorScheme.errorContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                            Text(
                                                text = stringResource(Res.string.error_loading_media),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            )
                            if (isGif) {
                                Surface(
                                    color = Color.Black.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
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
            }
        }
    }
}

@Composable
fun SubredditLinkCard(subreddit: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https://www.redditstatic.com/avatars/defaults/v2/avatar_default_0.png",
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.subreddit_handle, subreddit),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = stringResource(Res.string.view_button),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}