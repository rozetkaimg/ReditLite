package com.rozetka.presentation.ui.screen.feed

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rozetka.domain.model.Post
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.select_post_to_view_details
import com.rozetka.presentation.mvi.FeedState
import com.rozetka.presentation.ui.screen.postDetail.PostDetailScreen
import com.rozetka.presentation.ui.screen.postDetail.PostDetailViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdaptiveFeedScreen(
    windowSizeClass: WindowSizeClass,
    feedViewModel: FeedViewModel,
    onPostClick: (Post) -> Unit,
    onSubredditClick: (String) -> Unit,
    onUserClick: (String) -> Unit = {},
    onToggleBottomBar: (Boolean) -> Unit,
    onCreatePost: () -> Unit
) {
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    var selectedPostId by remember { mutableStateOf<String?>(null) }
    val state by feedViewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is FeedState.Loading) {
            selectedPostId = null
        }
    }

    if (isExpanded) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                FeedScreen(
                    viewModel = feedViewModel,
                    onPostClick = { post ->
                        if (post.id == "new") {
                            onPostClick(post)
                        } else {
                            selectedPostId = post.id
                        }
                    },
                    onSubredditClick = onSubredditClick,
                    onUserClick = onUserClick,
                    onToggleBottomBar = onToggleBottomBar,
                    onCreatePost = onCreatePost
                )
            }
            VerticalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Box(modifier = Modifier.weight(1.5f)) {
                if (selectedPostId != null) {
                    val detailViewModel: PostDetailViewModel = koinViewModel(key = selectedPostId)
                    PostDetailScreen(
                        postId = selectedPostId!!,
                        viewModel = detailViewModel,
                        onBack = { selectedPostId = null },
                        onSubredditClick = onSubredditClick,
                        onUserClick = onUserClick
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            stringResource(Res.string.select_post_to_view_details),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    } else {
        FeedScreen(
            viewModel = feedViewModel,
            onPostClick = onPostClick,
            onSubredditClick = onSubredditClick,
            onUserClick = onUserClick,
            onToggleBottomBar = onToggleBottomBar,
            onCreatePost = onCreatePost
        )
    }
}
