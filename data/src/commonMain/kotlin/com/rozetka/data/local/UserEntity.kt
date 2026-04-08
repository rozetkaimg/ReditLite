package com.rozetka.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rozetka.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: String = "me",
    val name: String,
    val totalKarma: Int,
    val linkKarma: Int,
    val commentKarma: Int,
    val iconUrl: String?,
    val createdUtc: Double
) {
    fun toDomain() = UserProfile(
        id = id,
        name = name,
        totalKarma = totalKarma,
        linkKarma = linkKarma,
        commentKarma = commentKarma,
        iconUrl = iconUrl ?: "",
        createdUtc = createdUtc,
        trophies = emptyList()
    )
}
