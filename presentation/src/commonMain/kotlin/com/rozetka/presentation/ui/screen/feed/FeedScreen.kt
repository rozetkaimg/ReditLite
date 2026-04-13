package com.rozetka.presentation.ui.screen.feed

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.rozetka.domain.model.FeedType
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.VoteDirection
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.search_reddit
import com.rozetka.presentation.mvi.FeedIntent
import com.rozetka.presentation.mvi.FeedState
import com.rozetka.presentation.ui.components.ImageViewer
import com.rozetka.presentation.ui.screen.feed.components.FeedFilterBar
import com.rozetka.presentation.ui.screen.feed.components.PostCard
import com.rozetka.presentation.ui.screen.feed.components.PostListShimmer
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel,
    onPostClick: (Post) -> Unit,
    onSubredditClick: (String) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onToggleBottomBar: (Boolean) -> Unit = {},
    onCreatePost: () -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFeedType by viewModel.currentFeedType.collectAsState()
    val feedTypes = remember { FeedType.entries }
    val pagerState = rememberPagerState(
        initialPage = feedTypes.indexOf(currentFeedType).coerceAtLeast(0),
        pageCount = { feedTypes.size }
    )
    val coroutineScope = rememberCoroutineScope()

    val tabStates = remember { mutableStateMapOf<FeedType, FeedState>() }
    val listStates = remember { feedTypes.associateWith { LazyListState() } }
    val searchListState = rememberLazyListState()

    LaunchedEffect(state, currentFeedType) {
        val cachedState = tabStates[currentFeedType]
        if (state is FeedState.Loading && cachedState is FeedState.Content) {
        } else {
            tabStates[currentFeedType] = state
        }
    }

    val density = LocalDensity.current
    val filterBarHeight = 72.dp
    val filterBarHeightPx = with(density) { filterBarHeight.toPx() }
    val filterOffsetHeightPx = rememberSaveable { mutableStateOf(0f) }

    LaunchedEffect(pagerState.currentPage) {
        val newFeedType = feedTypes[pagerState.currentPage]
        filterOffsetHeightPx.value = 0f
        if (newFeedType != currentFeedType) {
            viewModel.processIntent(FeedIntent.LoadInitial(newFeedType))
        }
    }

    LaunchedEffect(currentFeedType) {
        val targetPage = feedTypes.indexOf(currentFeedType)
        if (targetPage != -1 && targetPage != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

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
    var fullscreenMediaUrls by remember { mutableStateOf<List<String>?>(null) }
    var fullscreenStartIndex by remember { mutableStateOf(0) }

    LaunchedEffect(state) {
        if (state !is FeedState.Loading && (state as? FeedState.Content)?.isPaginating != true) {
            isRefreshing = false
        }
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = fullscreenMediaUrls,
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
            label = "fullscreen_media"
        ) { targetUrls ->
            if (targetUrls == null) {
                Scaffold(
                    modifier = Modifier.nestedScroll(nestedScrollConnection),
                    topBar = {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().statusBarsPadding()
                        ) {
                            Column {
                                Surface(
                                    modifier = Modifier.fillMaxWidth().zIndex(1f),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                                        modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 4.dp),
                                        placeholder = { Text(stringResource(Res.string.search_reddit)) },
                                        leadingIcon = { Icon(Icons.Default.Search, null) },
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
                                val dynamicHeight = with(density) {
                                    (filterBarHeightPx + filterOffsetHeightPx.value).toDp().coerceAtLeast(0.dp)
                                }
                                val chipsAlpha = (1f + (filterOffsetHeightPx.value / filterBarHeightPx)).coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(dynamicHeight).clipToBounds().zIndex(0f)
                                ) {
                                    FeedFilterBar(
                                        selectedFeedType = feedTypes[pagerState.currentPage],
                                        onFeedTypeSelected = { feedType ->
                                            val index = feedTypes.indexOf(feedType)
                                            if (index != -1) {
                                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer {
                                                translationY = filterOffsetHeightPx.value
                                                alpha = chipsAlpha
                                            }
                                            .padding(16.dp, 8.dp)
                                    )
                                }
                            }
                        }
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = onCreatePost,
                            modifier = Modifier.padding(
                                bottom = WindowInsets.navigationBars.asPaddingValues()
                                    .calculateBottomPadding() + 60.dp
                            ),
                            containerColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(16.dp)
                        ) { Icon(Icons.Default.Add, null) }
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .padding(top = paddingValues.calculateTopPadding())
                            .fillMaxSize()
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = feedTypes.size
                        ) { page ->
                            val pageFeedType = feedTypes[page]
                            val pageState = tabStates[pageFeedType] ?: FeedState.Loading
                            val isCurrentPage = pagerState.currentPage == page

                            val listState = if (searchQuery.isNotEmpty()) {
                                searchListState
                            } else {
                                listStates[pageFeedType] ?: rememberLazyListState()
                            }
                            var firstItemId by rememberSaveable(pageFeedType) { mutableStateOf<String?>(null) }

                            LaunchedEffect(pageState) {
                                if (pageState is FeedState.Content) {
                                    val currentFirstId = pageState.posts.firstOrNull()?.id
                                    if (currentFirstId != null) {
                                        if (firstItemId != null && firstItemId != currentFirstId) {
                                            listState.scrollToItem(0)
                                        }
                                        firstItemId = currentFirstId
                                    }
                                }
                            }

                            PullToRefreshBox(
                                isRefreshing = isRefreshing && isCurrentPage,
                                onRefresh = {
                                    isRefreshing = true
                                    viewModel.processIntent(FeedIntent.Refresh(pageFeedType))
                                },
                                modifier = Modifier.fillMaxSize()
                            ) {
                                when (pageState) {
                                    is FeedState.Loading -> {
                                        PostListShimmer()
                                    }
                                    is FeedState.Error -> {
                                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                                            Text(
                                                pageState.message,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    is FeedState.Content -> {
                                        LazyColumn(
                                            state = listState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(pageState.posts, key = { it.id }) { post ->
                                                PostCard(
                                                    modifier = Modifier,
                                                    post = post,
                                                    onPostClick = { onPostClick(post) },
                                                    onVote = { dir ->
                                                        viewModel.processIntent(
                                                            FeedIntent.Vote(
                                                                post,
                                                                when (dir) {
                                                                    1 -> VoteDirection.UP
                                                                    -1 -> VoteDirection.DOWN
                                                                    else -> VoteDirection.NONE
                                                                }
                                                            )
                                                        )
                                                    },
                                                    onMediaClick = { url ->
                                                        val urls = post.galleryUrls ?: listOfNotNull(url)
                                                        val index = urls.indexOf(url).coerceAtLeast(0)
                                                        fullscreenMediaUrls = urls
                                                        fullscreenStartIndex = index
                                                        onToggleBottomBar(false)
                                                    },
                                                    onSaveClick = {
                                                        viewModel.processIntent(FeedIntent.ToggleSave(post))
                                                    },
                                                    onSubredditClick = onSubredditClick,
                                                    onUserClick = onUserClick,
                                                    sharedTransitionScope = this@SharedTransitionLayout,
                                                    animatedVisibilityScope = this@AnimatedContent
                                                )
                                            }

                                            if (pageState.isPaginating) {
                                                item(key = "pagination_loader") {
                                                    Box(
                                                        Modifier.fillMaxWidth().padding(16.dp),
                                                        Alignment.Center
                                                    ) {
                                                        CircularProgressIndicator(Modifier.size(32.dp))
                                                    }
                                                }
                                            }
                                        }

                                        val shouldLoadMore by remember {
                                            derivedStateOf {
                                                val layoutInfo = listState.layoutInfo
                                                val totalItems = layoutInfo.totalItemsCount
                                                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                                totalItems > 0 && lastVisible >= totalItems - 5
                                            }
                                        }

                                        LaunchedEffect(shouldLoadMore, pageState.isPaginating, isCurrentPage) {
                                            if (shouldLoadMore && !pageState.isPaginating && isCurrentPage) {
                                                viewModel.processIntent(FeedIntent.LoadNextPage(pageFeedType))
                                            }
                                        }
                                    }

                                    else -> {}
                                }
                            }
                        }
                    }
                }
            } else {
                ImageViewer(
                    images = targetUrls,
                    startIndex = fullscreenStartIndex,
                    onDismiss = { fullscreenMediaUrls = null; onToggleBottomBar(true) },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent
                )
            }
        }
    }
}