package com.rozetka.presentation.ui.screen.subreddit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import com.rozetka.presentation.ui.components.ShareText
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rozetka.domain.model.Subreddit
import com.rozetka.presentation.mvi.SubredditDetailIntent
import com.rozetka.presentation.ui.components.ImageViewer
import com.rozetka.presentation.ui.screen.feed.components.PostCard
import org.monogram.presentation.core.ui.CollapsingToolbarScaffold
import org.monogram.presentation.core.ui.rememberCollapsingToolbarScaffoldState
import org.monogram.presentation.core.util.ScrollStrategy

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun SubredditDetailScreen(
    subredditName: String,
    viewModel: SubredditDetailViewModel,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onSubredditClick: (String) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onToggleBottomBar: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var fullscreenMediaUrl by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(subredditName) {
        viewModel.handleIntent(SubredditDetailIntent.LoadSubreddit(subredditName))
    }

    val states = rememberCollapsingToolbarScaffoldState()
    val lazyListState = rememberLazyListState()
    val collapsedColor = MaterialTheme.colorScheme.surface
    val expandedColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val dynamicContainerColor = lerp(
        start = collapsedColor,
        stop = expandedColor,
        fraction = states.toolbarState.progress
    )

    var shareTextData by remember { mutableStateOf<String?>(null) }

    shareTextData?.let { text ->
        ShareText(text = text, title = "Share Subreddit")
        shareTextData = null
    }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = fullscreenMediaUrl,
            label = "fullscreen_transition",
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }
        ) { targetUrl ->
            if (targetUrl == null) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "r/$subredditName",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.alpha(1f - states.toolbarState.progress)
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { shareTextData = "https://reddit.com/r/$subredditName" }
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Share")
                                }
                                IconButton(
                                    onClick = { showAbout = !showAbout },
                                    modifier = Modifier.background(
                                        color = if (showAbout) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                                        shape = CircleShape
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (showAbout) MaterialTheme.colorScheme.onSecondaryContainer else LocalContentColor.current
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = dynamicContainerColor,
                                scrolledContainerColor = dynamicContainerColor
                            )
                        )
                    }
                ) { padding ->
                    val subreddit = state.subreddit

                    if (subreddit != null) {
                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = { viewModel.handleIntent(SubredditDetailIntent.Refresh) },
                            modifier = Modifier
                                .fillMaxSize()
                                .background(dynamicContainerColor)
                                .padding(top = padding.calculateTopPadding())
                        ) {
                            CollapsingToolbarScaffold(
                                modifier = Modifier.fillMaxSize(),
                                state = states,
                                scrollStrategy = ScrollStrategy.ExitUntilCollapsed,
                                toolbar = {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(0.dp)
                                            .pin()
                                            .background(dynamicContainerColor)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .road(Alignment.Center, Alignment.BottomCenter)
                                    ) {
                                        SubredditHeaderContent(
                                            subreddit = subreddit,
                                            progress = states.toolbarState.progress,
                                            onToggleSubscription = { viewModel.handleIntent(SubredditDetailIntent.ToggleSubscription) }
                                        )
                                    }
                                }
                            ) {
                                val currentRadius = 32.dp * states.toolbarState.progress

                                Box(modifier = Modifier.fillMaxSize()) {
                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(topStart = currentRadius, topEnd = currentRadius),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {}

                                        val shouldLoadMore = remember {
                                            derivedStateOf {
                                                val lastVisibleItemIndex = lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                                                val totalItemsCount = lazyListState.layoutInfo.totalItemsCount
                                                lastVisibleItemIndex >= totalItemsCount - 5 && totalItemsCount > 0 && !state.isPaginating && state.after != null
                                            }
                                        }

                                        LaunchedEffect(shouldLoadMore.value) {
                                            if (shouldLoadMore.value) {
                                                viewModel.handleIntent(SubredditDetailIntent.LoadMore)
                                            }
                                        }

                                        LazyColumn(
                                            state = lazyListState,
                                            modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(
                                            top = 16.dp,
                                            start = 16.dp,
                                            end = 16.dp,
                                            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
                                        ),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        item {
                                            AnimatedVisibility(
                                                visible = showAbout,
                                                enter = fadeIn() + expandVertically(),
                                                exit = fadeOut() + shrinkVertically()
                                            ) {
                                                AboutSection(subreddit = subreddit)
                                            }
                                        }

                                        items(state.posts, key = { it.id }) { post ->
                                            PostCard(
                                                post = post,
                                                onPostClick = { onPostClick(post.id) },
                                                onVote = { directionInt ->
                                                    val direction = if (directionInt > 0) {
                                                        if (post.voteStatus == com.rozetka.domain.model.VoteDirection.UP) com.rozetka.domain.model.VoteDirection.NONE
                                                        else com.rozetka.domain.model.VoteDirection.UP
                                                    } else {
                                                        if (post.voteStatus == com.rozetka.domain.model.VoteDirection.DOWN) com.rozetka.domain.model.VoteDirection.NONE
                                                        else com.rozetka.domain.model.VoteDirection.DOWN
                                                    }
                                                    viewModel.handleIntent(SubredditDetailIntent.VotePost(post.id, direction))
                                                },
                                                onSaveClick = {
                                                    viewModel.handleIntent(SubredditDetailIntent.SavePost(post.id))
                                                },
                                                onMediaClick = { url ->
                                                    fullscreenMediaUrl = url
                                                    onToggleBottomBar(false)
                                                },
                                                onSubredditClick = onSubredditClick,
                                                onUserClick = onUserClick,
                                                sharedTransitionScope = this@SharedTransitionLayout,
                                                animatedVisibilityScope = this@AnimatedContent
                                            )
                                        }

                                        if (state.isPaginating) {
                                            item {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator()
                                                }
                                            }
                                        }
                                    }

                                    if (state.isLoading && state.posts.isEmpty()) {
                                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator()
                            } else if (state.error != null) {
                                Text(text = state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                            }
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

@Composable
private fun SubredditHeaderContent(
    subreddit: Subreddit,
    progress: Float,
    onToggleSubscription: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .alpha(progress),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val iconUrl = subreddit.iconUrl?.takeIf { it.isNotBlank() }

        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = subreddit.displayName.firstOrNull()?.toString()?.uppercase() ?: "R",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = subreddit.displayName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "r/${subreddit.name}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatSubscribers(subreddit.subscribersCount),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Members",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onToggleSubscription,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(48.dp),
                colors = if (subreddit.isSubscribed) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                Text(
                    text = if (subreddit.isSubscribed) "Joined" else "Join",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun AboutSection(subreddit: Subreddit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "About Community",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subreddit.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (subreddit.rules.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Community Rules",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                subreddit.rules.forEachIndexed { index, rule ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "${index + 1}. $rule",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatSubscribers(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}.${(count % 1_000_000) / 100_000}M"
        count >= 1_000 -> "${count / 1_000}k"
        else -> count.toString()
    }
}