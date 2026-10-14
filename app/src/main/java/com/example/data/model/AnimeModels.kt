package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AnimeApiResponse<T>(
    @Json(name = "status") val status: Any? = null,
    @Json(name = "error") val error: Any? = null,
    @Json(name = "data") val data: T? = null
)

@JsonClass(generateAdapter = true)
data class MovieListContainer(
    @Json(name = "movie") val movie: List<AnimeItem>? = null
)

@JsonClass(generateAdapter = true)
data class EpisodeListContainer(
    @Json(name = "episode") val episode: List<AnimeEpisodeItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "image_poster") val image_poster: String? = null,
    @Json(name = "image_cover") val image_cover: String? = null,
    @Json(name = "image") val rawImage: String? = null,
    @Json(name = "score") val score: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "views") val views: String? = null,
    @Json(name = "year") val year: String? = null,
    @Json(name = "studio") val studio: String? = null
) {
    val image: String
        get() = formatImageUrl(image_poster ?: image_cover ?: rawImage)

    val displayScore: String
        get() {
            if (!score.isNullOrBlank()) return score
            val v = views?.toLongOrNull() ?: 1000000L
            val scoreVal = 8.2 + ((v.coerceIn(100000L, 16000000L) - 100000L) / 15900000.0) * 1.5
            return String.format(java.util.Locale.US, "%.1f", scoreVal)
        }

    val numericScore: Double
        get() = score?.toDoubleOrNull() ?: displayScore.toDoubleOrNull() ?: 8.5
}

@JsonClass(generateAdapter = true)
data class AnimeDetailData(
    @Json(name = "movie") val movie: AnimeMovieDetail? = null,
    @Json(name = "episode") val episode: List<AnimeEpisodeItem>? = null,
    @Json(name = "season") val season: List<AnimeSeasonItem>? = null
)

@JsonClass(generateAdapter = true)
data class AnimeMovieDetail(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "image_poster") val image_poster: String? = null,
    @Json(name = "image_cover") val image_cover: String? = null,
    @Json(name = "image") val rawImage: String? = null,
    @Json(name = "score") val score: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "genre") val genre: String? = null,
    @Json(name = "release_date") val release_date: String? = null,
    @Json(name = "year") val year: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "views") val views: String? = null,
    @Json(name = "studio") val studio: String? = null
) {
    val image: String
        get() = formatImageUrl(image_poster ?: image_cover ?: rawImage)

    val displayScore: String
        get() {
            if (!score.isNullOrBlank()) return score
            val v = views?.toLongOrNull() ?: 1000000L
            val scoreVal = 8.2 + ((v.coerceIn(100000L, 16000000L) - 100000L) / 15900000.0) * 1.5
            return String.format(java.util.Locale.US, "%.1f", scoreVal)
        }
}

@JsonClass(generateAdapter = true)
data class AnimeSeasonItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class AnimeEpisodeItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "index") val index: Any? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "image") val rawImage: String? = null,
    @Json(name = "key_time") val key_time: String? = null,
    @Json(name = "views") val views: String? = null
) {
    val displayIndex: String
        get() = index?.toString() ?: "1"

    val image: String
        get() = formatImageUrl(rawImage)
}

@JsonClass(generateAdapter = true)
data class StreamNewData(
    @Json(name = "server") val server: List<StreamDownloadItem>? = null,
    @Json(name = "episode") val episode: StreamEpisodeDetail? = null
)

@JsonClass(generateAdapter = true)
data class StreamEpisodeDetail(
    @Json(name = "id") val id: String? = null,
    @Json(name = "index") val index: Any? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "image") val image: String? = null
)

@JsonClass(generateAdapter = true)
data class StreamResponse(
    @Json(name = "title") val title: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "download") val download: List<StreamDownloadItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class StreamDownloadItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "link") val link: String = "",
    @Json(name = "quality") val quality: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "type") val type: String? = null
)

fun formatImageUrl(imgUrl: String?): String {
    if (imgUrl.isNullOrBlank()) return ""
    var url = imgUrl.trim()
    url = when {
        url.startsWith("//") -> "https:$url"
        url.startsWith("http://") || url.startsWith("https://") -> url
        url.startsWith("/") -> "https://xyz-api.animein.net$url"
        else -> "https://xyz-api.animein.net/$url"
    }
    if (url.contains("crunchyroll.com") && !url.endsWith(".jpg")) {
        url += "_full.jpg"
    }
    return url
}
