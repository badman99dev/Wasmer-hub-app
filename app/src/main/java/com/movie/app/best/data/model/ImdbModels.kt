package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class ImdbEpisodeResponse(
    val episodes: List<ImdbEpisode> = emptyList()
)

data class ImdbEpisode(
    val id: String = "",
    val title: String = "",
    val primaryImage: ImdbImage? = null,
    val season: String = "",
    @SerializedName("episodeNumber") val episodeNumber: Int = 0,
    @SerializedName("runtimeSeconds") val runtimeSeconds: Int = 0,
    val plot: String = "",
    val rating: ImdbRating? = null,
    val releaseDate: ImdbReleaseDate? = null
) {
    val stillImageUrl: String get() = primaryImage?.url ?: ""
    val runtimeMinutes: Int get() = runtimeSeconds / 60
    val formattedDate: String get() {
        val d = releaseDate ?: return ""
        val months = listOf("","Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val m = if (d.month in 1..12) months[d.month] else "${d.month}"
        return "${d.day} $m ${d.year}"
    }
    val ratingValue: Double get() = rating?.aggregateRating ?: 0.0
}

data class ImdbImage(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0
)

data class ImdbRating(
    @SerializedName("aggregateRating") val aggregateRating: Double = 0.0,
    @SerializedName("voteCount") val voteCount: Int = 0
)

data class ImdbReleaseDate(
    val year: Int = 0,
    val month: Int = 0,
    val day: Int = 0
)
