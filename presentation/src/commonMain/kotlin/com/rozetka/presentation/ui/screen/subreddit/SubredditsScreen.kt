package com.rozetka.presentation.ui.screen.subreddit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rozetka.domain.model.Subreddit
import com.rozetka.presentation.mvi.SubredditsEffect
import com.rozetka.presentation.mvi.SubredditsIntent
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubredditsScreen(
    viewModel: SubredditsViewModel,
    onNavigateToSubreddit: (String) -> Unit,
    onToggleBottomBar: (Boolean) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchActive) {
        onToggleBottomBar(!isSearchActive)
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SubredditsEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SubredditsEffect.NavigateToSubreddit -> {
                    onNavigateToSubreddit(effect.name)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                Surface(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    TextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.handleIntent(SubredditsIntent.SearchSubreddits(it)) },
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        placeholder = { Text("Search subreddits...") },
                        leadingIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.handleIntent(SubredditsIntent.SearchSubreddits(""))
                            }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null)
                            }
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.handleIntent(SubredditsIntent.SearchSubreddits("")) }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(32.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                }
            } else {
                TopAppBar(
                    title = { Text("Subscriptions") },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            val displayList = if (isSearchActive) state.searchResults else state.subreddits
            
            if (state.isLoading && displayList.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (displayList.isEmpty() && !state.isLoading) {
                Text(
                    text = if (isSearchActive) "No results found" else "No subscriptions found",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayList, key = { it.id }) { subreddit ->
                        SubredditItem(
                            subreddit = subreddit,
                            onClick = { viewModel.handleIntent(SubredditsIntent.NavigateToSubreddit(subreddit.name)) },
                            onToggleSubscription = { viewModel.handleIntent(SubredditsIntent.ToggleSubscription(subreddit)) },
                            onToggleFavorite = { viewModel.handleIntent(SubredditsIntent.ToggleFavorite(subreddit)) }
                        )
                    }
                }
            }

            if (state.isLoading && displayList.isNotEmpty()) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}

@Composable
fun SubredditItem(
    subreddit: Subreddit,
    onClick: () -> Unit,
    onToggleSubscription: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (subreddit.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = null,
                    tint = if (subreddit.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            val iconUrl = subreddit.iconUrl?.takeIf { it.isNotBlank() }

            if (iconUrl != null) {
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subreddit.displayName.firstOrNull()?.toString()?.uppercase() ?: "R",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "r/${subreddit.displayName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${formatSubscribers(subreddit.subscribersCount)} subscribers",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onToggleSubscription,
                colors = if (subreddit.isSubscribed) {
                    ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (subreddit.isSubscribed) "Joined" else "Join",
                    style = MaterialTheme.typography.labelLarge
                )
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