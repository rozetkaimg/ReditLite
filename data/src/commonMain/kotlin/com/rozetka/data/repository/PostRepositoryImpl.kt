package com.rozetka.data.repository

import com.rozetka.data.local.AppDatabase
import com.rozetka.data.mapper.toDomain
import com.rozetka.data.model.remote.CommentListing
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

class PostRepositoryImpl(
    private val client: HttpClient,
    private val database: AppDatabase
) : PostRepository {

    override suspend fun getComments(postId: String): Result<List<Comment>> {
        return runCatching {
            val response: List<CommentListing> = client.get("https://oauth.reddit.com/comments/$postId") {
                parameter("depth", 5)
            }.body()

            val commentsResponse = response.getOrNull(1)
            commentsResponse?.data?.children?.mapNotNull { wrapper ->
                if (wrapper.data.body == null) null else wrapper.data.toDomain()
            } ?: emptyList()
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
        }
    }

    override suspend fun unsavePost(postId: String): Result<Unit> {
        return runCatching {
            client.post("https://oauth.reddit.com/api/unsave") {
                parameter("id", postId)
            }
        }
    }

    override suspend fun savePostLocally(post: Post) {}

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
        return Result.success(Unit)
    }
}