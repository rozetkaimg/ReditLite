package com.rozetka.domain.model

enum class FeedType(val path: String) {
    HOT("r/popular/hot"),
    NEW("r/popular/new"),
    BEST("r/popular/best"),
    TOP("r/popular/top")
}