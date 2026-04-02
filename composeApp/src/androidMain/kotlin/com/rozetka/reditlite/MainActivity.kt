package com.rozetka.reditlite

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val authCodeFlow = MutableStateFlow<String?>(null)
    private val deepLinkFlow = MutableStateFlow<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        requestNotificationPermission()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val authCode by authCodeFlow.collectAsStateWithLifecycle()
            val deepLink by deepLinkFlow.collectAsStateWithLifecycle()
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(
                        windowSizeClass = windowSizeClass,
                        authCode = authCode,
                        deepLink = deepLink,
                        onDeepLinkHandled = { deepLinkFlow.value = null },
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val data: Uri? = intent?.data
        data?.getQueryParameter("code")?.let { code ->
            authCodeFlow.value = code
        }
        
        if (data != null && (data.host == "reddit.com" || data.host == "www.reddit.com")) {
            deepLinkFlow.value = data.toString()
        }
    }
}