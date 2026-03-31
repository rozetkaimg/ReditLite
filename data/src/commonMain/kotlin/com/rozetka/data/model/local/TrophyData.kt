package com.rozetka.data.model.local



import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RedditTrophyResponse(
    @SerialName("kind") val kind: String,
    @SerialName("data") val data: TrophyListData
)

@Serializable
data class TrophyListData(
    @SerialName("trophies") val trophies: List<TrophyWrapper>
)

@Serializable
data class TrophyWrapper(
    @SerialName("kind") val kind: String,
    @SerialName("data") val data: TrophyData
)

@Serializable
data class TrophyData(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String,
    @SerialName("icon_70") val icon_70: String? = null,
    @SerialName("icon_40") val icon_40: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("url") val url: String? = null
)