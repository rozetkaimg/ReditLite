package com.rozetka.domain.model

data class NotificationMessage(
    val id: String,
    val author: String,
    val subject: String,
    val body: String,
    val isUnread: Boolean
)