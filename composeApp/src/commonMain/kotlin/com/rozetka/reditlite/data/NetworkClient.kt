package com.rozetka.reditlite.data

import com.rozetka.reditlite.models.TokenResponse
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.encodeBase64
import kotlinx.serialization.json.Json

object NetworkClient {
    lateinit var storageManager: SecureStorageManager

    val jsonConfig = Json { ignoreUnknownKeys = true; isLenient = true }

    val httpClient by lazy {
        HttpClient {
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            install(Logging) {
                level = LogLevel.ALL
                logger = object : Logger {
                    override fun log(message: String) {
                        Napier.d(tag = "KtorNetwork", message = message)
                    }
                }
            }
            install(Auth) {
                bearer {
                    loadTokens {
                        val access = storageManager.accessToken
                        val refresh = storageManager.refreshToken
                        if (access != null && refresh != null) {
                            BearerTokens(access, refresh)
                        } else null
                    }

                    refreshTokens {
                        val currentRefresh = storageManager.refreshToken ?: return@refreshTokens null
                        try {
                            val authString = "yH0aTnJEt6qUgGn835B4vg:"
                            val encodedAuth = authString.encodeBase64()
                            val response = client.submitForm(
                                url = "https://www.reddit.com/api/v1/access_token",
                                formParameters = parameters {
                                    append("grant_type", "refresh_token")
                                    append("refresh_token", currentRefresh)
                                }
                            ) {
                                header(HttpHeaders.Authorization, "Basic $encodedAuth")
                            }.body<TokenResponse>()

                            storageManager.accessToken = response.access_token


                            if (response.refresh_token != null) {
                                storageManager.refreshToken = response.refresh_token
                            }

                            BearerTokens(
                                accessToken = response.access_token,
                                refreshToken = storageManager.refreshToken!!
                            )
                        } catch (e: Exception) {
                            Napier.e("Token refresh failed", e, "KtorNetwork")
                            storageManager.clearTokens()
                            null
                        }
                    }

                    sendWithoutRequest { request ->
                        request.url.host == "oauth.reddit.com"
                    }
                }
            }
            defaultRequest {
                header(HttpHeaders.UserAgent, "CMPApp/1.0.0")
            }
        }
    }
}