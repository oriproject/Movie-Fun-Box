package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccessManager
import com.example.data.CategoriesData
import com.example.data.Category
import com.example.data.MovieDetail
import com.example.data.MovieItem
import com.example.data.MovieRepository
import com.example.data.ServerData
import com.example.data.StreamServer
import com.example.data.TvEpisode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    val accessManager = AccessManager(application.applicationContext)
    private val repository = MovieRepository()

    // 12-Hour Access state
    val isUnlocked: StateFlow<Boolean> = accessManager.isUnlocked
    val remainingTimeMillis: StateFlow<Long> = accessManager.remainingTimeMillis

    // Adsterra Smart Link Verification state
    private val _isAdVerificationActive = MutableStateFlow(false)
    val isAdVerificationActive: StateFlow<Boolean> = _isAdVerificationActive.asStateFlow()

    private val _adVerificationCountdown = MutableStateFlow(AccessManager.AD_TIMER_SECONDS)
    val adVerificationCountdown: StateFlow<Int> = _adVerificationCountdown.asStateFlow()

    private val _hasAttemptedAdClick = MutableStateFlow(false)
    val hasAttemptedAdClick: StateFlow<Boolean> = _hasAttemptedAdClick.asStateFlow()

    private val _isReturnedEarly = MutableStateFlow(false)
    val isReturnedEarly: StateFlow<Boolean> = _isReturnedEarly.asStateFlow()

    private val _earlyReturnRemaining = MutableStateFlow(0)
    val earlyReturnRemaining: StateFlow<Int> = _earlyReturnRemaining.asStateFlow()

    private var adStartTimeMs: Long = 0L
    private var adTimerJob: Job? = null
    private var sessionMonitorJob: Job? = null

    // Categories (10 in total)
    val categories: List<Category> = CategoriesData.top10Categories
    private val _selectedCategory = MutableStateFlow(categories.first())
    val selectedCategory: StateFlow<Category> = _selectedCategory.asStateFlow()

    // Movie list & loading
    private val _movieList = MutableStateFlow<List<MovieItem>>(emptyList())
    val movieList: StateFlow<List<MovieItem>> = _movieList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var currentPage = 1
    private var isLastPage = false

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    private var searchJob: Job? = null

    // Details & Streaming Player Modal
    private val _selectedMovie = MutableStateFlow<MovieDetail?>(null)
    val selectedMovie: StateFlow<MovieDetail?> = _selectedMovie.asStateFlow()

    private val _isDetailLoading = MutableStateFlow(false)
    val isDetailLoading: StateFlow<Boolean> = _isDetailLoading.asStateFlow()

    private val _selectedSeason = MutableStateFlow(1)
    val selectedSeason: StateFlow<Int> = _selectedSeason.asStateFlow()

    private val _episodes = MutableStateFlow<List<TvEpisode>>(emptyList())
    val episodes: StateFlow<List<TvEpisode>> = _episodes.asStateFlow()

    private val _selectedEpisode = MutableStateFlow(1)
    val selectedEpisode: StateFlow<Int> = _selectedEpisode.asStateFlow()

    val servers: List<StreamServer> = ServerData.servers
    private val _selectedServer = MutableStateFlow(servers.first())
    val selectedServer: StateFlow<StreamServer> = _selectedServer.asStateFlow()

    // Stream connecting countdown (15s secure server handshake like template)
    private val _isServerCountdownActive = MutableStateFlow(false)
    val isServerCountdownActive: StateFlow<Boolean> = _isServerCountdownActive.asStateFlow()

    private val _serverCountdown = MutableStateFlow(15)
    val serverCountdown: StateFlow<Int> = _serverCountdown.asStateFlow()

    private val _activeStreamUrl = MutableStateFlow<String?>(null)
    val activeStreamUrl: StateFlow<String?> = _activeStreamUrl.asStateFlow()

    private val _showTrailerPlayer = MutableStateFlow(false)
    val showTrailerPlayer: StateFlow<Boolean> = _showTrailerPlayer.asStateFlow()

    private var serverTimerJob: Job? = null

    init {
        startSessionMonitor()
        loadCategory(categories.first())
    }

    private fun startSessionMonitor() {
        sessionMonitorJob?.cancel()
        sessionMonitorJob = viewModelScope.launch {
            while (true) {
                accessManager.updateStatus()
                delay(1000L)
            }
        }
    }

    fun startAdVerification() {
        _hasAttemptedAdClick.value = true
        _isReturnedEarly.value = false
        _isAdVerificationActive.value = true
        _adVerificationCountdown.value = AccessManager.AD_TIMER_SECONDS
        adStartTimeMs = System.currentTimeMillis()

        adTimerJob?.cancel()
        adTimerJob = viewModelScope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - adStartTimeMs
                val requiredMs = AccessManager.AD_TIMER_SECONDS * 1000L
                val remaining = ((requiredMs - elapsed) / 1000L).toInt()
                if (remaining <= 0) {
                    _adVerificationCountdown.value = 0
                    completeAdUnlock()
                    break
                } else {
                    _adVerificationCountdown.value = remaining
                }
                delay(500L)
            }
        }
    }

    fun handleAppResume() {
        accessManager.updateStatus()
        if (accessManager.checkAccessValid()) return

        if (_hasAttemptedAdClick.value && adStartTimeMs > 0L) {
            val elapsed = System.currentTimeMillis() - adStartTimeMs
            val requiredMs = AccessManager.AD_TIMER_SECONDS * 1000L
            if (elapsed >= requiredMs) {
                // Completed full 20 seconds! Automatically unlock and redirect inside
                completeAdUnlock()
            } else {
                // Returned early before completing 20 seconds!
                val remainingSec = ((requiredMs - elapsed) / 1000L).toInt().coerceAtLeast(1)
                _isReturnedEarly.value = true
                _earlyReturnRemaining.value = remainingSec
            }
        }
    }

    fun completeAdUnlock() {
        adTimerJob?.cancel()
        accessManager.unlockSession()
        _isAdVerificationActive.value = false
        _hasAttemptedAdClick.value = false
        _isReturnedEarly.value = false
        _earlyReturnRemaining.value = 0
    }

    fun selectCategory(category: Category) {
        if (_selectedCategory.value.id == category.id && _searchQuery.value.isEmpty()) return
        _selectedCategory.value = category
        _searchQuery.value = ""
        currentPage = 1
        isLastPage = false
        _movieList.value = emptyList()
        loadCategory(category)
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.trim().length >= 2) {
            searchJob = viewModelScope.launch {
                delay(500) // Debounce
                performSearch(query.trim())
            }
        } else if (query.isEmpty()) {
            loadCategory(_selectedCategory.value)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            currentPage = 1
            val result = repository.searchContent(query, currentPage)
            result.onSuccess { items ->
                _movieList.value = items
                isLastPage = items.isEmpty()
            }.onFailure { err ->
                _errorMessage.value = err.message
            }
            _isLoading.value = false
        }
    }

    fun loadMore() {
        if (_isLoading.value || isLastPage) return
        currentPage++
        val q = _searchQuery.value
        if (q.isNotBlank()) {
            viewModelScope.launch {
                _isLoading.value = true
                val result = repository.searchContent(q, currentPage)
                result.onSuccess { items ->
                    if (items.isEmpty()) isLastPage = true
                    else _movieList.value = _movieList.value + items
                }
                _isLoading.value = false
            }
        } else {
            loadCategory(_selectedCategory.value, isLoadMore = true)
        }
    }

    private fun loadCategory(category: Category, isLoadMore: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = repository.getCategoryContent(category, currentPage)
            result.onSuccess { items ->
                if (items.isEmpty()) {
                    isLastPage = true
                } else {
                    if (isLoadMore) {
                        _movieList.value = _movieList.value + items
                    } else {
                        _movieList.value = items
                    }
                }
            }.onFailure { err ->
                _errorMessage.value = err.message
            }
            _isLoading.value = false
        }
    }

    fun openMovieDetail(item: MovieItem) {
        _isDetailLoading.value = true
        _selectedMovie.value = null
        _activeStreamUrl.value = null
        _showTrailerPlayer.value = false
        _isServerCountdownActive.value = false
        _selectedSeason.value = 1
        _selectedEpisode.value = 1
        _episodes.value = emptyList()
        _selectedServer.value = servers.first()

        viewModelScope.launch {
            val result = repository.getDetails(item.id, item.mediaType)
            result.onSuccess { detail ->
                _selectedMovie.value = detail
                if (detail.mediaType == "tv") {
                    loadEpisodes(detail.id, 1)
                }
            }
            _isDetailLoading.value = false
        }
    }

    fun closeMovieDetail() {
        serverTimerJob?.cancel()
        _selectedMovie.value = null
        _activeStreamUrl.value = null
        _showTrailerPlayer.value = false
        _isServerCountdownActive.value = false
    }

    fun selectSeason(seasonNumber: Int) {
        _selectedSeason.value = seasonNumber
        _selectedEpisode.value = 1
        _activeStreamUrl.value = null
        _showTrailerPlayer.value = false
        _selectedMovie.value?.let { detail ->
            loadEpisodes(detail.id, seasonNumber)
        }
    }

    private fun loadEpisodes(tvId: Long, seasonNumber: Int) {
        viewModelScope.launch {
            val result = repository.getTvEpisodes(tvId, seasonNumber)
            result.onSuccess { list ->
                _episodes.value = list
            }
        }
    }

    fun playMovie() {
        val movie = _selectedMovie.value ?: return
        serverTimerJob?.cancel()
        _showTrailerPlayer.value = false
        _isServerCountdownActive.value = false
        _activeStreamUrl.value = buildStreamUrl(
            movie.id,
            movie.mediaType,
            _selectedSeason.value,
            _selectedEpisode.value
        )
    }

    fun buildStreamUrl(id: Long, mediaType: String, season: Int, episode: Int): String {
        return if (mediaType == "tv") {
            "https://vidsrc.to/embed/tv/$id/$season/$episode"
        } else {
            "https://vidsrc.to/embed/movie/$id"
        }
    }

    fun selectEpisode(episodeNumber: Int) {
        _selectedEpisode.value = episodeNumber
        _showTrailerPlayer.value = false
        _isServerCountdownActive.value = false
        val movie = _selectedMovie.value ?: return
        _activeStreamUrl.value = buildStreamUrl(movie.id, movie.mediaType, _selectedSeason.value, episodeNumber)
    }

    fun selectServer(server: StreamServer) {
        _selectedServer.value = server
        playMovie()
    }

    fun startStreamCountdown() {
        playMovie()
    }

    fun playTrailer() {
        val movie = _selectedMovie.value ?: return
        if (!movie.trailerYoutubeKey.isNullOrBlank()) {
            serverTimerJob?.cancel()
            _isServerCountdownActive.value = false
            _activeStreamUrl.value = null
            _showTrailerPlayer.value = true
        }
    }
}
