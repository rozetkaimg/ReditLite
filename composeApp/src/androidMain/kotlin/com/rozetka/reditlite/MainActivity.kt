package com.rozetka.reditlite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val authCodeFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val authCode by authCodeFlow.collectAsStateWithLifecycle()
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        authCode = authCode,
                        onLoginClick = {
                            val clientId = "yH0aTnJEt6qUgGn835B4vg"
                            val redirectUri = "redreader://rr_oauth_redir"
                            val url = "https://www.reddit.com/api/v1/authorize.compact?client_id=$clientId&response_type=code&state=random_state_string&redirect_uri=$redirectUri&duration=permanent&scope=identity read vote submit subscribe history mysubreddits"

                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.getQueryParameter("code")?.let { code ->
            authCodeFlow.value = code
        }
    }
}