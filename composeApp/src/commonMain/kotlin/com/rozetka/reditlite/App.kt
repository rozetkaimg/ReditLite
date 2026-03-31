package com.rozetka.reditlite

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rozetka.data.local.SecureStorageManager
import com.rozetka.presentation.ui.screen.favorites.FavoritesScreen
import com.rozetka.presentation.ui.screen.favorites.FavoritesViewModel
import com.rozetka.presentation.ui.screen.feed.FeedScreen
import com.rozetka.presentation.ui.screen.feed.FeedViewModel
import com.rozetka.presentation.ui.screen.login.LoginScreen
import com.rozetka.presentation.ui.screen.postDetail.PostDetailViewModel
import com.rozetka.presentation.ui.screen.profile.ProfileViewModel
import com.rozetka.presentation.ui.screen.login.AuthViewModel
import com.rozetka.presentation.ui.screen.postDetail.PostDetailScreen
import com.rozetka.presentation.ui.screen.subreddit.SubredditDetailScreen
import com.rozetka.presentation.ui.screen.subreddit.SubredditDetailViewModel
import com.rozetka.presentation.ui.screen.subreddit.SubredditsScreen
import com.rozetka.presentation.ui.screen.subreddit.SubredditsViewModel
import com.rozetka.reditlite.presentation.screens.*
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.rozetka.presentation.ui.screen.postDetail.PostDetailScreen
import io.ktor.client.HttpClient

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Feed : BottomNavItem("feed", "Лента", Icons.Default.Home)
    object Subscriptions : BottomNavItem("subscriptions", "Подписки", Icons.Default.List)
    object Favorites : BottomNavItem("favorites", "Избранное", Icons.Default.Favorite)
    object Profile : BottomNavItem("profile", "Профиль", Icons.Default.Person)
}



@Composable
fun App(
    authCode: String?,
    onLoginClick: () -> Unit
) {
    val client: HttpClient = koinInject()
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(client))
            }
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }

    KoinContext {
        val storageManager: SecureStorageManager = koinInject()
        val navController = rememberNavController()

        val bottomNavItems = listOf(
            BottomNavItem.Feed,
            BottomNavItem.Subscriptions,
            BottomNavItem.Favorites,
            BottomNavItem.Profile
        )

        MaterialTheme {
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
            val showBottomBar = currentDestination?.route !in excludeBottomBarRoutes && isBottomBarVisible

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            bottomNavItems.forEach { item ->
                                NavigationBarItem(
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
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
                },
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                val startDestination = remember {
                    when {
                        !storageManager.accessToken.isNullOrBlank() -> BottomNavItem.Feed.route
                        storageManager.isFirstLaunch -> "onboarding"
                        else -> "login"
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding)
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
                        FeedScreen(
                            viewModel = viewModel,
                            onPostClick = { post ->
                                navController.navigate("detail/${post.id}")
                            },
                            onSubredditClick = { subredditName ->
                                navController.navigate("subreddit/$subredditName")
                            },
                            onToggleBottomBar = { isBottomBarVisible = it }
                        )
                    }

                    composable(BottomNavItem.Subscriptions.route) {
                        val viewModel: SubredditsViewModel = koinViewModel()
                        SubredditsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
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
                            }
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
                            }
                        )
                    }

                    composable(BottomNavItem.Profile.route) {
                        val viewModel: ProfileViewModel = koinViewModel()
                        ProfileScreen(
                            viewModel = viewModel,
                            onLogout = {
                                storageManager.accessToken = null
                                navController.navigate("login") {
                                    popUpTo(0)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}