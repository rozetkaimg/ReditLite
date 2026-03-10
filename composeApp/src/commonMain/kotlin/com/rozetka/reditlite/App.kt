package com.rozetka.reditlite

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rozetka.reditlite.data.RedditRepository
import com.rozetka.reditlite.data.TokenStorage
import com.rozetka.reditlite.models.RedditPost
import com.rozetka.reditlite.screens.FeedScreen
import com.rozetka.reditlite.screens.PostDetailScreen


@Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)

enum class AppScreen {
    Login, Feed, Detail
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(authCode: String?, onLoginClick: () -> Unit) {
    val repository = remember { RedditRepository() }

    var isLoggedIn by remember { mutableStateOf(TokenStorage.accessToken != null) }
    var selectedPost by remember { mutableStateOf<RedditPost?>(null) }

    LaunchedEffect(authCode) {
        if (authCode != null && !isLoggedIn) {
            repository.getAccessToken(authCode).onSuccess { token ->
                TokenStorage.accessToken = token
                isLoggedIn = true
            }
        }
    }

    val darkTheme = isSystemInDarkTheme()
    val dynamicColorScheme = getDynamicColorScheme(darkTheme)

    val lightColors = lightColorScheme(
        primary = Color(0xFFFF4500),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFFFDBCF),
        onPrimaryContainer = Color(0xFF330B00),
        surface = Color(0xFFFCFCFC),
        onSurface = Color(0xFF1F1B1A),
        surfaceVariant = Color(0xFFF0E5E2),
        onSurfaceVariant = Color(0xFF4F4441),
        error = Color(0xFFBA1A1A)
    )

    val darkColors = darkColorScheme(
        primary = Color(0xFFFFB59D),
        onPrimary = Color(0xFF5D1600),
        primaryContainer = Color(0xFF872100),
        onPrimaryContainer = Color(0xFFFFDBCF),
        surface = Color(0xFF1A110F),
        onSurface = Color(0xFFF1DFDA),
        surfaceVariant = Color(0xFF332A27),
        onSurfaceVariant = Color(0xFFD8C2BC),
        error = Color(0xFFFFB4AB)
    )

    val colorScheme = dynamicColorScheme ?: if (darkTheme) darkColors else lightColors

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            val currentScreen = when {
                !isLoggedIn -> AppScreen.Login
                selectedPost != null -> AppScreen.Detail
                else -> AppScreen.Feed
            }

            SystemBackHandler(enabled = currentScreen == AppScreen.Detail) {
                selectedPost = null
            }

            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    when {
                        targetState == AppScreen.Detail -> {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) togetherWith
                                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                        }
                        initialState == AppScreen.Detail && targetState == AppScreen.Feed -> {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) togetherWith
                                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                        }
                        else -> {
                            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                        }
                    }
                },
                label = "AppNavigation"
            ) { screen ->
                when (screen) {
                    AppScreen.Login -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            FilledTonalButton(
                                onClick = onLoginClick,
                                shape = RoundedCornerShape(28.dp),
                                contentPadding = PaddingValues(horizontal = 32.dp, vertical = 20.dp)
                            ) {
                                Text("Log in with Reddit", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                    }
                    AppScreen.Feed -> {
                        FeedScreen(onPostClick = { selectedPost = it })
                    }
                    AppScreen.Detail -> {
                        PostDetailScreen(
                            post = selectedPost!!,
                            onBack = { selectedPost = null }
                        )
                    }
                }
            }
        }
    }
}







