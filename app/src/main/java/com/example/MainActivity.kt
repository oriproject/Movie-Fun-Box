package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.AdUnlockScreen
import com.example.ui.MainMovieDashboard
import com.example.ui.MovieViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MovieViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val isUnlocked by viewModel.isUnlocked.collectAsState()

                    AnimatedContent(
                        targetState = isUnlocked,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "unlock_transition",
                        modifier = Modifier.padding(innerPadding)
                    ) { unlocked ->
                        if (unlocked) {
                            MainMovieDashboard(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AdUnlockScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check 12-hour session validity and auto-unlock if 20s ad completed
        viewModel.handleAppResume()
    }
}
