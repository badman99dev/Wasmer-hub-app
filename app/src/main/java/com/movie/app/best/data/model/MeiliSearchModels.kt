package com.movie.app.best.data.model

import com.google.gson.annotations.SerializedName

data class MeiliSearchRequest(
    val q: String,
    val limit: Int = 20,
    val offset: Int = 0,
    val attributesToRetrieve: List<String> = listOf(
        "id", "slug", "title", "poster_url", "release_year",
        "rating", "quality_label", "is_series", "has_stream",
        "audio_label", "content_moderation", "poster_moderation"
    ),
    val attributesToHighlight: List<String> = listOf("title"),
    val highlightPreTag: String = "\u0001",
    val highlightPostTag: String = "\u0002"
)

data class MeiliSearchResponse(
    val hits: List<MeiliHit> = emptyList(),
    val estimatedTotalHits: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0
)

data class MeiliHit(
    val id: Int = 0,
    val slug: String = "",
    val title: String = "",
    @SerializedName("poster_url") val posterUrl: String = "",
    @SerializedName("release_year") val releaseYear: String = "",
    val rating: String = "",
    @SerializedName("quality_label") val qualityLabel: String = "",
    @SerializedName("is_series") val isSeries: Int = 0,
    @SerializedName("has_stream") val hasStream: Boolean? = null,
    @SerializedName("audio_label") val audioLabel: String = "",
    @SerializedName("content_moderation") val contentModeration: Map<String, String>? = null,
    @SerializedName("poster_moderation") val posterModeration: String? = null,
    val _formatted: MeiliFormatted? = null
) {
    val effectiveContentModeration: ContentModeration?
        get() = contentModeration?.toContentModeration()

    val isSeriesBool: Boolean
        get() = isSeries == 1

    fun toWasmerMovie(): WasmerMovie = WasmerMovie(
        id = id,
        slug = slug,
        title = title,
        posterUrl = posterUrl,
        qualityLabel = qualityLabel,
        releaseYear = releaseYear,
        rating = rating,
        audioLabel = audioLabel,
        isSeries = isSeries == 1,
        hasStream = hasStream ?: false,
        views = 0,
        rank = 0,
        contentModeration = effectiveContentModeration,
        posterModeration = posterModeration
    )
}

data class MeiliFormatted(
    val title: String = ""
)

data class MeiliKeyResponse(
    val key: String = "",
    val uid: String = ""
)
