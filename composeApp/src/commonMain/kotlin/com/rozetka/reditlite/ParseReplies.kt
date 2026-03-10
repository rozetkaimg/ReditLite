package com.rozetka.reditlite


import com.rozetka.reditlite.models.CommentItem
import com.rozetka.reditlite.models.CommentListing
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

fun parseReplies(element: JsonElement?): List<CommentItem> {
    if (element == null || element is JsonPrimitive) return emptyList()
    return try {
        val json = Json { ignoreUnknownKeys = true }
        val listing = json.decodeFromJsonElement<CommentListing>(element)
        listing.data.children.map { it.data }
    } catch (e: Exception) {
        emptyList()
    }
}