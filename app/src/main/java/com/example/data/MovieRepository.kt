package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MovieRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        const val TMDB_KEY = "76049db53639d206ed172eaaa9cccdd3"
        const val TMDB_BASE = "https://api.themoviedb.org/3"
    }

    suspend fun getCategoryContent(category: Category, page: Int = 1): Result<List<MovieItem>> =
        withContext(Dispatchers.IO) {
            try {
                val url = if (category.endpoint.contains("?")) {
                    "$TMDB_BASE/${category.endpoint}&api_key=$TMDB_KEY&page=$page"
                } else {
                    "$TMDB_BASE/${category.endpoint}?api_key=$TMDB_KEY&page=$page"
                }

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results") ?: return@withContext Result.success(emptyList())
                    val items = mutableListOf<MovieItem>()

                    for (i in 0 until results.length()) {
                        val obj = results.optJSONObject(i) ?: continue
                        val posterPath = obj.optString("poster_path", "").takeIf { it.isNotEmpty() && it != "null" }
                        if (posterPath == null) continue // Skip items without poster

                        val id = obj.optLong("id")
                        val title = obj.optString("title").ifEmpty { obj.optString("name", "Untitled") }
                        val backdropPath = obj.optString("backdrop_path", "").takeIf { it.isNotEmpty() && it != "null" }
                        val rating = obj.optDouble("vote_average", 0.0)
                        val releaseDate = obj.optString("release_date").ifEmpty { obj.optString("first_air_date", "") }
                        val mediaType = obj.optString("media_type").ifEmpty { category.defaultMediaType }
                        val overview = obj.optString("overview", "")

                        items.add(
                            MovieItem(
                                id = id,
                                title = title,
                                posterPath = posterPath,
                                backdropPath = backdropPath,
                                rating = rating,
                                releaseDate = releaseDate,
                                mediaType = if (mediaType == "tv") "tv" else "movie",
                                overview = overview
                            )
                        )
                    }
                    Result.success(items)
                } else {
                    Result.failure(Exception("HTTP ${response.code}: ${response.message}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun searchContent(query: String, page: Int = 1): Result<List<MovieItem>> =
        withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val url = "$TMDB_BASE/search/multi?api_key=$TMDB_KEY&query=$encoded&page=$page"

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val json = JSONObject(body)
                    val results = json.optJSONArray("results") ?: return@withContext Result.success(emptyList())
                    val items = mutableListOf<MovieItem>()

                    for (i in 0 until results.length()) {
                        val obj = results.optJSONObject(i) ?: continue
                        val mediaType = obj.optString("media_type")
                        if (mediaType != "movie" && mediaType != "tv") continue

                        val posterPath = obj.optString("poster_path", "").takeIf { it.isNotEmpty() && it != "null" }
                        if (posterPath == null) continue

                        val id = obj.optLong("id")
                        val title = obj.optString("title").ifEmpty { obj.optString("name", "Untitled") }
                        val backdropPath = obj.optString("backdrop_path", "").takeIf { it.isNotEmpty() && it != "null" }
                        val rating = obj.optDouble("vote_average", 0.0)
                        val releaseDate = obj.optString("release_date").ifEmpty { obj.optString("first_air_date", "") }
                        val overview = obj.optString("overview", "")

                        items.add(
                            MovieItem(
                                id = id,
                                title = title,
                                posterPath = posterPath,
                                backdropPath = backdropPath,
                                rating = rating,
                                releaseDate = releaseDate,
                                mediaType = mediaType,
                                overview = overview
                            )
                        )
                    }
                    Result.success(items)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getDetails(id: Long, mediaType: String): Result<MovieDetail> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$TMDB_BASE/$mediaType/$id?api_key=$TMDB_KEY&append_to_response=videos"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val obj = JSONObject(body)
                    val title = obj.optString("title").ifEmpty { obj.optString("name", "Untitled") }
                    val posterPath = obj.optString("poster_path", "").takeIf { it.isNotEmpty() && it != "null" }
                    val backdropPath = obj.optString("backdrop_path", "").takeIf { it.isNotEmpty() && it != "null" }
                    val rating = obj.optDouble("vote_average", 0.0)
                    val releaseDate = obj.optString("release_date").ifEmpty { obj.optString("first_air_date", "") }
                    val overview = obj.optString("overview", "No synopsis available.")
                    val numberOfSeasons = obj.optInt("number_of_seasons", 1)

                    var trailerKey: String? = null
                    val videosObj = obj.optJSONObject("videos")
                    if (videosObj != null) {
                        val videoResults = videosObj.optJSONArray("results")
                        if (videoResults != null) {
                            for (i in 0 until videoResults.length()) {
                                val v = videoResults.optJSONObject(i) ?: continue
                                val site = v.optString("site")
                                val type = v.optString("type")
                                if (site.equals("YouTube", ignoreCase = true) &&
                                    (type.equals("Trailer", ignoreCase = true) || type.equals("Teaser", ignoreCase = true))
                                ) {
                                    trailerKey = v.optString("key")
                                    break
                                }
                            }
                        }
                    }

                    Result.success(
                        MovieDetail(
                            id = id,
                            title = title,
                            posterPath = posterPath,
                            backdropPath = backdropPath,
                            rating = rating,
                            releaseDate = releaseDate,
                            overview = overview,
                            mediaType = mediaType,
                            numberOfSeasons = numberOfSeasons,
                            trailerYoutubeKey = trailerKey
                        )
                    )
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getTvEpisodes(tvId: Long, seasonNumber: Int): Result<List<TvEpisode>> =
        withContext(Dispatchers.IO) {
            try {
                val url = "$TMDB_BASE/tv/$tvId/season/$seasonNumber?api_key=$TMDB_KEY"
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val json = JSONObject(body)
                    val episodesArray = json.optJSONArray("episodes") ?: return@withContext Result.success(emptyList())
                    val episodes = mutableListOf<TvEpisode>()

                    for (i in 0 until episodesArray.length()) {
                        val ep = episodesArray.optJSONObject(i) ?: continue
                        val epNum = ep.optInt("episode_number", i + 1)
                        val epName = ep.optString("name", "Episode $epNum")
                        val epOverview = ep.optString("overview", "").takeIf { it.isNotBlank() }
                        val stillPath = ep.optString("still_path", "").takeIf { it.isNotBlank() }

                        episodes.add(
                            TvEpisode(
                                episodeNumber = epNum,
                                name = epName,
                                overview = epOverview,
                                stillPath = stillPath
                            )
                        )
                    }
                    Result.success(episodes)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
