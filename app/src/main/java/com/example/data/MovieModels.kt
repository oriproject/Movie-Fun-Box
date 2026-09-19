package com.example.data

data class MovieItem(
    val id: Long,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val rating: Double,
    val releaseDate: String?,
    val mediaType: String, // "movie" or "tv"
    val overview: String
) {
    val fullPosterUrl: String
        get() = if (!posterPath.isNullOrBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""

    val fullBackdropUrl: String
        get() = if (!backdropPath.isNullOrBlank()) "https://image.tmdb.org/t/p/w780$backdropPath" else ""

    val year: String
        get() = releaseDate?.split("-")?.firstOrNull() ?: "N/A"

    val formattedRating: String
        get() = if (rating > 0) String.format("%.1f", rating) else "N/A"
}

data class MovieDetail(
    val id: Long,
    val title: String,
    val posterPath: String?,
    val backdropPath: String?,
    val rating: Double,
    val releaseDate: String?,
    val overview: String,
    val mediaType: String,
    val numberOfSeasons: Int = 1,
    val trailerYoutubeKey: String? = null
) {
    val fullPosterUrl: String
        get() = if (!posterPath.isNullOrBlank()) "https://image.tmdb.org/t/p/w500$posterPath" else ""

    val fullBackdropUrl: String
        get() = if (!backdropPath.isNullOrBlank()) "https://image.tmdb.org/t/p/w780$backdropPath" else ""

    val year: String
        get() = releaseDate?.split("-")?.firstOrNull() ?: "N/A"

    val formattedRating: String
        get() = if (rating > 0) String.format("%.1f", rating) else "N/A"
}

data class TvEpisode(
    val episodeNumber: Int,
    val name: String,
    val overview: String?,
    val stillPath: String?
)

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val defaultMediaType: String,
    val endpoint: String
)

object CategoriesData {
    // 4 Existing Categories + 6 New Categories = Top 10 Categories
    val top10Categories = listOf(
        Category(
            id = "trending",
            name = "Trending Now",
            icon = "🔥",
            defaultMediaType = "movie",
            endpoint = "trending/all/day"
        ),
        Category(
            id = "popular_movies",
            name = "Popular Movies",
            icon = "🎬",
            defaultMediaType = "movie",
            endpoint = "movie/popular"
        ),
        Category(
            id = "web_series",
            name = "Web Series",
            icon = "📺",
            defaultMediaType = "tv",
            endpoint = "tv/popular"
        ),
        Category(
            id = "anime",
            name = "Anime Series",
            icon = "⚡",
            defaultMediaType = "tv",
            endpoint = "discover/tv?with_genres=16&sort_by=popularity.desc"
        ),
        // 6 Added Categories:
        Category(
            id = "kdrama",
            name = "K-Drama",
            icon = "💖",
            defaultMediaType = "tv",
            endpoint = "discover/tv?with_original_language=ko&sort_by=popularity.desc"
        ),
        Category(
            id = "top_rated",
            name = "Top Rated",
            icon = "⭐",
            defaultMediaType = "movie",
            endpoint = "movie/top_rated"
        ),
        Category(
            id = "action",
            name = "Action & Adventure",
            icon = "💥",
            defaultMediaType = "movie",
            endpoint = "discover/movie?with_genres=28,12&sort_by=popularity.desc"
        ),
        Category(
            id = "scifi",
            name = "Sci-Fi & Fantasy",
            icon = "🚀",
            defaultMediaType = "movie",
            endpoint = "discover/movie?with_genres=878,14&sort_by=popularity.desc"
        ),
        Category(
            id = "horror",
            name = "Horror & Thriller",
            icon = "👻",
            defaultMediaType = "movie",
            endpoint = "discover/movie?with_genres=27,53&sort_by=popularity.desc"
        ),
        Category(
            id = "bollywood",
            name = "Bollywood Hits",
            icon = "🌟",
            defaultMediaType = "movie",
            endpoint = "discover/movie?with_original_language=hi&sort_by=popularity.desc"
        )
    )
}

data class StreamServer(
    val id: Int,
    val name: String,
    val tag: String,
    val icon: String,
    val urlBuilder: (id: Long, mediaType: String, season: Int, episode: Int) -> String
)

object ServerData {
    val servers = listOf(
        StreamServer(
            id = 1,
            name = "Server 1",
            tag = "Fast",
            icon = "🚀",
            urlBuilder = { id, type, s, e ->
                if (type == "tv") "https://vidsrc.to/embed/tv/$id/$s/$e"
                else "https://vidsrc.to/embed/movie/$id"
            }
        ),
        StreamServer(
            id = 2,
            name = "Server 2",
            tag = "Auto",
            icon = "⚡",
            urlBuilder = { id, type, s, e ->
                if (type == "tv") "https://vidsrc.me/embed/tv?tmdb=$id&season=$s&episode=$e"
                else "https://vidsrc.me/embed/movie?tmdb=$id"
            }
        ),
        StreamServer(
            id = 3,
            name = "Server 3",
            tag = "HD",
            icon = "🎬",
            urlBuilder = { id, type, s, e ->
                if (type == "tv") "https://multiembed.mov/directstream.php?video_id=$id&tmdb=1&s=$s&e=$e"
                else "https://multiembed.mov/directstream.php?video_id=$id&tmdb=1"
            }
        ),
        StreamServer(
            id = 4,
            name = "Server 4",
            tag = "Backup",
            icon = "🛡️",
            urlBuilder = { id, type, s, e ->
                if (type == "tv") "https://2embed.org/embed/tv/$id/$s/$e"
                else "https://2embed.org/embed/movie/$id"
            }
        )
    )
}
