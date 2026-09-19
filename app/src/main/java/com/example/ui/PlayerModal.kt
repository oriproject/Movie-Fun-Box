package com.example.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AccessManager
import com.example.data.MovieDetail
import com.example.data.StreamServer
import kotlinx.coroutines.delay
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentRed
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerModal(
    movie: MovieDetail,
    viewModel: MovieViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val activeStreamUrl by viewModel.activeStreamUrl.collectAsState()
    val showTrailer by viewModel.showTrailerPlayer.collectAsState()
    val selectedSeason by viewModel.selectedSeason.collectAsState()
    val selectedEpisode by viewModel.selectedEpisode.collectAsState()
    val episodes by viewModel.episodes.collectAsState()

    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var seasonDropdownExpanded by remember { mutableStateOf(false) }
    var episodeDropdownExpanded by remember { mutableStateOf(false) }

    val exitFullscreen = {
        try {
            customViewCallback?.onCustomViewHidden()
        } catch (e: Exception) {}
        customFullscreenView = null
        customViewCallback = null
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        activity?.window?.let { win ->
            WindowInsetsControllerCompat(win, win.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isFullscreenMode = isLandscape || customFullscreenView != null

    LaunchedEffect(isFullscreenMode) {
        activity?.window?.let { win ->
            val controller = WindowInsetsControllerCompat(win, win.decorView)
            if (isFullscreenMode) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                customViewCallback?.onCustomViewHidden()
            } catch (e: Exception) {}
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            activity?.window?.let { win ->
                WindowInsetsControllerCompat(win, win.decorView).show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (isFullscreenMode) {
        BackHandler {
            if (customFullscreenView != null) {
                exitFullscreen()
            } else {
                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }

        var showLandscapeControls by remember { mutableStateOf(true) }
        LaunchedEffect(showLandscapeControls) {
            if (showLandscapeControls) {
                delay(4000L)
                showLandscapeControls = false
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showLandscapeControls = !showLandscapeControls
                }
        ) {
            if (customFullscreenView != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        (customFullscreenView?.parent as? ViewGroup)?.removeView(customFullscreenView)
                        customFullscreenView!!
                    }
                )
            } else {
                when {
                    activeStreamUrl != null -> {
                        EmbeddedWebView(
                            url = activeStreamUrl!!,
                            modifier = Modifier.fillMaxSize(),
                            onEnterFullscreen = { view, callback ->
                                customFullscreenView = view
                                customViewCallback = callback
                            },
                            onExitFullscreen = {
                                exitFullscreen()
                            }
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { viewModel.playMovie() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (movie.fullBackdropUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = movie.fullBackdropUrl,
                                    contentDescription = movie.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (movie.fullPosterUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = movie.fullPosterUrl,
                                    contentDescription = movie.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.55f))
                            )
                            Button(
                                onClick = { viewModel.playMovie() },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play Movie", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Floating Top Bar in Landscape
            AnimatedVisibility(
                visible = showLandscapeControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable {
                                    if (customFullscreenView != null) {
                                        exitFullscreen()
                                    } else {
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = movie.title,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (movie.mediaType == "tv" && selectedSeason != null && selectedEpisode != null) {
                                Text(
                                    text = "Season $selectedSeason Episode $selectedEpisode",
                                    color = NeonGreen,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Exit Fullscreen / Switch to Portrait
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable {
                                if (customFullscreenView != null) {
                                    exitFullscreen()
                                } else {
                                    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_fullscreen_exit),
                            contentDescription = "Exit Fullscreen",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    } else {
        BackHandler {
            onDismiss()
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Video Player / Preview Area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            // 1. Active Streaming Embed
                            activeStreamUrl != null -> {
                                EmbeddedWebView(
                                    url = activeStreamUrl!!,
                                    modifier = Modifier.fillMaxSize(),
                                    onEnterFullscreen = { view, callback ->
                                        customFullscreenView = view
                                        customViewCallback = callback
                                        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                    },
                                    onExitFullscreen = {
                                        exitFullscreen()
                                    }
                                )

                                // Fullscreen button on player overlay
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.70f))
                                        .clickable {
                                            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_fullscreen),
                                        contentDescription = "Fullscreen",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            // 2. Movie Poster / Click to Play
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clickable { viewModel.playMovie() }
                                        .testTag("player_poster_click_to_play"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (movie.fullBackdropUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = movie.fullBackdropUrl,
                                            contentDescription = movie.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (movie.fullPosterUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = movie.fullPosterUrl,
                                            contentDescription = movie.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Gradient overlay
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(
                                                        Color.Black.copy(alpha = 0.35f),
                                                        Color.Black.copy(alpha = 0.75f)
                                                    )
                                                )
                                            )
                                    )

                                    // Big Play Button
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(NeonGreen),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Play",
                                                tint = Color.Black,
                                                modifier = Modifier.size(38.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (movie.mediaType == "tv") "Click to Play Episode $selectedEpisode" else "Click to Play Movie",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Content Details Section
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Title and Badges
                        Text(
                            text = movie.title,
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = NeonGreen,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "IMDB ${movie.formattedRating}",
                                        color = Color.Black,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                color = DarkSurfaceVariant,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = movie.year,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                color = AccentBlue.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = if (movie.mediaType == "tv") "TV SERIES" else "MOVIE",
                                    color = AccentBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Main Direct Play Button
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { viewModel.playMovie() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("btn_play_movie_direct")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (movie.mediaType == "tv") "Play Episode $selectedEpisode Now" else "Play Movie Now",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        // TV Series Season & Episode Selector
                        if (movie.mediaType == "tv") {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "SELECT SEASON & EPISODE",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Season Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = seasonDropdownExpanded,
                                    onExpandedChange = { seasonDropdownExpanded = !seasonDropdownExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = "Season $selectedSeason",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seasonDropdownExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkCard,
                                            unfocusedContainerColor = DarkCard,
                                            focusedBorderColor = NeonGreen,
                                            unfocusedBorderColor = DarkBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = seasonDropdownExpanded,
                                        onDismissRequest = { seasonDropdownExpanded = false },
                                        modifier = Modifier.background(DarkCard)
                                    ) {
                                        for (s in 1..movie.numberOfSeasons) {
                                            DropdownMenuItem(
                                                text = { Text("Season $s", color = TextPrimary) },
                                                onClick = {
                                                    viewModel.selectSeason(s)
                                                    seasonDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Episode Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = episodeDropdownExpanded,
                                    onExpandedChange = { episodeDropdownExpanded = !episodeDropdownExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = "Ep $selectedEpisode",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = episodeDropdownExpanded) },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = DarkCard,
                                            unfocusedContainerColor = DarkCard,
                                            focusedBorderColor = NeonGreen,
                                            unfocusedBorderColor = DarkBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = episodeDropdownExpanded,
                                        onDismissRequest = { episodeDropdownExpanded = false },
                                        modifier = Modifier.background(DarkCard)
                                    ) {
                                        if (episodes.isEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text("Loading episodes...", color = TextSecondary) },
                                                onClick = { episodeDropdownExpanded = false }
                                            )
                                        } else {
                                            episodes.forEach { ep ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = "Ep ${ep.episodeNumber}: ${ep.name}",
                                                            color = TextPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    },
                                                    onClick = {
                                                        viewModel.selectEpisode(ep.episodeNumber)
                                                        episodeDropdownExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }



                        // Telegram Link Card
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AccentBlue.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Buffering or Link Broken?",
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Get Direct 4K Files on Telegram",
                                        color = AccentBlue,
                                        fontSize = 11.sp
                                    )
                                }
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AccessManager.TELEGRAM_URL))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Join Group",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Synopsis / Plot
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "STORYLINE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = movie.overview.ifEmpty { "No storyline available." },
                            color = TextPrimary.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )

                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }

                // Top Floating Close Button (Made smaller and compact)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable(onClick = onDismiss)
                        .testTag("close_player_modal"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbeddedWebView(
    url: String,
    modifier: Modifier = Modifier,
    onEnterFullscreen: ((View, WebChromeClient.CustomViewCallback) -> Unit)? = null,
    onExitFullscreen: (() -> Unit)? = null
) {
    var isPageLoading by remember(url) { mutableStateOf(true) }
    val initialHost = remember(url) {
        try {
            Uri.parse(url).host?.lowercase().orEmpty()
        } catch (e: Exception) {
            ""
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                    setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        mediaPlaybackRequiresUserGesture = false
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        javaScriptCanOpenWindowsAutomatically = false
                        setSupportMultipleWindows(false)
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                    }

                    val injectAntiAdJs = { view: WebView? ->
                        val script = """
                            (function() {
                                window.open = function() { return null; };
                                window.alert = function() {};
                                window.confirm = function() {};
                                window.prompt = function() {};
                                try {
                                    Object.defineProperty(window, 'open', {
                                        value: function() { return null; },
                                        writable: false
                                    });
                                } catch(e) {}
                                try {
                                    var style = document.getElementById('fit-player-style');
                                    if (!style) {
                                        style = document.createElement('style');
                                        style.id = 'fit-player-style';
                                        style.innerHTML = 'html, body { width: 100% !important; height: 100% !important; margin: 0 !important; padding: 0 !important; overflow: hidden !important; background: #000 !important; } iframe, video, #player, .player { width: 100% !important; height: 100% !important; border: 0 !important; }';
                                        if (document.head) {
                                            document.head.appendChild(style);
                                        } else if (document.documentElement) {
                                            document.documentElement.appendChild(style);
                                        }
                                    }
                                } catch(e) {}
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(script, null)
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, pageUrl, favicon)
                            view?.setBackgroundColor(android.graphics.Color.BLACK)
                            injectAntiAdJs(view)
                        }

                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                            super.onPageFinished(view, pageUrl)
                            view?.setBackgroundColor(android.graphics.Color.BLACK)
                            injectAntiAdJs(view)
                            isPageLoading = false
                        }

                        override fun onReceivedSslError(
                            view: WebView?,
                            handler: android.webkit.SslErrorHandler?,
                            error: android.net.http.SslError?
                        ) {
                            handler?.proceed()
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): android.webkit.WebResourceResponse? {
                            val reqUrl = request?.url?.toString()?.lowercase() ?: return null
                            val blockedKeywords = listOf(
                                "popads", "popunder", "smartlink", "histats", "propeller",
                                "clickadu", "monetag", "adsterra", "hilltopads", "exoclick",
                                "doubleclick", "googlesyndication", "adservice", "adsystem",
                                "trafficjunky", "syndication", "bet365", "1xbet", "parimatch",
                                "onclick", "banner", "tracking", "adnxs", "ad-delivery",
                                "adrun", "adskeeper", "yandex.ru", "adcolony", "inmobi", "taboola", "outbrain"
                            )
                            if (blockedKeywords.any { reqUrl.contains(it) }) {
                                return android.webkit.WebResourceResponse(
                                    "text/plain",
                                    "UTF-8",
                                    java.io.ByteArrayInputStream(ByteArray(0))
                                )
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): Boolean {
                            val reqUri = request?.url ?: return true
                            val reqHost = reqUri.host?.lowercase().orEmpty()
                            val scheme = reqUri.scheme?.lowercase().orEmpty()

                            // Block intent://, market://, etc.
                            if (scheme != "http" && scheme != "https") {
                                return true
                            }

                            // Strictly allow only the authorized stream provider domain
                            if (reqHost.isNotEmpty() && (
                                reqHost == initialHost ||
                                reqHost.endsWith(".$initialHost") ||
                                reqHost.contains("vidsrc") ||
                                reqHost.contains("2embed") ||
                                reqHost.contains("multiembed")
                            )) {
                                return false
                            }

                            // BLOCK ALL external ad sites, YouTube, smartlinks, and redirect hijackers
                            return true
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                            if (view != null && callback != null) {
                                onEnterFullscreen?.invoke(view, callback)
                            }
                        }

                        override fun onHideCustomView() {
                            onExitFullscreen?.invoke()
                        }

                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            if (newProgress >= 70) {
                                isPageLoading = false
                            }
                        }
                    }

                    loadUrl(url)
                }
            },
            update = { webView ->
                if (webView.url != url) {
                    isPageLoading = true
                    webView.setBackgroundColor(android.graphics.Color.BLACK)
                    webView.loadUrl(url)
                }
            }
        )

        // Sleek dark buffering overlay to completely eliminate any white screen flash
        AnimatedVisibility(
            visible = isPageLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = NeonGreen,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Buffering Stream...",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}
