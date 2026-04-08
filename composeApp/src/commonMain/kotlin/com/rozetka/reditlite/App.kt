package com.rozetka.reditlite

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.rozetka.data.local.SecureStorageManager
import com.rozetka.domain.repository.RateLimitRepository
import com.rozetka.presentation.ui.screen.favorites.FavoritesScreen
import com.rozetka.presentation.ui.screen.favorites.FavoritesViewModel
import com.rozetka.presentation.ui.screen.feed.AdaptiveFeedScreen
import com.rozetka.presentation.ui.screen.feed.FeedScreen
import com.rozetka.presentation.ui.screen.feed.FeedViewModel
import com.rozetka.presentation.ui.screen.login.AuthViewModel
import com.rozetka.presentation.ui.screen.login.LoginScreen
import com.rozetka.presentation.ui.screen.onboarding.OnboardingScreen
import com.rozetka.presentation.ui.screen.postCreation.PostCreationScreen
import com.rozetka.presentation.ui.screen.postCreation.PostCreationViewModel
import com.rozetka.presentation.ui.screen.postDetail.PostDetailScreen
import com.rozetka.presentation.ui.screen.postDetail.PostDetailViewModel
import com.rozetka.presentation.ui.screen.profile.ProfileScreen
import com.rozetka.presentation.ui.screen.profile.ProfileViewModel
import com.rozetka.presentation.ui.screen.subreddit.SubredditDetailScreen
import com.rozetka.presentation.ui.screen.subreddit.SubredditDetailViewModel
import com.rozetka.presentation.ui.screen.subreddit.SubredditsScreen
import com.rozetka.presentation.ui.screen.subreddit.SubredditsViewModel
import com.rozetka.reditlite.generated.resources.Res
import com.rozetka.reditlite.generated.resources.*
import com.rozetka.reditlite.ui.components.SwipeBackWrapper
import io.ktor.client.HttpClient
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

sealed class BottomNavItem(val route: String, val label: StringResource, val icon: ImageVector) {
    object Feed : BottomNavItem("feed", Res.string.feed, Icons.Default.Home)
    object Subscriptions : BottomNavItem("subscriptions", Res.string.subscriptions, Icons.Default.List)
    object Favorites : BottomNavItem("favorites", Res.string.favorites, Icons.Default.Favorite)
    object Profile : BottomNavItem("profile", Res.string.profile, Icons.Default.Person)
}

