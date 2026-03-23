package com.rozetka.reditlite

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rozetka.data.local.SecureStorageManager
import com.rozetka.presentation.ui.screen.feed.FeedScreen
import com.rozetka.presentation.ui.screen.feed.FeedViewModel
import com.rozetka.presentation.ui.screen.login.LoginScreen
import com.rozetka.presentation.ui.screen.postDetail.PostDetailViewModel
import com.rozetka.presentation.ui.screen.profile.ProfileViewModel
import com.rozetka.presentation.ui.screen.login.AuthViewModel
import com.rozetka.reditlite.presentation.screens.*
import org.koin.compose.KoinContext
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    authCode: String?,
    onLoginClick: () -> Unit
) {
    KoinContext {
        val storageManager: SecureStorageManager = koinInject()

        MaterialTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                val navController = rememberNavController()

                val startDestination = remember {
                    when {
                        !storageManager.accessToken.isNullOrBlank() -> "feed"
                        storageManager.isFirstLaunch -> "onboarding"
                        else -> "login"
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination
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
                                navController.navigate("feed") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("feed") {
                        val viewModel: FeedViewModel = koinViewModel()
                        FeedScreen(
                            viewModel = viewModel,
                            onPostClick = { post ->
                                navController.navigate("detail/${post.id}")
                            }
                        )
                    }

                    composable("detail/{postId}") { backStackEntry ->
                        val postId = backStackEntry.arguments?.getString("postId") ?: ""
                        val viewModel: PostDetailViewModel = koinViewModel()
                        PostDetailScreen(
                            postId = postId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("profile") {
                        val viewModel: ProfileViewModel = koinViewModel()
                        ProfileScreen(
                            viewModel = viewModel,
                            onLogout = {
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