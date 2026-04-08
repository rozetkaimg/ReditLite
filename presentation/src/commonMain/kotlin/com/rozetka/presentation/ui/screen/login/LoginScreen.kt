package com.rozetka.presentation.ui.screen.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rozetka.presentation.mvi.AuthState

import org.jetbrains.compose.resources.stringResource
import com.rozetka.presentation.generated.resources.Res
import com.rozetka.presentation.generated.resources.*

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    authCode: String?,
    onLoginClick: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    LaunchedEffect(authCode) {
        if (!authCode.isNullOrBlank() && authState !is AuthState.Authenticated) {
            viewModel.authenticate(authCode)
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthenticated()
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        when (authState) {
            is AuthState.Loading -> {
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
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(Res.string.login_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(48.dp))

                    FilledTonalButton(
                        onClick = onLoginClick,
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 20.dp)
                    ) {
                        Text(stringResource(Res.string.login_button), style = MaterialTheme.typography.titleLarge)
                    }

                    if (authState is AuthState.Error) {
                        Spacer(modifier = Modifier.height(16.dp))
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