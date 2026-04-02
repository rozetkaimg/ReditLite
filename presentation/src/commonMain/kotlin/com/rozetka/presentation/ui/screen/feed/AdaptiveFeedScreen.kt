package com.rozetka.presentation.ui.screen.feed

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rozetka.domain.model.Post
import com.rozetka.presentation.ui.screen.postDetail.PostDetailScreen
import com.rozetka.presentation.ui.screen.postDetail.PostDetailViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AdaptiveFeedScreen(
    windowSizeClass: WindowSizeClass,
    feedViewModel: FeedViewModel,
    onPostClick: (Post) -> Unit,
    onSubredditClick: (String) -> Unit,
    onToggleBottomBar: (Boolean) -> Unit
) {
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    var selectedPostId by remember { mutableStateOf<String?>(null) }

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
                    onToggleBottomBar = onToggleBottomBar
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
                        onSubredditClick = onSubredditClick
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                        Text("Select a post to view details", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    } else {
        FeedScreen(
            viewModel = feedViewModel,
            onPostClick = onPostClick,
            onSubredditClick = onSubredditClick,
            onToggleBottomBar = onToggleBottomBar
        )
    }
}
