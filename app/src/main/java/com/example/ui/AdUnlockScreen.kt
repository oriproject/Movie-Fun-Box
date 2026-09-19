package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AccessManager

@Composable
fun AdUnlockScreen(
    viewModel: MovieViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVerifying by viewModel.isAdVerificationActive.collectAsState()
    val countdown by viewModel.adVerificationCountdown.collectAsState()
    val hasAttemptedClick by viewModel.hasAttemptedAdClick.collectAsState()
    val isReturnedEarly by viewModel.isReturnedEarly.collectAsState()
    val earlyRemaining by viewModel.earlyReturnRemaining.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0D10))
    ) {
        // Blurred movie collage background as shown in screenshot
        Image(
            painter = painterResource(id = R.drawable.bg_movie_collage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(16.dp)
        )

        // Dark dimming scrim overlay matching screenshot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.80f),
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        // Main Content matching user's screenshot
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. "We Need Support" Yellow Heading
            Text(
                text = "We Need Support",
                color = Color(0xFFFFD500),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
                modifier = Modifier.testTag("ads_heading_text")
            )

            Spacer(modifier = Modifier.height(30.dp))

            // 2. Button Area:
            // If user has not attempted or not returned early: Show RED "Click Here" button.
            // If user returned early before 20s: 'Click Here' button is HIDDEN as requested,
            // and an alert tells them to use 'Reload' below to view the ad for 20 seconds.
            if (!isReturnedEarly) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFA12C2C)),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .widthIn(min = 230.dp, max = 280.dp)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AccessManager.SMART_LINK_URL))
                            context.startActivity(intent)
                            viewModel.startAdVerification()
                        }
                        .testTag("open_ads_button")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Click Here",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Watch ADS to Unlock",
                            color = Color(0xFFFFD500),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // When returning early (<20 seconds), 'Click Here' is hidden, and user is instructed to tap Reload
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1515)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE53935)),
                    modifier = Modifier
                        .widthIn(min = 260.dp, max = 340.dp)
                        .padding(horizontal = 8.dp)
                        .testTag("early_return_notice")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = "বিজ্ঞাপন সম্পূর্ণ দেখা হয়নি!",
                                color = Color(0xFFFF5252),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "আপনি ২০ সেকেন্ড সম্পূর্ণ হওয়ার আগেই ফিরে এসেছেন (আরও ${earlyRemaining}s বাকি ছিল)। পুনরায় বিজ্ঞাপনটি দেখতে নিচের 'Reload' বাটনে ক্লিক করুন।",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Live status badge if timer is actively counting down
            AnimatedVisibility(
                visible = isVerifying && !isReturnedEarly,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFFFD500).copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFFD500),
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                text = "Checking ads: ${countdown}s remaining...",
                                color = Color(0xFFFFD500),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // 3. "How To Use." Heading
            Text(
                text = "How To Use.",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Instructions List
            Column(
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "(1) Click on above ADS Button.",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp
                )
                Text(
                    text = "(2) Ads will open in your device's browser.",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp
                )
                Text(
                    text = "(3) Check ads more then 20 seconds.",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp
                )
                Text(
                    text = "(4) After 20 Seconds Close ads and come back to APP.",
                    color = Color.White,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp
                )
            }

            Spacer(modifier = Modifier.height(26.dp))

            // 5. Green Note
            Text(
                text = "Guys, we know ads are a headache. If we don't put ads then we will have to shut down the app because there is not enough funds to run the app.",
                color = Color(0xFF00E676),
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
                modifier = Modifier.widthIn(max = 340.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 6. Reload Button
            // If user returned early, clicking Reload re-opens the ad in browser and resets the 20s timer.
            // If user hasn't clicked yet, it also opens the ad.
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(4.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AccessManager.SMART_LINK_URL))
                        context.startActivity(intent)
                        viewModel.startAdVerification()
                    }
                    .testTag("reload_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = "Reload",
                        color = Color.Black,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
