package com.rozetka.data.repository

import com.rozetka.data.local.AppDatabase
import com.rozetka.data.mapper.toDomain
import com.rozetka.data.mapper.toEntity
import com.rozetka.data.model.remote.CommentListing
import com.rozetka.data.model.remote.MediaAssetResponse
import com.rozetka.data.model.remote.RedditListingResponse
import com.rozetka.domain.model.Comment
import com.rozetka.domain.model.Post
import com.rozetka.domain.model.VoteDirection
import com.rozetka.domain.repository.PostRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.parameters
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess

class PostRepositoryImpl(
    private val client: HttpClient,
    private val database: AppDatabase
) : PostRepository {

    override suspend fun getComments(postId: String): Result<List<Comment>> {
        return getPostAndComments(postId, "best").map { it.second }
    }

    override suspend fun getPostAndComments(postId: String, sort: String): Result<Pair<Post, List<Comment>>> {
        return runCatching {
            val cleanPostId = postId.removePrefix("t3_")
            val url = "https://oauth.reddit.com/comments/$cleanPostId"

            val response = client.get(url) {
                parameter("depth", 10)
                parameter("limit", 100)
                parameter("sort", sort)
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                throw Exception("Failed to fetch comments: ${response.status} - $errorBody")
            }

            val json = Json { ignoreUnknownKeys = true; isLenient = true }
            val listings: List<JsonElement> = response.body()

            val postListing = listings.getOrNull(0)?.let {
                json.decodeFromJsonElement<RedditListingResponse>(it)
            }
            val commentListing = listings.getOrNull(1)?.let {
                json.decodeFromJsonElement<CommentListing>(it)
            }

            val post = postListing?.data?.children?.firstOrNull()?.data?.toDomain()
                ?: throw Exception("Post not found")

            val comments = commentListing?.data?.children?.mapNotNull { wrapper ->
                if (wrapper.kind == "more") {
                    Comment(
                        id = wrapper.data.id ?: "",
                        author = "",
                        body = "Load more comments...",
                        score = 0,
                        voteStatus = VoteDirection.NONE,
                        depth = wrapper.data.depth ?: 0,
                        replies = emptyList()
                    )
                } else if (wrapper.data.body == null) {
                    null
                } else {
                    wrapper.data.toDomain()
                }
            } ?: emptyList()

            Pair(post, comments)
        }.onFailure {
            it.printStackTrace()
        }
    }

    override suspend fun vote(id: String, direction: VoteDirection): Result<Unit> {
        return runCatching {
            client.post("https://oauth.reddit.com/api/vote") {
                parameter("id", id)
                parameter("dir", direction.value)
            }
        }
    }

    override suspend fun savePost(postId: String): Result<Unit> {
        return runCatching {
            client.post("https://oauth.reddit.com/api/save") {
                parameter("id", postId)
            }
            database.postDao().updateSaveStatus(postId, true)
        }
    }

    override suspend fun unsavePost(postId: String): Result<Unit> {
        return runCatching {
            client.post("https://oauth.reddit.com/api/unsave") {
                parameter("id", postId)
            }
            database.postDao().updateSaveStatus(postId, false)
        }
    }

    override suspend fun savePostLocally(post: Post) {
        database.postDao().insertPost(post.toEntity("saved").copy(isSaved = true))
    }

    override fun getLocalSavedPosts(): Flow<List<Post>> {
        return database.postDao().getSavedPosts().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun submitTextPost(subreddit: String, title: String, text: String): Result<Unit> {
        return runCatching {
            val response = client.submitForm(
                url = "https://oauth.reddit.com/api/submit",
                formParameters = parameters {
                    append("sr", subreddit.removePrefix("r/"))
                    append("kind", "self")
                    append("title", title)
                    append("text", text)
                    append("api_type", "json")
                }
            )
            val body = response.bodyAsText()
            checkRedditError(body)
        }
    }

    override suspend fun submitLinkPost(subreddit: String, title: String, url: String): Result<Unit> {
        return runCatching {
            val response = client.submitForm(
                url = "https://oauth.reddit.com/api/submit",
                formParameters = parameters {
                    append("sr", subreddit.removePrefix("r/"))
                    append("kind", "link")
                    append("title", title)
                    append("url", url)
                    append("api_type", "json")
                }
            )
            val body = response.bodyAsText()
            checkRedditError(body)
        }
    }

    override suspend fun submitImagePost(subreddit: String, title: String, imageUri: String): Result<Unit> {
        return Result.failure(Exception("Use submitImagePostWithBytes instead"))
    }

    override suspend fun submitImagePostWithBytes(
        subreddit: String,
        title: String,
        imageBytes: ByteArray,
        fileName: String
    ): Result<Unit> {
        return runCatching {
            val rawExtension = fileName.substringAfterLast(".", "jpg").lowercase()
            val extension = if (rawExtension == "jpeg") "jpg" else rawExtension
            val mimeType = when (extension) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                else -> "image/jpeg"
            }
            val cleanFileName = "image.$extension"

            // 1. Get upload lease
            val leaseResponse = client.submitForm(
                url = "https://oauth.reddit.com/api/media/asset.json",
                formParameters = parameters {
                    append("filepath", cleanFileName)
                    append("mimetype", mimeType)
                }
            )

            if (!leaseResponse.status.isSuccess()) {
                val errorBody = leaseResponse.bodyAsText()
                throw Exception("Failed to get upload lease: ${leaseResponse.status} - $errorBody")
            }

            val lease = leaseResponse.body<MediaAssetResponse>()
            val assetId = lease.asset?.asset_id ?: throw Exception("Invalid lease response: asset_id missing")
            val action = lease.args?.action ?: throw Exception("Invalid lease response: action missing")
            val fields = lease.args.fields

            // 2. Upload the actual image
            val uploadUrl = if (action.startsWith("//")) "https:$action" else action

            val uploadResponse = client.post(uploadUrl) {
                setBody(MultiPartFormDataContent(
                    formData {
                        fields.forEach { field ->
                            append(field.name, field.value)
                        }
                        append("file", imageBytes, Headers.build {
                            append(HttpHeaders.ContentType, mimeType)
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$cleanFileName\"")
                        })
                    }
                ))
            }

            if (!uploadResponse.status.isSuccess()) {
                val errorBody = uploadResponse.bodyAsText()
                throw Exception("Failed to upload image: ${uploadResponse.status} - $errorBody")
            }

            // 3. Submit the post with the uploaded image URL
            // Give Reddit a moment to process the uploaded asset
            delay(1000)

            val submitResponse = client.submitForm(
                url = "https://oauth.reddit.com/api/submit",
                formParameters = parameters {
                    append("sr", subreddit.removePrefix("r/"))
                    append("kind", "image")
                    append("title", title)
                    append("resubmit", "true")
                    append("api_type", "json")
                    append("url", "https://i.redd.it/$assetId.$extension")
                }
            )
            val body = submitResponse.bodyAsText()
            checkRedditError(body)
        }
    }

    private fun checkRedditError(jsonString: String) {
        val json = Json { ignoreUnknownKeys = true }
        val root = json.parseToJsonElement(jsonString).jsonObject
        val redditJson = root["json"]?.jsonObject
        val errors = redditJson?.get("errors")?.jsonArray
        if (errors != null && errors.isNotEmpty()) {
            val error = errors[0].jsonArray
            val errorType = error.getOrNull(0)?.jsonPrimitive?.content ?: "Unknown"
            val errorMessage = error.getOrNull(1)?.jsonPrimitive?.content ?: "Submission failed"
            throw Exception("$errorType: $errorMessage")
        }
    }


    override fun enqueuePostSubmission(
        subreddit: String,
        title: String,
        content: String,
        type: String,
        mediaUri: String?
    ) {
    }

    override suspend fun searchPosts(subreddit: String, query: String): Result<List<Post>> {
        return runCatching {
            val response = client.get("https://oauth.reddit.com/r/${subreddit.removePrefix("r/")}/search") {
                parameter("q", query)
                parameter("restrict_sr", "on")
                parameter("sort", "relevance")
                parameter("t", "all")
            }

            if (!response.status.isSuccess()) {
                throw Exception("Failed to search posts: ${response.status}")
            }

            val listing = response.body<RedditListingResponse>()
            listing.data.children.map { it.data.toDomain() }
        }
    }

    override suspend fun submitComment(
        parentId: String,
        text: String,
        mediaBytes: ByteArray?,
        fileName: String?
    ): Result<Comment> {
        return runCatching {
            var finalBody = text

            if (mediaBytes != null && fileName != null) {
                val rawExtension = fileName.substringAfterLast(".", "jpg").lowercase()
                val extension = if (rawExtension == "jpeg") "jpg" else rawExtension
                val mimeType = when (extension) {
                    "png" -> "image/png"
                    "gif" -> "image/gif"
                    else -> "image/jpeg"
                }
                val cleanFileName = "image.$extension"

                // 1. Get upload lease
                val leaseResponse = client.submitForm(
                    url = "https://oauth.reddit.com/api/media/asset.json",
                    formParameters = parameters {
                        append("filepath", cleanFileName)
                        append("mimetype", mimeType)
                    }
                )

                if (leaseResponse.status.isSuccess()) {
                    val lease = leaseResponse.body<MediaAssetResponse>()
                    val assetId = lease.asset?.asset_id
                    val action = lease.args?.action
                    val fields = lease.args?.fields

                    if (assetId != null && action != null && fields != null) {
                        // 2. Upload the actual image to the URL provided in the lease
                        val uploadUrl = if (action.startsWith("//")) "https:$action" else action
                        
                        val uploadResponse = client.post(uploadUrl) {
                            setBody(MultiPartFormDataContent(
                                formData {
                                    fields.forEach { field ->
                                        append(field.name, field.value)
                                    }
                                    append("file", mediaBytes, Headers.build {
                                        append(HttpHeaders.ContentType, mimeType)
                                        append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$cleanFileName\"")
                                    })
                                }
                            ))
                        }

                        if (uploadResponse.status.isSuccess()) {
                            val imageUrl = "https://i.redd.it/$assetId.$extension"
                            finalBody += "\n\n![]($imageUrl)"
                        }
                    }
                }
            }

            val response = client.submitForm(
                url = "https://oauth.reddit.com/api/comment",
                formParameters = parameters {
                    append("parent", parentId)
                    append("text", finalBody)
                    append("api_type", "json")
                }
            )

            if (!response.status.isSuccess()) {
                throw Exception("Failed to submit comment: ${response.status}")
            }

            val body = response.bodyAsText()
            val json = Json { ignoreUnknownKeys = true }
            val jsonElement = json.parseToJsonElement(body)

            val newCommentData = jsonElement.jsonObject["json"]
                ?.jsonObject?.get("data")
                ?.jsonObject?.get("things")
                ?.jsonArray?.get(0)
                ?.jsonObject?.get("data")

            if (newCommentData != null) {
                Comment(
                    id = newCommentData.jsonObject["id"]?.jsonPrimitive?.content ?: "",
                    author = newCommentData.jsonObject["author"]?.jsonPrimitive?.content ?: "me",
                    body = text,
                    score = 1,
                    voteStatus = VoteDirection.NONE,
                    depth = 0,
                    replies = emptyList()
                )
            } else {
                throw Exception("Could not parse new comment from response")
            }
        }
    }
}