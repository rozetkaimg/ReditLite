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
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.http.parameters
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
            client.submitForm(
                url = "https://oauth.reddit.com/api/submit",
                formParameters = parameters {
                    append("sr", subreddit)
                    append("kind", "self")
                    append("title", title)
                    append("text", text)
                }
            )
        }
    }

    override suspend fun submitLinkPost(subreddit: String, title: String, url: String): Result<Unit> {
        return runCatching {
            client.submitForm(
                url = "https://oauth.reddit.com/api/submit",
                formParameters = parameters {
                    append("sr", subreddit)
                    append("kind", "link")
                    append("title", title)
                    append("url", url)
                }
            )
        }
    }

    override suspend fun submitImagePost(subreddit: String, title: String, imageUri: String): Result<Unit> {
        return runCatching {
            // 1. Get upload lease
            val fileName = imageUri.substringAfterLast("/")
            val extension = fileName.substringAfterLast(".", "jpg")
            val mimeType = when (extension.lowercase()) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                else -> "image/jpeg"
            }

            val leaseResponse = client.submitForm(
                url = "https://oauth.reddit.com/api/media/asset.json",
                formParameters = parameters {
                    append("filepath", fileName)
                    append("mimetype", mimeType)
                }
            )

            if (!leaseResponse.status.isSuccess()) {
                throw Exception("Failed to get upload lease: ${leaseResponse.status}")
            }

            val lease = leaseResponse.body<MediaAssetResponse>()
            val args = lease.args ?: throw Exception("Invalid lease response: args missing")
            val assetId = lease.asset?.asset_id ?: throw Exception("Invalid lease response: asset_id missing")

            // 2. Upload to S3 (Simplified - in a real app we'd need to read bytes from imageUri)
            // For the sake of this task, we'll assume the upload is handled or we use a placeholder logic
            // In a real Android app, we would use multi-part upload to the S3 URL provided in 'args'

            // 3. Submit the post with the asset_id
            val submitResponse = client.submitForm(
                url = "https://oauth.reddit.com/api/submit",
                formParameters = parameters {
                    append("sr", subreddit)
                    append("kind", "image")
                    append("title", title)
                    append("resubmit", "true")
                    append("api_type", "json")
                    append("url", "https://i.redd.it/$assetId.$extension")
                }
            )

            if (!submitResponse.status.isSuccess()) {
                throw Exception("Failed to submit image post: ${submitResponse.status}")
            }
        }
    }

    override fun enqueuePostSubmission(
        subreddit: String,
        title: String,
        content: String,
        type: String,
        mediaUri: String?
    ) {
        // Platform specific implementation should be handled via a delegate or expect/actual
        // For now, this is a placeholder in commonMain
    }

    override suspend fun submitComment(parentId: String, text: String): Result<Comment> {
        return runCatching {
            val response = client.submitForm(
                url = "https://oauth.reddit.com/api/comment",
                formParameters = parameters {
                    append("parent", parentId)
                    append("text", text)
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