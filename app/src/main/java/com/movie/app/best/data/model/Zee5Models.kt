package com.movie.app.best.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

// ─── ZEE5 Collection / Search Response ───

@Parcelize
data class Zee5CollectionResponse(
    val buckets: List<Zee5Bucket>? = null,
    val items: List<Zee5Item>? = null,
    val episode: List<Zee5Item>? = null,
    val total: Int? = null,
    val title: String? = null,
    val id: String? = null
) : Parcelable

@Parcelize
data class Zee5Bucket(
    val id: String? = null,
    val title: String? = null,
    @SerializedName("collection_id") val collectionId: String? = null,
    @SerializedName("total_items") val totalItems: Int? = null,
    val items: List<Zee5Item>? = null,
    val description: String? = null
) : Parcelable

@Parcelize
data class Zee5Item(
    val id: String? = null,
    val title: String? = null,
    @SerializedName("original_title") val originalTitle: String? = null,
    val description: String? = null,
    val duration: Int? = null,
    @SerializedName("asset_type") val assetType: Int? = null,
    @SerializedName("assetType") val assetTypeCamel: Int? = null,
    @SerializedName("asset_subtype") val assetSubtype: String? = null,
    @SerializedName("assetSubType") val assetSubtypeCamel: String? = null,
    @SerializedName("business_type") val businessType: String? = null,
    @SerializedName("businessType") val businessTypeCamel: String? = null,
    val image: Zee5Image? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("imageUrlLandscape") val imageUrlLandscape: String? = null,
    val languages: List<String>? = null,
    val tags: List<String>? = null,
    @SerializedName("age_rating") val ageRating: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("episode_number") val episodeNumber: Int? = null,
    val tvshow: Zee5TvShowRef? = null,
    @SerializedName("tvShow") val tvShowCamel: Zee5TvShowRef? = null,
    val genres: List<Zee5Genre>? = null,
    val genre: List<Zee5Genre>? = null,
    @SerializedName("stream_url_hls") val streamUrlHls: String? = null,
    @SerializedName("stream_url") val streamUrl: String? = null,
    val hls: List<String>? = null,
    val video: List<String>? = null,
    @SerializedName("is_drm") val isDrm: Int? = null,
    @SerializedName("isDrm") val isDrmCamel: Int? = null,
    @SerializedName("on_air") val onAir: String? = null,
    val slug: String? = null
) : Parcelable {
    val effectiveType: Int
        get() = assetType ?: assetTypeCamel ?: 0
    
    val effectiveSubtype: String
        get() = assetSubtype ?: assetSubtypeCamel ?: ""
    
    val effectiveBusinessType: String
        get() = businessType ?: businessTypeCamel ?: ""
    
    val effectiveImageUrl: String?
        get() = imageUrl ?: image?.portrait?.let { buildPortraitUrl(id ?: "", it) }
    
    val effectiveLandscapeUrl: String?
        get() = imageUrlLandscape ?: image?.list?.let { buildLandscapeUrl(id ?: "", it) }
    
    val isMovie: Boolean
        get() = effectiveType == 0
    
    val isTvShow: Boolean
        get() = effectiveType == 6
    
    val isEpisode: Boolean
        get() = effectiveType == 1
    
    val isLive: Boolean
        get() = effectiveType == 9
    
    val isSeries: Boolean
        get() = isTvShow || (tvshow != null || tvShowCamel != null)
    
    companion object {
        private fun buildPortraitUrl(id: String, hash: String): String {
            return "https://akamaividz2.zee5.com/image/upload/w_400,h_600,c_scale/v1557819982/resources/${id}/portrait/${hash}.jpg"
        }
        private fun buildLandscapeUrl(id: String, hash: String): String {
            return "https://akamaividz2.zee5.com/image/upload/w_800,h_450,c_scale/v1557819982/resources/${id}/list/${hash}.jpg"
        }
    }
}

@Parcelize
data class Zee5Image(
    val portrait: String? = null,
    @SerializedName("portraitclean") val portraitClean: String? = null,
    val list: String? = null,
    val cover: String? = null
) : Parcelable

@Parcelize
data class Zee5TvShowRef(
    val id: String? = null,
    val title: String? = null,
    @SerializedName("assetSubType") val assetSubType: String? = null
) : Parcelable

@Parcelize
data class Zee5Genre(
    val id: String? = null,
    val value: String? = null
) : Parcelable

// ─── ZEE5 Search (GQL) Response ───

@Parcelize
data class Zee5SearchResponse(
    val data: Zee5SearchData? = null
) : Parcelable

@Parcelize
data class Zee5SearchData(
    @SerializedName("hybridSearchResults") val hybridSearchResults: Zee5HybridSearchResults? = null
) : Parcelable

@Parcelize
data class Zee5HybridSearchResults(
    val queryId: String? = null,
    val total: Int? = null,
    val rails: List<Zee5SearchRail>? = null
) : Parcelable

