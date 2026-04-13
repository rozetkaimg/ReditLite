package com.rozetka.presentation.ui.screen.favorites

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rozetka.presentation.mvi.FavoritesIntent
import com.rozetka.presentation.ui.components.ImageViewer
import com.rozetka.presentation.ui.screen.feed.components.PostCard
import com.rozetka.presentation.ui.screen.subreddit.SubredditItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onPostClick: (String) -> Unit,
    onSubredditClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    onToggleBottomBar: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Посты", "Сабреддиты")

    val postsListState = rememberLazyListState()
    val subredditsListState = rememberLazyListState()

    var fullscreenMediaUrls by remember { mutableStateOf<List<String>?>(null) }
    var fullscreenStartIndex by remember { mutableStateOf(0) }

    SharedTransitionLayout {
        AnimatedContent(
            targetState = fullscreenMediaUrls,
            label = "fullscreen_media"
        ) { targetUrls ->
            if (targetUrls == null) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Избранное") }
                        )
                    }
                ) { padding ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        TabRow(selectedTabIndex = selectedTab) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = { Text(title) }
                                )
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            when (selectedTab) {
                                0 -> {
                                    if (state.savedPosts.isEmpty()) {
                                        EmptyState("Нет сохраненных постов")
                                    } else {
                                        LazyColumn(
                                            state = postsListState,
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            items(state.savedPosts, key = { it.id }) { post ->
                                                PostCard(
                                                    post = post,
                                                    onPostClick = { onPostClick(post.id) },
                                                    onVote = { },
                                                    onSaveClick = {
                                                        viewModel.handleIntent(
                                                            FavoritesIntent.TogglePostSave(
                                                                post
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
                                                    onSubredditClick = onSubredditClick,
                                                    onUserClick = onUserClick,
                                                    sharedTransitionScope = this@SharedTransitionLayout,
                                                    animatedVisibilityScope = this@AnimatedContent
                                                )
                                            }
                                        }
                                    }
                                }
                                1 -> {
                                    if (state.favoriteSubreddits.isEmpty()) {
                                        EmptyState("Нет избранных сабреддитов")
                                    } else {
                                        LazyColumn(
                                            state = subredditsListState,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(state.favoriteSubreddits, key = { it.id }) { subreddit ->
                                                SubredditItem(
                                                    subreddit = subreddit,
                                                    onClick = { onSubredditClick(subreddit.name) },
                                                    onToggleSubscription = { },
                                                    onToggleFavorite = {
                                                        viewModel.handleIntent(
                                                            FavoritesIntent.ToggleSubredditFavorite(
                                                                subreddit
                                                            )
                                                        )
                                                    }
                                                )
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(horizontal = 16.dp),
                                                    thickness = 0.5.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                ImageViewer(
                    images = targetUrls,
                    startIndex = fullscreenStartIndex,
                    onDismiss = {
                        fullscreenMediaUrls = null
                        onToggleBottomBar(true)
                    },
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedVisibilityScope = this@AnimatedContent
                )
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge)
    }
}
