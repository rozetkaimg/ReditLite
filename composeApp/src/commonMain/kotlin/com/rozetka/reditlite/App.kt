package com.rozetka.reditlite

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.rozetka.reditlite.data.RedditRepository
import com.rozetka.reditlite.data.TokenStorage
import com.rozetka.reditlite.models.RedditPost
import com.rozetka.reditlite.screens.FeedScreen
import com.rozetka.reditlite.screens.PostDetailScreen

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(authCode: String?, onLoginClick: () -> Unit) {
    val repository = remember { RedditRepository() }
    val navController = rememberNavController() // Главный контроллер навигации

    var authState by remember {
        mutableStateOf<AuthState>(
            if (TokenStorage.accessToken != null) AuthState.Authenticated else AuthState.Idle
        )
    }

    // Храним выбранный пост на уровне App, чтобы передавать его в экран деталей
    var selectedPost by remember { mutableStateOf<RedditPost?>(null) }

    // Логика авторизации
    LaunchedEffect(authCode) {
        if (authCode != null && authState !is AuthState.Authenticated) {
            authState = AuthState.Loading
            repository.getAccessToken(authCode).fold(
                onSuccess = { token ->
                    TokenStorage.accessToken = token
                    authState = AuthState.Authenticated
                    // После успешного входа направляем на feed и чистим login из стека жеста "назад"
                    navController.navigate("feed") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onFailure = { error ->
                    authState = AuthState.Error(error.message ?: "Login failed")
                }
            )
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
            val startDestination = if (TokenStorage.accessToken != null) "feed" else "login"

            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) },
                popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) },
                popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) }
            ) {

                composable("login") {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        when (authState) {
                            is AuthState.Loading -> {
                                CircularProgressIndicator()
                            }
                            else -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    FilledTonalButton(
                                        onClick = onLoginClick,
                                        shape = RoundedCornerShape(28.dp),
                                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 20.dp)
                                    ) {
                                        Text("Log in with Reddit", style = MaterialTheme.typography.titleLarge)
                                    }
                                    if (authState is AuthState.Error) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = (authState as AuthState.Error).message,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                composable("feed") {
                    FeedScreen(onPostClick = { post ->
                        selectedPost = post
                        navController.navigate("detail")
                    })
                }

                composable("detail") {
                    selectedPost?.let { post ->
                        PostDetailScreen(
                            post = post,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}