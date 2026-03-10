package com.rozetka.reditlite


import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val authCode = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            App(
                authCode = authCode.value,
                onLoginClick = {
                    val url =
                        "https://www.reddit.com/api/v1/authorize?response_type=code&client_id=yH0aTnJEt6qUgGn835B4vg&redirect_uri=redreader://rr_oauth_redir&scope=read&state=Texas&duration=permanent"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data
        if (uri != null && uri.scheme == "redreader" && uri.host == "rr_oauth_redir") {
            authCode.value = uri.getQueryParameter("code")
        }
    }
}