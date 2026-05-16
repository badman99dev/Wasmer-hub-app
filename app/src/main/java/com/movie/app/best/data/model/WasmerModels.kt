package com.movie.app.best.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContentModeration(
    val poster: String = "safe",
    val screenshots: String = "safe",
    val storyline: String = "none"
) : Parcelable {
    val isPosterSexual get() = poster == "sexual"
    val isScreenshotsSexual get() = screenshots == "sexual"
    val isStorylineSexual get() = storyline == "sexual"
    val hasAnyFlag get() = isPosterSexual || isScreenshotsSexual || isStorylineSexual
}

data class ContentModerationResponse(
    val poster: String = "safe",
    val screenshots: String = "safe",
    val storyline: String = "none",
    val confidence: String = "low",
    val reasoning: String = "",
    val model: String = ""
)

@Parcelize
data class WasmerMovie(
    val id: Int,
    val slug: String,
    val title: String,
    @SerializedName("poster_url") val posterUrl: String,
    @SerializedName("quality_label") val qualityLabel: String,
    @SerializedName("release_year") val releaseYear: String,
    val rating: String,
    @SerializedName("audio_label") val audioLabel: String,
    @SerializedName("is_series") val isSeries: Boolean,
    @SerializedName("has_stream") val hasStream: Boolean,
    val views: Int,
    @SerializedName("_rank") val rank: Int,
    @SerializedName("content_moderation") val contentModeration: ContentModeration? = null,
    @SerializedName("poster_moderation") val posterModeration: String? = null,
    @SerializedName("stream_avl") val streamAvl: Boolean? = null
) : Parcelable {
    val shouldBlurPoster: Boolean
        get() = contentModeration?.isPosterSexual == true || posterModeration == "sexual"
}

@Parcelize
data class WasmerMovieDetails(
    val id: Int,
    val slug: String,
    val title: String,
    @SerializedName("original_title") val originalTitle: String,
    @SerializedName("poster_url") val posterUrl: String,
    @SerializedName("backdrop_url") val backdropUrl: String,
    @SerializedName("quality_label") val qualityLabel: String,
    @SerializedName("release_year") val releaseYear: String,
    val rating: String,
    val runtime: String,
    val director: String,
    val cast: String,
    @SerializedName("audio_label") val audioLabel: String,
    val language: String,
    val description: String,
    val country: String,
    @SerializedName("youtube_id") val youtubeId: String,
    @SerializedName("imdb_id") val imdbId: String,
    val views: Int,
    @SerializedName("is_series") val isSeries: Boolean,
    @SerializedName("has_stream") val hasStream: Boolean,
    @SerializedName("stream_url") val streamUrl: String,
    @SerializedName("player_url") val playerUrl: String,
    @SerializedName("content_moderation") val contentModeration: ContentModeration? = null
) : Parcelable

@Parcelize
data class WasmerCategory(
    val id: Int,
    @SerializedName("category_name") val categoryName: String,
    val slug: String,
    @SerializedName("banner_url") val bannerUrl: String,
    val count: Int
) : Parcelable

@Parcelize
data class WasmerDownloadLink(
    val id: Int,
    val label: String,
    @SerializedName("link_url") val linkUrl: String,
    val type: String,
    @SerializedName("file_size") val fileSize: String,
    @SerializedName("episode_id") val episodeId: Int?
) : Parcelable

@Parcelize
data class WasmerComment(
    val id: Int,
    @SerializedName("user_name") val userName: String,
    val comment: String,
    @SerializedName("created_at") val createdAt: String
) : Parcelable

@Parcelize
data class WasmerSeriesDetails(
    val id: Int,
    val slug: String,
    val title: String,
    @SerializedName("original_title") val originalTitle: String,
    @SerializedName("poster_url") val posterUrl: String,
    @SerializedName("backdrop_url") val backdropUrl: String,
    @SerializedName("quality_label") val qualityLabel: String,
    @SerializedName("release_year") val releaseYear: String,
    val rating: String,
    val runtime: String,
    val director: String,
    val cast: String,
    @SerializedName("audio_label") val audioLabel: String,
    val language: String,
    val description: String,
    val country: String,
    @SerializedName("youtube_id") val youtubeId: String,
    @SerializedName("imdb_id") val imdbId: String,
    val views: Int,
    @SerializedName("is_series") val isSeries: Boolean,
    val status: String,
    @SerializedName("season_label") val seasonLabel: String,
    @SerializedName("total_episodes") val totalEpisodes: Int,
    @SerializedName("series_group_id") val seriesGroupId: Int,
    @SerializedName("has_stream") val hasStream: Boolean,
    @SerializedName("stream_url") val streamUrl: String,
    @SerializedName("player_url") val playerUrl: String,
    @SerializedName("content_moderation") val contentModeration: ContentModeration? = null
) : Parcelable

