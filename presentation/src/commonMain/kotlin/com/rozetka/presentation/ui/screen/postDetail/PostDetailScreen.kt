package com.rozetka.presentation.ui.screen.postDetail

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mikepenz.markdown.m3.Markdown
import com.rozetka.domain.model.Comment
import com.rozetka.domain.model.VoteDirection
import com.rozetka.presentation.ui.components.VideoPlayer
import com.rozetka.presentation.ui.screen.feed.components.PostCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: PostDetailViewModel,
    onBack: () -> Unit,
    onSubredditClick: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }
    var commentText by remember { mutableStateOf("") }
    val sortOptions = listOf("best", "top", "new", "controversial")

    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { Text("Post", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                actions = {
                    IconButton(onClick = { }) {
                        Icon(Icons.Outlined.Share, null)
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.MoreVert, null)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            sortOptions.forEach { sort ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = sort.replaceFirstChar { it.uppercase() },
                                            fontWeight = if (sort == currentSort) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        showSortMenu = false
                                        viewModel.loadPostDetail(postId, sort)
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state is PostDetailState.Content) {
                CommentInput(
                    text = commentText,
                    onTextChange = { commentText = it },
                    replyingTo = replyingTo,
                    onCancelReply = { replyingTo = null },
                    onSend = {
                        val parentId = replyingTo?.id ?: (state as PostDetailState.Content).post.id
                        viewModel.submitComment(parentId, commentText)
                        commentText = ""
                        replyingTo = null
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val s = state) {
                is PostDetailState.Loading -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is PostDetailState.Content -> {
                    val flatComments = remember(s.comments, s.collapsedIds) {
                        flattenComments(s.comments, s.collapsedIds)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            PostCard(
                                post = s.post,
                                onPostClick = {},
                                onSubredditClick = onSubredditClick,
                                onVote = { direction ->
                                    viewModel.votePost(if (direction > 0) VoteDirection.UP else VoteDirection.DOWN)
                                },
                                onSaveClick = { viewModel.toggleSavePost() },
                                onMediaClick = {}
                            )

                            if (!s.post.text.isNullOrBlank()) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    Markdown(content = s.post.text!!)
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                        }

                        items(flatComments, key = { it.id }) { comment ->
                            CommentItem(
                                comment = comment,
                                isCollapsed = s.collapsedIds.contains(comment.id),
                                onCollapseToggle = { viewModel.toggleCommentCollapse(comment.id) },
                                onVote = { direction -> viewModel.voteComment(comment.id, direction) },
                                onReply = { replyingTo = it }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
                is PostDetailState.Error -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = s.message, color = MaterialTheme.colorScheme.error)
                            Button(onClick = { viewModel.loadPostDetail(postId) }) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentInput(
    text: String,
    onTextChange: (String) -> Unit,
    replyingTo: Comment?,
    onCancelReply: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.imePadding()
    ) {
        Column {
            AnimatedVisibility(visible = replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Replying to u/${replyingTo?.author}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Close, null, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = text,
                    onValueChange = onTextChange,
                    placeholder = { Text("Add a comment") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    maxLines = 5
                )

                IconButton(
                    onClick = onSend,
                    enabled = text.isNotBlank(),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null)
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    isCollapsed: Boolean,
    onCollapseToggle: () -> Unit,
    onVote: (VoteDirection) -> Unit,
    onReply: (Comment) -> Unit
) {
    if (comment.author.isEmpty() && comment.body == "Load more comments...") {
        TextButton(
            onClick = { },
            modifier = Modifier
                .padding(start = (comment.depth * 12).dp + 16.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                text = comment.body,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        return
    }

    val depthColors = listOf(
        Color(0xFF3498DB),
        Color(0xFF2ECC71),
        Color(0xFFF1C40F),
        Color(0xFFE74C3C),
        Color(0xFF9B59B6),
        Color(0xFFE67E22)
    )

    val depthOffset = (comment.depth * 12).dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCollapseToggle() }
            .padding(start = depthOffset)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (comment.depth > 0) {
                val lineColor = depthColors[(comment.depth - 1) % depthColors.size]

                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(lineColor, RoundedCornerShape(1.5.dp))
                )
                Spacer(Modifier.width(12.dp))
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = comment.authorIconUrl ?: "https://www.redditstatic.com/avatars/defaults/v2/avatar_default_0.png",
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "u/${comment.author}",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
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
                    Spacer(Modifier.height(4.dp))
                    Markdown(
                        content = comment.body,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onVote(VoteDirection.UP) }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (comment.voteStatus == VoteDirection.UP) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowUp,
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

                        IconButton(onClick = { onVote(VoteDirection.DOWN) }, modifier = Modifier.size(32.dp)) {
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

                        Icon(
                            Icons.Outlined.MoreHoriz,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun flattenComments(comments: List<Comment>, collapsedIds: Set<String>): List<Comment> {
    val result = mutableListOf<Comment>()
    fun traverse(comment: Comment) {
        result.add(comment)
        if (!collapsedIds.contains(comment.id)) {
            comment.replies.forEach { traverse(it) }
        }
    }
    comments.forEach { traverse(it) }
    return result
}

private fun countReplies(comment: Comment): Int {
    var count = comment.replies.size
    comment.replies.forEach { count += countReplies(it) }
    return count
}