@Parcelize
data class Zee5SearchRail(
    val id: String? = null,
    val title: String? = null,
    @SerializedName("originalTitle") val originalTitle: String? = null,
    val contents: List<Zee5SearchContent>? = null
) : Parcelable

@Parcelize
data class Zee5SearchContent(
    val movie: Zee5Item? = null,
    val episode: Zee5Item? = null,
    @SerializedName("tvShowDetails") val tvShowDetails: Zee5Item? = null
) : Parcelable {
    val effectiveItem: Zee5Item?
        get() = movie ?: episode ?: tvShowDetails
}

// ─── ZEE5 Detail Response ───

@Parcelize
data class Zee5DetailResponse(
    val id: String? = null,
    val title: String? = null,
    @SerializedName("original_title") val originalTitle: String? = null,
    val description: String? = null,
    val actors: List<String>? = null,
    val directors: List<String>? = null,
    val duration: Int? = null,
    @SerializedName("age_rating") val ageRating: String? = null,
    @SerializedName("content_owner") val contentOwner: String? = null,
    @SerializedName("business_type") val businessType: String? = null,
    @SerializedName("on_air") val onAir: String? = null,
    val tags: List<String>? = null,
    val genres: List<Zee5Genre>? = null,
    val genre: List<Zee5Genre>? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("asset_type") val assetType: Int? = null,
    @SerializedName("asset_subtype") val assetSubtype: String? = null,
    val image: Zee5Image? = null,
    @SerializedName("imageUrl") val imageUrl: String? = null,
    @SerializedName("imageUrlLandscape") val imageUrlLandscape: String? = null,
    @SerializedName("stream_url_hls") val streamUrlHls: String? = null,
    @SerializedName("stream_url") val streamUrl: String? = null,
    val hls: List<String>? = null,
    val video: List<String>? = null,
    @SerializedName("is_drm") val isDrm: Int? = null,
    @SerializedName("episode_number") val episodeNumber: Int? = null,
    val related: List<Zee5Item>? = null,
    @SerializedName("related_collections_ss") val relatedCollections: Zee5RelatedCollections? = null,
    val seasons: List<Zee5Season>? = null,
    @SerializedName("list_image") val listImage: String? = null,
    @SerializedName("cover_image") val coverImage: String? = null,
    val free: Boolean? = null,
    val direct: String? = null,
    val proxied: String? = null,
    val languages: List<String>? = null,
    val slug: String? = null
) : Parcelable {
    val effectiveImageUrl: String?
        get() = imageUrl ?: image?.portrait?.let { buildPortraitUrl(id ?: "", it) }
    
    val effectiveLandscapeUrl: String?
        get() = imageUrlLandscape ?: image?.list?.let { buildLandscapeUrl(id ?: "", it) }
    
    val isMovie: Boolean
        get() = assetType == 0
    
    val isTvShow: Boolean
        get() = assetType == 6
    
    val isEpisode: Boolean
        get() = assetType == 1
    
    companion object {
        private fun buildPortraitUrl(id: String, hash: String): String {
            return "https://akamaividz2.zee5.com/image/upload/w_400,h_600,c_scale/v1557819982/resources/${id}/portrait/${hash}.jpg"
        }
        private fun buildLandscapeUrl(id: String, hash: String): String {
            return "https://akamaividz2.zee5.com/image/upload/w_800,h_450,c_scale/v1557819982/resources/${id}/list/${hash}.jpg"
        }
    }
}

@Parcelize
data class Zee5RelatedCollections(
    val id: String? = null,
    val title: String? = null
) : Parcelable

@Parcelize
data class Zee5Season(
    val id: String? = null,
    val title: String? = null,
    @SerializedName("season_number") val seasonNumber: Int? = null,
    @SerializedName("total_episodes") val totalEpisodes: Int? = null
) : Parcelable

// ─── ZEE5 Playback Response ───

@Parcelize
data class Zee5PlaybackResponse(
    val id: String? = null,
    val title: String? = null,
    @SerializedName("stream_url_hls") val streamUrlHls: String? = null,
    @SerializedName("stream_url") val streamUrl: String? = null,
    val hls: List<String>? = null,
    val video: List<String>? = null,
    @SerializedName("is_drm") val isDrm: Int? = null,
    val free: Boolean? = null,
    val direct: String? = null,
    val proxied: String? = null,
    val duration: Int? = null,
    @SerializedName("asset_type") val assetType: Int? = null
) : Parcelable {
    val effectiveStreamUrl: String?
        get() = proxied ?: direct ?: streamUrlHls ?: hls?.firstOrNull() ?: streamUrl
    
    val hasProxiedUrl: Boolean
        get() = !proxied.isNullOrBlank()
    
    val isZee5Content: Boolean = true
}