@Parcelize
data class WasmerEpisode(
    val id: Int,
    @SerializedName("season_no") val seasonNo: Int,
    @SerializedName("episode_no") val episodeNo: Int,
    val title: String,
    val overview: String,
    @SerializedName("still_path") val stillPath: String,
    @SerializedName("air_date") val airDate: String,
    @SerializedName("vote_average") val voteAverage: Double
) : Parcelable

@Parcelize
data class WasmerSeason(
    val id: Int,
    val title: String,
    @SerializedName("season_label") val seasonLabel: String,
    @SerializedName("poster_url") val posterUrl: String,
    @SerializedName("quality_label") val qualityLabel: String,
    @SerializedName("total_episodes") val totalEpisodes: Int
) : Parcelable

@Parcelize
data class WasmerNotification(
    val id: Int,
    val type: String,
    val content: String,
    @SerializedName("btn_text") val btnText: String,
    @SerializedName("btn_link") val btnLink: String,
    @SerializedName("is_active") val isActive: Boolean
) : Parcelable

@Parcelize
data class WasmerSettings(
    @SerializedName("site_name") val siteName: String,
    @SerializedName("maintenance_mode") val maintenanceMode: String,
    @SerializedName("slider_mode") val sliderMode: String,
    @SerializedName("slider_limit") val sliderLimit: String,
    @SerializedName("enable_slider") val enableSlider: String,
    @SerializedName("movie_sort_order") val movieSortOrder: String,
    @SerializedName("stream_url_source") val streamUrlSource: String,
    @SerializedName("ad_header") val adHeader: String,
    @SerializedName("ad_footer") val adFooter: String,
    @SerializedName("telegram") val telegram: String,
    @SerializedName("discord") val discord: String,
    @SerializedName("instagram") val instagram: String,
    @SerializedName("footer_text") val footerText: String
) : Parcelable

data class WasmerApiResponse<T>(
    val status: String,
    val data: T?,
    val message: String?
)

data class WasmerSearchResult(
    val query: String,
    val results: List<WasmerMovie>,
    val total: Int,
    val page: Int,
    @SerializedName("total_pages") val totalPages: Int
)

data class WasmerOffsetResult(
    val items: List<WasmerMovie>,
    val total: Int,
    val offset: Int,
    val limit: Int
)

data class WasmerCategoryOffsetResult(
    val category: WasmerCategory,
    val items: List<WasmerMovie>,
    val total: Int,
    val offset: Int,
    val limit: Int
)

data class WasmerPageResult(
    val movies: List<WasmerMovie>,
    val total: Int,
    val page: Int,
    @SerializedName("total_pages") val totalPages: Int,
    @SerializedName("sort_order") val sortOrder: String,
    val category: WasmerCategory?,
    val search: String?
)

data class WasmerDownloadResult(
    val movie: WasmerDownloadMovieInfo,
    val link: WasmerDownloadLink,
    @SerializedName("target_url") val targetUrl: String,
    @SerializedName("target_url_encoded") val targetUrlEncoded: String,
    @SerializedName("bypass_api") val bypassApi: String,
    val alternatives: List<WasmerDownloadLink>,
    val redirect: Boolean,
    @SerializedName("redirect_url") val redirectUrl: String?
)

@Parcelize
data class WasmerDownloadMovieInfo(
    val id: Int,
    val title: String,
    @SerializedName("poster_url") val posterUrl: String,
    @SerializedName("release_year") val releaseYear: String
) : Parcelable

data class WasmerSliderResult(
    val mode: String,
    val limit: Int,
    val movies: List<WasmerMovie>
)

data class WasmerMovieDetailResponse(
    val movie: WasmerMovieDetails,
    val categories: List<WasmerCategorySimple>,
    val download_links: List<WasmerDownloadLink>,
    val comments: List<WasmerComment>,
    val screenshots: List<String>,
    /** Optional similar / related movies returned by the API */
    @SerializedName("similar_movies")
    val similar_movies: List<WasmerMovie> = emptyList()
)

@Parcelize
data class WasmerCategorySimple(
    val id: String,
    @SerializedName("category_name") val categoryName: String,
    val slug: String
) : Parcelable

data class WasmerSeriesDetailResponse(
    val movie: WasmerSeriesDetails,
    val categories: List<WasmerCategorySimple>,
    val episodes_by_season: Map<String, List<WasmerEpisode>>,
    val links_by_episode: Map<String, List<WasmerDownloadLink>>,
    val links_no_episode: List<WasmerDownloadLink>,
    val comments: List<WasmerComment>,
    val screenshots: List<String>,
    val more_seasons: List<WasmerSeason>
)
