package com.rozetka.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rozetka.domain.model.UserProfile

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: String = "me",
    val name: String,
    val totalKarma: Int,
    val iconUrl: String?
) {
    fun toDomain() = UserProfile(
        id = id,
        name = name,
        totalKarma = totalKarma,
        iconUrl = iconUrl ?: "",
        trophies = emptyList()
    )
      
}