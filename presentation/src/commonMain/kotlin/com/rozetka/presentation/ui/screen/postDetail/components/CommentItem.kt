package com.rozetka.presentation.ui.screen.postDetail.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import coil3.network.HttpException
import com.rozetka.domain.model.Comment
import com.rozetka.domain.model.VoteDirection
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.*
import com.rozetka.presentation.ui.components.ShareText
import org.jetbrains.compose.resources.stringResource

@Composable
fun CommentItem(
    comment: Comment,
    isCollapsed: Boolean,
    onCollapseToggle: () -> Unit,
    onVote: (VoteDirection) -> Unit,
    onReply: (Comment) -> Unit,
    onMediaClick: (String) -> Unit,
    onSubredditClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {}
) {
    if (comment.author.isEmpty() && comment.body == "Load more comments...") {
        TextButton(
            onClick = { },
            modifier = Modifier.padding(start = (comment.depth * 14).dp + 16.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = stringResource(Res.string.load_more_comments),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        return
    }

    val clipboardManager = LocalClipboardManager.current
    var showMoreMenu by remember { mutableStateOf(false) }

    var shareTextData by remember { mutableStateOf<String?>(null) }
    shareTextData?.let { text ->
        ShareText(text = text, title = stringResource(Res.string.share_comment))
        LaunchedEffect(text) { shareTextData = null }
    }

    val depthColors = listOf(
        Color(0xFF3498DB), Color(0xFF2ECC71), Color(0xFFF1C40F),
        Color(0xFFE74C3C), Color(0xFF9B59B6), Color(0xFFE67E22)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .drawBehind {
                val indent = 14.dp.toPx()
                for (i in 0 until comment.depth) {
                    val x = (i * indent) + (indent / 2)
                    drawLine(
                        color = depthColors[i % depthColors.size].copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }
            .padding(start = (comment.depth * 14).dp + 8.dp, end = 8.dp)
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCollapseToggle() }
                .padding(vertical = 4.dp)
        ) {
            val authorIconUrl = remember(comment.authorIconUrl) {
                comment.authorIconUrl?.replace("&amp;", "&") ?: "https://www.redditstatic.com/avatars/defaults/v2/avatar_default_0.png"
            }

            SubcomposeAsyncImage(
                model = authorIconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onUserClick(comment.author) },
                contentScale = ContentScale.Crop,
                error = { state ->
                    val errorCode = (state.result.throwable as? HttpException)?.response?.code ?: 404
                    AsyncImage(
                        model = "https://http.cat/$errorCode",
                        contentDescription = "Error $errorCode",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "u/${comment.author}",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onUserClick(comment.author) }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "• ${comment.score}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isCollapsed) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        " +${countReplies(comment)} ",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        if (!isCollapsed) {
            MediaContent(
                rawText = comment.body,
                mediaUrls = comment.mediaUrls,
                onMediaClick = onMediaClick,
                onSubredditClick = onSubredditClick,
                modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier.padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val target = if (comment.voteStatus == VoteDirection.UP) VoteDirection.NONE else VoteDirection.UP
                        onVote(target)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.KeyboardArrowUp,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (comment.voteStatus == VoteDirection.UP) Color(0xFFFF4500) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = comment.score.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = when(comment.voteStatus) {
                        VoteDirection.UP -> Color(0xFFFF4500)
                        VoteDirection.DOWN -> Color(0xFF0079D3)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                IconButton(
                    onClick = {
                        val target = if (comment.voteStatus == VoteDirection.DOWN) VoteDirection.NONE else VoteDirection.DOWN
                        onVote(target)
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (comment.voteStatus == VoteDirection.DOWN) Color(0xFF7193FF) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(16.dp))

                IconButton(onClick = { onReply(comment) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Reply,
                        null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(16.dp))

                Box {
                    IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.MoreHoriz,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.copy_text)) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                            onClick = {
                                clipboardManager.setText(AnnotatedString(comment.body))
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(Res.string.share_comment)) },
                            leadingIcon = { Icon(Icons.Outlined.Share, null) },
                            onClick = {
                                shareTextData = comment.body
                                showMoreMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun countReplies(comment: Comment): Int {
    var count = comment.replies.size
    comment.replies.forEach { count += countReplies(it) }
    return count
}