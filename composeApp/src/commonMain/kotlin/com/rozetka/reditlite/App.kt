package com.rozetka.reditlite

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory

import com.rozetka.reditlite.data.NetworkClient
import com.rozetka.reditlite.data.RedditRepository
import com.rozetka.reditlite.data.SecureStorageManager
import com.rozetka.reditlite.data.local.AppDatabase
import com.rozetka.reditlite.models.RedditPost
import com.rozetka.reditlite.screens.FeedScreen
import com.rozetka.reditlite.screens.OnboardingScreen
import com.rozetka.reditlite.screens.PostDetailScreen
import com.rozetka.reditlite.screens.ProfileScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    authCode: String?,
    onLoginClick: () -> Unit,
    storageManager: SecureStorageManager,
    database: AppDatabase
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }

    LaunchedEffect(Unit) {
        NetworkClient.storageManager = storageManager
    }

    val repository = remember { RedditRepository(database, storageManager) }
    val navController = rememberNavController()

    var selectedPost by remember { mutableStateOf<RedditPost?>(null) }

    val darkTheme = isSystemInDarkTheme()

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

    val colorScheme = if (darkTheme) darkColors else lightColors

    MaterialTheme(colorScheme = colorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            val startDestination = remember {
                when {
                    storageManager.accessToken != null -> "feed"
                    storageManager.isFirstLaunch -> "onboarding"
                    else -> "login"
                }
            }

            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
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
                    val authViewModel: AuthViewModel = viewModel { AuthViewModel(repository, storageManager) }
                    val authState by authViewModel.authState.collectAsState()

                    LaunchedEffect(authCode) {
                        if (authCode != null && authState !is com.rozetka.reditlite.data.AuthState.Authenticated) {
                            authViewModel.authenticate(authCode)
                        }
                    }

                    LaunchedEffect(authState) {
                        if (authState is com.rozetka.reditlite.data.AuthState.Authenticated) {
                            navController.navigate("feed") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    }

                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        when (authState) {
                            is com.rozetka.reditlite.data.AuthState.Loading -> {
                                CircularProgressIndicator()
                            }
                            else -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = "Logo",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(100.dp)
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))

                                    Text(
                                        text = "Вход в RedditLite",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(48.dp))

                                    FilledTonalButton(
                                        onClick = { onLoginClick() },
                                        shape = RoundedCornerShape(28.dp),
                                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 20.dp)
                                    ) {
                                        Text("Войти через Reddit", style = MaterialTheme.typography.titleLarge)
                                    }

                                    if (authState is com.rozetka.reditlite.data.AuthState.Error) {
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = (authState as com.rozetka.reditlite.data.AuthState.Error).message,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                composable("feed") {
                    val feedViewModel: FeedViewModel = viewModel { FeedViewModel(repository) }

                    Box(modifier = Modifier.fillMaxSize()) {
                        FeedScreen(
                            viewModel = feedViewModel,
                            onPostClick = { post ->
                                selectedPost = post
                                navController.navigate("detail")
                            }
                        )

                        FloatingActionButton(
                            onClick = { navController.navigate("profile") },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(24.dp)
                                .navigationBarsPadding(),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Profile")
                        }
                    }
                }

                composable("profile") {
                    val profileViewModel: ProfileViewModel = viewModel { ProfileViewModel(storageManager, database) }
                    ProfileScreen(
                        viewModel = profileViewModel,
                        navigateToLogin = {
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }
                    )
                }

                composable("detail") {
                    selectedPost?.let { post ->
                        val detailViewModel: PostDetailViewModel = viewModel { PostDetailViewModel(repository) }

                        PostDetailScreen(
                            post = post,
                            viewModel = detailViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}