package com.example

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.AdUnlockScreen
import com.example.ui.MainMovieDashboard
import com.example.ui.MovieViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MovieViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setContent {
            MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    val isUnlocked by viewModel.isUnlocked.collectAsState()

                    AnimatedContent(
                        targetState = isUnlocked,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "unlock_transition",
                        modifier = Modifier.fillMaxSize()
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
        viewModel.handleAppResume()
    }

    override fun onStop() {
        super.onStop()
        viewModel.handleAppStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.handleAppDestroy()
    }
}
