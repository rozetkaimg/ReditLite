package com.rozetka.data.model.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.rozetka.domain.model.Trophy

@Entity(tableName = "trophies")
data class TrophyEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconUrl: String,
    val description: String?
) {
    fun toDomain() = Trophy(id = id, name = name, iconUrl = iconUrl, description = description)
}