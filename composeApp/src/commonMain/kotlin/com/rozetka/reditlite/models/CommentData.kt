package com.rozetka.reditlite.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement


@Serializable
data class CommentListing(val data: CommentData)

@Serializable
data class CommentData(val children: List<CommentWrapper>)

@Serializable
data class CommentWrapper(val data: CommentItem)

@Serializable
data class CommentItem(
    val id: String? = null,
    val author: String? = null,
    val body: String? = null,
    val score: Int? = null,
    val replies: JsonElement? = null
)

