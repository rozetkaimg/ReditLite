package com.rozetka.presentation.ui.screen.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rozetka.presentation.ui.screen.profile.components.*
import com.rozetka.presentation.ui.screen.feed.components.PostCard
import com.rozetka.presentation.ui.components.ImageViewer
import org.jetbrains.compose.resources.stringResource
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.*
import org.monogram.presentation.core.ui.CollapsingToolbarScaffold
import org.monogram.presentation.core.ui.rememberCollapsingToolbarScaffoldState
import org.monogram.presentation.core.util.ScrollStrategy

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    username: String? = null,
    viewModel: ProfileViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit = {},
    onUserClick: (String) -> Unit = {},
    onToggleBottomBar: (Boolean) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var fullscreenMediaUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(username) {
        viewModel.loadProfileData(username)
    }

    val states = rememberCollapsingToolbarScaffoldState()
    val collapsedColor = MaterialTheme.colorScheme.surface
    val expandedColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val dynamicContainerColor = lerp(
        start = collapsedColor,
        stop = expandedColor,
        fraction = states.toolbarState.progress
    )

    SharedTransitionLayout {
        AnimatedContent(
            targetState = fullscreenMediaUrl,
            label = "fullscreen_transition",
            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }
        ) { targetUrl ->
            if (targetUrl == null) {
                LaunchedEffect(Unit) { onToggleBottomBar(true) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (username == null) stringResource(Res.string.profile_title) else "@$username",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.alpha(1f - states.toolbarState.progress)
                                )
                            },
                            navigationIcon = {
                                if (username != null) {
                                    IconButton(onClick = onBack) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = null
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (username == null) {
                                    IconButton(onClick = { viewModel.logout { onLogout() } }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                            contentDescription = stringResource(Res.string.logout),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = dynamicContainerColor,
                                scrolledContainerColor = dynamicContainerColor
                            )
                        )
                    },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = paddingValues.calculateTopPadding())
                            .background(dynamicContainerColor)
                    ) {
                        when {
                            uiState.isLoading && uiState.profile == null -> {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            }
                            uiState.errorMessage != null -> {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(Res.string.error_loading, uiState.errorMessage ?: ""),
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(onClick = { viewModel.loadProfileData() }) {
                                        Text(stringResource(Res.string.retry))
                                    }
                                }
                            }
                            uiState.profile != null -> {
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
                                            ProfileHeaderSection(
                                                profile = uiState.profile!!,
                                                progress = states.toolbarState.progress
                                            )
                                        }
                                    }
                                ) {
                                    val currentRadius = 32.dp * states.toolbarState.progress
                                    Card(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = RoundedCornerShape(topStart = currentRadius, topEnd = currentRadius),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        ProfileContent(
                                            uiState = uiState,
                                            onPostClick = onPostClick,
                                            onUserClick = onUserClick,
                                            onMediaClick = { url ->
                                                fullscreenMediaUrl = url
                                                onToggleBottomBar(false)
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileContent(
    uiState: ProfileUiState,
    onPostClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onMediaClick: (String) -> Unit,
    sharedTransitionScope: androidx.compose.animation.SharedTransitionScope,
    animatedVisibilityScope: androidx.compose.animation.AnimatedVisibilityScope
) {
    val profile = uiState.profile!!
    val lazyListState = rememberLazyListState()
    
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            KarmaStatsSection(profile)
        }

        if (uiState.trophies.isNotEmpty()) {
            item {
                TrophiesSection(uiState.trophies)
            }
        }

        if (uiState.userPosts.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.my_posts_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(uiState.userPosts) { post ->
               PostCard(
                    post = post,
                    onPostClick = { onPostClick(post.id) },
                    onMediaClick = onMediaClick,
                    onVote = { },
                    onSaveClick = { },
                    onUserClick = onUserClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }

        if (uiState.savedPosts.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.saved_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(uiState.savedPosts) { post ->
                PostCard(
                    post = post,
                    onPostClick = { onPostClick(post.id) },
                    onMediaClick = onMediaClick,
                    onVote = { },
                    onSaveClick = { },
                    onUserClick = onUserClick,
                    sharedTransitionScope = sharedTransitionScope,
                    animatedVisibilityScope = animatedVisibilityScope
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
