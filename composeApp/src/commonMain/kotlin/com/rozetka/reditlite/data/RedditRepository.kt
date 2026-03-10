package com.rozetka.reditlite.data


import com.rozetka.reditlite.data.NetworkClient
import com.rozetka.reditlite.models.CommentItem
import com.rozetka.reditlite.models.CommentListing
import com.rozetka.reditlite.models.RedditPost
import com.rozetka.reditlite.models.RedditResponse
import com.rozetka.reditlite.models.TokenResponse

import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters
import io.ktor.util.encodeBase64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

class RedditRepository {
    private val client = NetworkClient.httpClient
    private val clientId = "yH0aTnJEt6qUgGn835B4vg"
    private val redirectUri = "redreader://rr_oauth_redir"

    suspend fun getAccessToken(code: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val authString = "$clientId:"
            val encodedAuth = authString.encodeBase64()

            val response = client.submitForm(
                url = "https://www.reddit.com/api/v1/access_token",
                formParameters = parameters {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", redirectUri)
                }
            ) {
                header(HttpHeaders.Authorization, "Basic $encodedAuth")
            }.body<TokenResponse>()

            response.access_token
        }
    }

    suspend fun fetchPosts(query: String = "", after: String? = null): Result<Pair<List<RedditPost>, String?>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url =
                    if (query.isNotBlank()) "https://oauth.reddit.com/r/all/search" else "https://oauth.reddit.com/r/all/hot"
                val response = client.get(url) {
                    if (query.isNotBlank()) parameter("q", query)
                    if (after != null) parameter("after", after)
                    parameter("limit", "15")
                }
                val redditData = response.body<RedditResponse>().data
                val posts = redditData.children.map { it.data }
                Pair(posts, redditData.after)
            }
        }

    suspend fun getComments(subreddit: String, articleId: String): Result<List<CommentItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rawResponse: JsonArray =
                    client.get("https://oauth.reddit.com/r/$subreddit/comments/$articleId").body()
                if (rawResponse.size > 1) {
                    val commentsListing =
                        NetworkClient.jsonConfig.decodeFromJsonElement<CommentListing>(rawResponse[1])
                    commentsListing.data.children.map { it.data }
                } else {
                    emptyList()
                }
            }
        }
}