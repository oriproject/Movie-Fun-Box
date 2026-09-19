package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.AccessManager
import com.example.data.Category
import com.example.data.MovieItem
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AccentYellow
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MainMovieDashboard(
    viewModel: MovieViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val categories = viewModel.categories
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val movieList by viewModel.movieList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedMovie by viewModel.selectedMovie.collectAsState()
    val isDetailLoading by viewModel.isDetailLoading.collectAsState()

    var isSearchVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            DashboardTopBar(
                isSearchActive = isSearchVisible,
                onSearchToggle = {
                    isSearchVisible = !isSearchVisible
                    if (!isSearchVisible) {
                        viewModel.onSearchQueryChanged("")
                    }
                },
                onTelegramClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AccessManager.TELEGRAM_URL))
                    context.startActivity(intent)
                }
            )

            // Search Bar Input (Collapsible)
            AnimatedVisibility(visible = isSearchVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search movies, web series, anime...", color = TextSecondary, fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = NeonGreen, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface,
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("search_text_input")
                    )
                }
            }

            // Category Horizontal Swipeable Row (Top 10 Categories)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(16.dp)
                                .background(NeonGreen, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Search Results: $searchQuery" else selectedCategory.name,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Swipe for 10 Categories →",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Horizontal scrollable categories row (swipable left/right)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("categories_horizontal_list"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        CategoryChip(
                            category = cat,
                            isSelected = selectedCategory.id == cat.id && searchQuery.isEmpty(),
                            onSelect = {
                                if (isSearchVisible) isSearchVisible = false
                                viewModel.selectCategory(cat)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Movie & Web Series Grid
            Box(modifier = Modifier.weight(1f)) {
                if (isLoading && movieList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = NeonGreen)
                    }
                } else if (errorMessage != null && movieList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Failed to load content",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMessage ?: "Unknown error",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.selectCategory(selectedCategory) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("movies_vertical_grid")
                    ) {
                        items(movieList, key = { "${it.mediaType}_${it.id}" }) { movie ->
                            MovieCardItem(
                                movie = movie,
                                onClick = { viewModel.openMovieDetail(movie) }
                            )
                        }

                        // Bottom Load More Button
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = NeonGreen,
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                } else {
                                    Button(
                                        onClick = { viewModel.loadMore() },
                                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                                        shape = RoundedCornerShape(24.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                                        modifier = Modifier
                                            .height(44.dp)
                                            .testTag("load_more_button")
                                    ) {
                                        Text(
                                            text = "Load More Content",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Full Screen Loading Indicator when opening details
        if (isDetailLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NeonGreen)
            }
        }

        // Selected Movie / Player Modal
        selectedMovie?.let { detail ->
            PlayerModal(
                movie = detail,
                viewModel = viewModel,
                onDismiss = { viewModel.closeMovieDetail() }
            )
        }
    }
}

@Composable
private fun DashboardTopBar(
    isSearchActive: Boolean,
    onSearchToggle: () -> Unit,
    onTelegramClick: () -> Unit
) {
    Surface(
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(0.dp, Color.Transparent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Logo & Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeonGreen)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Moviefun",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Box",
                    color = NeonGreen,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Right side: Access Unlocked Indicator + Search + Telegram
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Session Unlocked pill
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.testTag("session_timer_badge")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Unlocked",
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onSearchToggle,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchActive) NeonGreen else TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onTelegramClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Telegram",
                        tint = AccentBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        color = if (isSelected) NeonGreen else DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) NeonGreen else DarkBorder
        ),
        modifier = Modifier
            .clickable { onSelect() }
            .testTag("category_chip_${category.id}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = category.icon, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = category.name,
                color = if (isSelected) Color.Black else TextPrimary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MovieCardItem(
    movie: MovieItem,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("movie_item_${movie.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .background(DarkSurfaceVariant)
            ) {
                if (movie.fullPosterUrl.isNotEmpty()) {
                    AsyncImage(
                        model = movie.fullPosterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Neon Rating Badge at Top Right
                Surface(
                    color = NeonGreen,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = movie.formattedRating,
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Play overlay indicator
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = "Play",
                        tint = NeonGreen.copy(alpha = 0.85f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Info below poster
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = movie.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = movie.year,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = if (movie.mediaType == "tv") "SERIES" else "HD",
                        color = if (movie.mediaType == "tv") AccentYellow else AccentBlue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
