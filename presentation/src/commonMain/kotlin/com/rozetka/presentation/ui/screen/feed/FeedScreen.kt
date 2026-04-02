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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rozetka.domain.model.FeedType
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.VoteDirection
import com.rozetka.presentation.mvi.FeedIntent
import com.rozetka.presentation.mvi.FeedState
import com.rozetka.presentation.ui.screen.feed.components.PostCard
import com.rozetka.presentation.ui.components.ImageViewer
import org.jetbrains.compose.resources.stringResource
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.*
import kotlin.math.roundToInt

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

    val density = LocalDensity.current

    val filterBarHeight = 56.dp
    val filterBarHeightPx = with(density) { filterBarHeight.toPx() }

    val filterOffsetHeightPx = remember { mutableStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = filterOffsetHeightPx.value + delta
                filterOffsetHeightPx.value = newOffset.coerceIn(-filterBarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

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
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }
        ) { targetUrl ->
            if (targetUrl == null) {
                LaunchedEffect(Unit) { onToggleBottomBar(true) }
                Scaffold(
                    modifier = Modifier.nestedScroll(nestedScrollConnection),
                    topBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            Column {
                                // 1. Поиск: задаем zIndex, чтобы перекрывать чипсы
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .zIndex(1f),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                                        placeholder = { Text(stringResource(Res.string.search_reddit)) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                        singleLine = true,
                                        shape = RoundedCornerShape(32.dp),
                                        colors = TextFieldDefaults.colors(
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent,
                                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                        )
                                    )
                                }

                                // Вычисляем динамическую высоту контейнера (от 56dp до 0dp)
                                val dynamicHeight = with(density) {
                                    (filterBarHeightPx + filterOffsetHeightPx.value).toDp().coerceAtLeast(0.dp)
                                }

                                // 2. Чипсы: уходят под поиск, а сам Box физически сжимается
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(dynamicHeight)
                                        .zIndex(0f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(filterBarHeight) // Фиксируем высоту Row, чтобы чипсы не сплющивались
                                            .offset { IntOffset(x = 0, y = filterOffsetHeightPx.value.roundToInt()) }
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
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
                                                label = {
                                                    val labelRes = when (feedType) {
                                                        FeedType.HOT -> Res.string.hot
                                                        FeedType.NEW -> Res.string.new_posts
                                                        FeedType.TOP -> Res.string.top
                                                        FeedType.RISING -> Res.string.rising
                                                        FeedType.BEST -> Res.string.best
                                                        FeedType.SAVED -> Res.string.saved
                                                        is FeedType.Subreddit -> Res.string.hot
                                                    }
                                                    Text(stringResource(labelRes))
                                                },
                                                shape = RoundedCornerShape(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface,
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { onPostClick(Post(id = "new", title = "NEW_POST_TRIGGER", author = "", subreddit = "", score = 0, commentsCount = 0, isSaved = false, voteStatus = VoteDirection.NONE, text = null, mediaUrl = null, postUrl = "", createdUtc = 0L)) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create Post")
                        }
                    }
                ) { paddingValues ->
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            isRefreshing = true
                            viewModel.processIntent(FeedIntent.Refresh(selectedFeedType))
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            // Теперь используем стандартный паддинг от Scaffold, так как высота TopBar меняется физически
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
                                            onSaveClick = { viewModel.processIntent(FeedIntent.ToggleSave(post)) },
                                            onSubredditClick = onSubredditClick,
                                            sharedTransitionScope = this@SharedTransitionLayout,
                                            animatedVisibilityScope = this@AnimatedContent
                                        )
                                    }
                                    if (currentState.isPaginating) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
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