package com.rozetka.domain.model

sealed class FeedType(val path: String, val name: String) {
    object HOT : FeedType("hot", "Hot")
    object NEW : FeedType("new", "New")
    object BEST : FeedType("best", "Best")
    object TOP : FeedType("top", "Top")
    object RISING : FeedType("rising", "Rising")
    object SAVED : FeedType("saved", "Saved")
    data class Subreddit(val subredditName: String, val sort: String = "hot") : 
        FeedType("r/${subredditName}/${sort}", subredditName)
    
    companion object {
        val entries by lazy { listOf(HOT, NEW, BEST, TOP, RISING, SAVED) }
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