@Composable
fun App(
    windowSizeClass: WindowSizeClass,
    authCode: String?,
    deepLink: String? = null,
    onDeepLinkHandled: () -> Unit = {},
    onLoginClick: () -> Unit
) {
    KoinContext {
        val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        val client: HttpClient = koinInject()
        val rateLimitRepository: RateLimitRepository = koinInject()
        val isRateLimited by rateLimitRepository.isRateLimited.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        val storageManager: SecureStorageManager = koinInject()
        val navController = rememberNavController()

        LaunchedEffect(deepLink) {
            deepLink?.let { link ->
                val uri = link.removePrefix("https://").removePrefix("www.").removePrefix("reddit.com").removePrefix("/")
                when {
                    uri.startsWith("r/") -> {
                        val parts = uri.split("/")
                        if (parts.size >= 2) {
                            val subredditName = parts[1]
                            navController.navigate("subreddit/$subredditName")
                        }
                    }
                    uri.startsWith("comments/") -> {
                        val parts = uri.split("/")
                        if (parts.size >= 2) {
                            val postId = parts[1]
                            navController.navigate("detail/$postId")
                        }
                    }
                }
                onDeepLinkHandled()
            }
        }

        LaunchedEffect(isRateLimited) {
            if (isRateLimited) {
                snackbarHostState.showSnackbar(
                    message = "Rate limit reached. Please wait a moment.",
                    duration = SnackbarDuration.Indefinite
                )
            } else {
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }

        setSingletonImageLoaderFactory { context ->
            ImageLoader.Builder(context)
                .components {
                    add(KtorNetworkFetcherFactory(client))
                    addPlatformComponents()
                }
                .crossfade(true)
                .logger(DebugLogger())
                .build()
        }

        val bottomNavItems = listOf(
            BottomNavItem.Feed,
            BottomNavItem.Subscriptions,
            BottomNavItem.Favorites,
            BottomNavItem.Profile
        )

        ReditLiteTheme {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val excludeBottomBarRoutes = listOf("login", "onboarding")

            var lastSelectedTab by remember { mutableStateOf(BottomNavItem.Feed.route) }
            val currentSelectedItem = bottomNavItems.find { item ->
                currentDestination?.hierarchy?.any { it.route == item.route } == true
            }

            LaunchedEffect(currentSelectedItem) {
                currentSelectedItem?.let {
                    lastSelectedTab = it.route
                }
            }

            var isBottomBarVisible by remember { mutableStateOf(true) }
            val showNavComponents = currentDestination?.route !in excludeBottomBarRoutes && isBottomBarVisible

            Row(modifier = Modifier.fillMaxSize()) {
                if (isExpanded && showNavComponents) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ) {
                        bottomNavItems.forEach { item ->
                            NavigationRailItem(
                                icon = { Icon(item.icon, contentDescription = stringResource(item.label)) },
                                label = { Text(stringResource(item.label)) },
                                selected = item.route == lastSelectedTab,
                                onClick = {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (!isExpanded && showNavComponents) {
                            AppBottomNavigationBarNew(
                                bottomNavItems = bottomNavItems,
                                lastSelectedTab = lastSelectedTab,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                ) { innerPadding ->
                    val startDestination = remember {
                        when {
                            !storageManager.accessToken.isNullOrBlank() -> BottomNavItem.Feed.route
                            storageManager.isFirstLaunch -> "onboarding"
                            else -> "login"
                        }
                    }

                    val canGoBack = navController.previousBackStackEntry != null
                    val isSwipeEnabled = canGoBack && currentDestination?.route !in excludeBottomBarRoutes

                    SwipeBackWrapper(
                        modifier = Modifier.fillMaxSize(),
                        onBack = { navController.popBackStack() },
                        enabled = isSwipeEnabled
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    top = innerPadding.calculateTopPadding(),
                                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current)
                                )
                        ) {
                            composable("onboarding") {
                                OnboardingScreen(
                                    onComplete = {
                                        storageManager.isFirstLaunch = false
                                        navController.navigate("login") {
                                            popUpTo("onboarding") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable("login") {
                                val viewModel: AuthViewModel = koinViewModel()
                                LoginScreen(
                                    viewModel = viewModel,
                                    authCode = authCode,
                                    onLoginClick = onLoginClick,
                                    onAuthenticated = {
                                        navController.navigate(BottomNavItem.Feed.route) {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            }

                            composable(BottomNavItem.Feed.route) {
                                val viewModel: FeedViewModel = koinViewModel()
                                AdaptiveFeedScreen(
                                    windowSizeClass = windowSizeClass,
                                    feedViewModel = viewModel,
                                    onPostClick = { post ->
                                        if (post.id == "new") {
                                            navController.navigate("create_post")
                                        } else {
                                            navController.navigate("detail/${post.id}")
                                        }
                                    },
                                    onSubredditClick = { subredditName ->
                                        navController.navigate("subreddit/$subredditName")
                                    },
                                    onUserClick = { username ->
                                        navController.navigate("profile/$username")
                                    },
                                    onToggleBottomBar = { isBottomBarVisible = it },
                                    onCreatePost = { navController.navigate("create_post") }
                                )
                            }

                            composable(BottomNavItem.Subscriptions.route) {
                                val viewModel: SubredditsViewModel = koinViewModel()
                                SubredditsScreen(
                                    viewModel = viewModel,
                                    onNavigateToSubreddit = { subredditName ->
                                        navController.navigate("subreddit/$subredditName")
                                    }
                                )
                            }

                            composable("subreddit/{subredditName}") { backStackEntry ->
                                val subredditName = backStackEntry.arguments?.getString("subredditName") ?: ""
                                val viewModel: SubredditDetailViewModel = koinViewModel()
                                SubredditDetailScreen(
                                    subredditName = subredditName,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onPostClick = { postId ->
                                        navController.navigate("detail/$postId")
                                    },
                                    onUserClick = { username ->
                                        navController.navigate("profile/$username")
                                    },
                                    onToggleBottomBar = { isBottomBarVisible = it }
                                )
                            }

                            composable(BottomNavItem.Favorites.route) {
                                val viewModel: FavoritesViewModel = koinViewModel()
                                FavoritesScreen(
                                    viewModel = viewModel,
                                    onPostClick = { postId ->
                                        navController.navigate("detail/$postId")
                                    },
                                    onSubredditClick = { subredditName ->
                                        navController.navigate("subreddit/$subredditName")
                                    },
                                    onUserClick = { username ->
                                        navController.navigate("profile/$username")
                                    }
                                )
                            }

                            composable("detail/{postId}") { backStackEntry ->
                                val postId = backStackEntry.arguments?.getString("postId") ?: ""
                                val viewModel: PostDetailViewModel = koinViewModel()
                                PostDetailScreen(
                                    postId = postId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onSubredditClick = { subredditName ->
                                        navController.navigate("subreddit/$subredditName")
                                    },
                                    onUserClick = { username ->
                                        navController.navigate("profile/$username")
                                    },
                                    onToggleBottomBar = { isBottomBarVisible = it }
                                )
                            }

                            composable("create_post") {
                                val viewModel: PostCreationViewModel = koinViewModel()
                                PostCreationScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(BottomNavItem.Profile.route) {
                                val viewModel: ProfileViewModel = koinViewModel()
                                ProfileScreen(
                                    username = null,
                                    viewModel = viewModel,
                                    onLogout = {
                                        storageManager.accessToken = null
                                        navController.navigate("login") {
                                            popUpTo(0)
                                        }
                                    },
                                    onBack = { navController.popBackStack() },
                                    onPostClick = { postId ->
                                        navController.navigate("detail/$postId")
                                    },
                                    onUserClick = { username ->
                                        navController.navigate("profile/$username")
                                    },
                                    onToggleBottomBar = { isBottomBarVisible = it }
                                )
                            }

                            composable("profile/{username}") { backStackEntry ->
                                val username = backStackEntry.arguments?.getString("username")
                                val viewModel: ProfileViewModel = koinViewModel()
                                ProfileScreen(
                                    username = username,
                                    viewModel = viewModel,
                                    onLogout = { },
                                    onBack = { navController.popBackStack() },
                                    onPostClick = { postId ->
                                        navController.navigate("detail/$postId")
                                    },
                                    onUserClick = { clickedUsername ->
                                        navController.navigate("profile/$clickedUsername")
                                    },
                                    onToggleBottomBar = { isBottomBarVisible = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigationBarNew(
    bottomNavItems: List<BottomNavItem>,
    lastSelectedTab: String,
    onNavigate: (String) -> Unit
) {
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier.align(Alignment.BottomCenter),
            verticalAlignment = Alignment.Bottom
        ) {
            Card(
                modifier = Modifier.padding(
                    end = 8.dp,
                    bottom = navBarHeight + 20.dp
                ),
                shape = RoundedCornerShape(30.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 10.dp
                )
            ) {
                LazyRow(Modifier.padding(8.dp)) {
                    items(3) { index ->
                        val item = bottomNavItems[index]
                        val isSelected = lastSelectedTab == item.route

                        Surface(
                            onClick = { onNavigate(item.route) },
                            shape = RoundedCornerShape(24.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = fadeIn() + expandHorizontally(),
                                    exit = fadeOut() + shrinkHorizontally()
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = stringResource(item.label),
                                        modifier = Modifier.padding(end = 6.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(modifier = Modifier.height(32.dp)) {
                                    Text(
                                        stringResource(item.label),
                                        modifier = Modifier.align(Alignment.Center),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Box {
                val item = bottomNavItems[3]
                val isSelected = lastSelectedTab == item.route
                Card(
                    modifier = Modifier
                        .padding(bottom = navBarHeight + 20.dp)
                        .size(58.dp),
                    shape = RoundedCornerShape(100),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 10.dp
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(onClick = { onNavigate(item.route) }),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = stringResource(item.label),
                            tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}