package com.rozetka.presentation.ui.screen.postDetail

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mohamedrejeb.calf.io.*
import com.mohamedrejeb.calf.picker.FilePickerFileType
import com.mohamedrejeb.calf.picker.FilePickerSelectionMode
import com.mohamedrejeb.calf.picker.rememberFilePickerLauncher
import com.rozetka.domain.model.Comment
import com.rozetka.domain.model.VoteDirection
import com.rozetka.presentation.ui.components.ImageViewer
import com.rozetka.presentation.ui.components.ShareText
import com.rozetka.presentation.ui.components.VideoViewer
import com.rozetka.presentation.ui.screen.feed.components.PostCard
import com.rozetka.presentation.ui.screen.postDetail.components.CommentInput
import com.rozetka.presentation.ui.screen.postDetail.components.CommentItem
import com.rozetka.presentation.ui.screen.postDetail.components.MediaContent
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun PostDetailScreen(
    postId: String,
    viewModel: PostDetailViewModel,
    onBack: () -> Unit,
    onSubredditClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    onToggleBottomBar: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var replyingTo by remember { mutableStateOf<Comment?>(null) }
    var commentText by remember { mutableStateOf("") }
    
    val sortOptions = listOf("best", "top", "new", "controversial")
    val sortLabels = mapOf(
        "best" to Res.string.sort_best,
        "top" to Res.string.sort_top,
        "new" to Res.string.sort_new,
        "controversial" to Res.string.sort_controversial
    )

    val scope = rememberCoroutineScope()
    val pickerLauncher = rememberFilePickerLauncher(
        type = FilePickerFileType.Image,
        selectionMode = FilePickerSelectionMode.Single,
        onResult = { files ->
            files.firstOrNull()?.let { file ->
                scope.launch {
                    val bytes = file.readByteArray()
                    viewModel.onMediaSelected(bytes, "image.jpg")
                }
            }
        }
    )

    var shareTextData by remember { mutableStateOf<Pair<String, String>?>(null) }
    var fullscreenMediaUrl by remember { mutableStateOf<String?>(null) }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val lazyListState = rememberLazyListState()

    shareTextData?.let { data ->
        ShareText(text = data.first, title = data.second)
        LaunchedEffect(data) { shareTextData = null }
    }

    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = fullscreenMediaUrl,
            label = "fullscreen_transition",
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }
        ) { targetUrl ->
            if (targetUrl == null) {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(Res.string.post_detail_title), style = MaterialTheme.typography.titleMedium) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                            },
                            actions = {
                                val sharePostTitle = stringResource(Res.string.share_post)
                                IconButton(onClick = {
                                    if (state is PostDetailState.Content) {
                                        val post = (state as PostDetailState.Content).post
                                        val postUrl = "https://reddit.com/comments/${post.id}"
                                        shareTextData = ("${post.title}\n$postUrl" to sharePostTitle)
                                    }
                                }) {
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
                                                        text = stringResource(sortLabels[sort] ?: Res.string.sort_best),
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
                            },
                            scrollBehavior = scrollBehavior,
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                                scrolledContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize()) {
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
                                    state = lazyListState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        top = paddingValues.calculateTopPadding(),
                                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 180.dp
                                    )
                                ) {
                                    item {
                                        PostCard(
                                            post = s.post,
                                            onPostClick = {},
                                            onSubredditClick = onSubredditClick,
                                            onUserClick = onUserClick,
                                            onVote = { directionInt ->
                                                val targetDirection = when {
                                                    directionInt > 0 -> VoteDirection.UP
                                                    directionInt < 0 -> VoteDirection.DOWN
                                                    else -> VoteDirection.NONE
                                                }
                                                viewModel.votePost(targetDirection)
                                            },
                                            onSaveClick = { viewModel.toggleSavePost() },
                                            onMediaClick = { url ->
                                                fullscreenMediaUrl = url
                                                onToggleBottomBar(false)
                                            },
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedContent
                                        )

                                        MediaContent(
                                            rawText = s.post.text,
                                            onMediaClick = { url ->
                                                fullscreenMediaUrl = url
                                                onToggleBottomBar(false)
                                            },
                                            onSubredditClick = onSubredditClick,
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                        )

                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                                    }

                                    items(flatComments, key = { it.id }) { comment ->
                                        CommentItem(
                                            comment = comment,
                                            isCollapsed = s.collapsedIds.contains(comment.id),
                                            onCollapseToggle = { viewModel.toggleCommentCollapse(comment.id) },
                                            onVote = { direction -> viewModel.voteComment(comment.id, direction) },
                                            onReply = { replyingTo = it },
                                            onMediaClick = { url ->
                                                fullscreenMediaUrl = url
                                                onToggleBottomBar(false)
                                            },
                                            onSubredditClick = onSubredditClick,
                                            onUserClick = onUserClick
                                        )
                                    }
                                }

                                val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                                val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .imePadding()
                                        .padding(
                                            bottom = if (isImeVisible) 8.dp else (navBarHeight + 84.dp),
                                            start = 16.dp,
                                            end = 16.dp
                                        )
                                ) {
                                    CommentInput(
                                        text = commentText,
                                        onTextChange = { commentText = it },
                                        replyingTo = replyingTo,
                                        attachedMedia = selectedMedia,
                                        onCancelReply = { replyingTo = null },
                                        onRemoveMedia = { viewModel.clearMedia() },
                                        onAttachClick = { pickerLauncher.launch() },
                                        onSend = {
                                            val parentId = replyingTo?.id ?: s.post.id
                                            viewModel.submitComment(parentId, commentText)
                                            commentText = ""
                                            replyingTo = null
                                        }
                                    )
                                }
                            }
                            is PostDetailState.Error -> {
                                Box(Modifier.fillMaxSize(), Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = s.message, color = MaterialTheme.colorScheme.error)
                                        Button(onClick = { viewModel.loadPostDetail(postId) }) {
                                            Text(stringResource(Res.string.retry))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    val isVideo = targetUrl.lowercase().let { it.contains(".mp4") || it.contains(".gifv") || it.contains(".webm") || it.contains("v.redd.it") }
                    if (isVideo) {
                        VideoViewer(
                            url = targetUrl,
                            onDismiss = {
                                fullscreenMediaUrl = null
                                onToggleBottomBar(true)
                            },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent
                        )
                    } else {
                        ImageViewer(
                            images = listOf(targetUrl),
                            onDismiss = {
                                fullscreenMediaUrl = null
                                onToggleBottomBar(true)
                            },
                            sharedTransitionScope = this@SharedTransitionLayout,
                            animatedVisibilityScope = this@AnimatedContent
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