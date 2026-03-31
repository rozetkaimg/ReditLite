package com.rozetka.presentation.ui.screen.feed

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rozetka.domain.model.FeedType
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.VoteDirection
import com.rozetka.presentation.mvi.FeedIntent
import com.rozetka.presentation.mvi.FeedState
import com.rozetka.presentation.ui.screen.feed.components.PostCard
import com.rozetka.presentation.ui.components.ImageViewer

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onPostClick: (Post) -> Unit,
    onSubredditClick: (String) -> Unit = {},
    onToggleBottomBar: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val listState = rememberLazyListState()

    var isRefreshing by remember { mutableStateOf(false) }
    var selectedFeedType: FeedType by remember { mutableStateOf(FeedType.HOT) }

    var fullscreenMediaUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(state) {
        val isPaginating = (state as? FeedState.Content)?.isPaginating == true
        if (state !is FeedState.Loading && !isPaginating) {
            isRefreshing = false
        }
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = fullscreenMediaUrl,
            label = "fullscreen_transition",
            transitionSpec = {
                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
            }
        ) { targetUrl ->
            if (targetUrl == null) {
                LaunchedEffect(Unit) { onToggleBottomBar(true) }
                Scaffold(
                    topBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            Column {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                                    placeholder = { Text("Search Reddit...") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Search, contentDescription = null)
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(32.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FeedType.entries.forEach { feedType ->
                                        FilterChip(
                                            selected = selectedFeedType == feedType,
                                            onClick = {
                                                if (selectedFeedType != feedType) {
                                                    selectedFeedType = feedType
                                                    viewModel.processIntent(FeedIntent.LoadInitial(feedType))
                                                }
                                            },
                                            label = { Text(feedType.name) },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                ) { paddingValues ->
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            viewModel.processIntent(FeedIntent.Refresh(selectedFeedType))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = paddingValues.calculateTopPadding())
                    ) {
                        when (val currentState = state) {
                            is FeedState.Loading -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is FeedState.Content -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(currentState.posts, key = { it.id }) { post ->
                                        PostCard(
                                            post = post,
                                            onPostClick = { onPostClick(post) },
                                            onVote = { directionInt ->
                                                val voteDirection = when (directionInt) {
                                                    1 -> VoteDirection.UP
                                                    -1 -> VoteDirection.DOWN
                                                    else -> VoteDirection.NONE
                                                }
                                                viewModel.processIntent(FeedIntent.Vote(post, voteDirection))
                                            },
                                            onMediaClick = { url ->
                                                fullscreenMediaUrl = url
                                                onToggleBottomBar(false)
                                            },
                                            onSaveClick = {
                                                viewModel.processIntent(FeedIntent.ToggleSave(post))
                                            },
                                            onSubredditClick = onSubredditClick,
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedContent
                                        )
                                    }
                                    if (currentState.isPaginating) {
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                            }
                                        }
                                    }
                                }

                                LaunchedEffect(listState) {
                                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                                        .collect { lastIndex ->
                                            if (lastIndex != null && lastIndex >= currentState.posts.size - 5) {
                                                viewModel.processIntent(FeedIntent.LoadNextPage(selectedFeedType))
                                            }
                                        }
                                }
                            }
                            is FeedState.Error -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(currentState.message, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            else -> {}
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
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