package com.rozetka.domain.model

sealed class FeedType(val path: String, val name: String) {
    object HOT : FeedType("r/popular/hot", "Hot")
    object NEW : FeedType("r/popular/new", "New")
    object BEST : FeedType("r/popular/best", "Best")
    object TOP : FeedType("r/popular/top", "Top")
    object SAVED : FeedType("saved", "Saved")
    data class Subreddit(val subredditName: String, val sort: String = "hot") : 
        FeedType("r/${subredditName}/${sort}", subredditName)
    
    companion object {
        val entries by lazy { listOf(HOT, NEW, BEST, TOP, SAVED) }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FeedType) return false
        return path == other.path
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}
