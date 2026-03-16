package com.rozetka.reditlite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.rozetka.reditlite.data.SecureStorageManager
import com.rozetka.reditlite.data.local.AppDatabase
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {


    private val authCodeFlow = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {

            val authCode by authCodeFlow.collectAsState()

            val settings = remember { Settings() }
            val storageManager = remember { SecureStorageManager(settings) }

            val database = remember {
                val dbFile = applicationContext.getDatabasePath("reddit_app.db")
                Room.databaseBuilder<AppDatabase>(
                    context = applicationContext,
                    name = dbFile.absolutePath
                ).setDriver(BundledSQLiteDriver()).build()
            }

            App(
                authCode = authCode,
                onLoginClick = {
                    val clientId = "yH0aTnJEt6qUgGn835B4vg"
                    val redirectUri = "redreader://rr_oauth_redir"
                    val url =
                        "https://www.reddit.com/api/v1/authorize.compact?client_id=$clientId&response_type=code&state=random_state_string&redirect_uri=$redirectUri&duration=permanent&scope=read"

                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(browserIntent)
                },
                storageManager = storageManager,
                database = database
            )
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }


    private fun handleIntent(intent: Intent?) {
        val code = intent?.data?.getQueryParameter("code")
        if (code != null) {
            authCodeFlow.value = code
        }
    }
}