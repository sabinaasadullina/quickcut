package com.example.quickcut

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.example.quickcut.screens.EditScreen
import com.example.quickcut.screens.MenuScreen
import com.example.quickcut.screens.PlayerScreen
import com.example.quickcut.ui.theme.QuickcutTheme

@UnstableApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuickcutTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VideoEditorApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

enum class Screen { MENU, EDIT, PLAYER }

@UnstableApi
@Composable
fun VideoEditorApp(modifier: Modifier = Modifier) {
    val viewModel: VideoEditorViewModel = viewModel()
    var currentScreen by remember { mutableStateOf(Screen.MENU) }
    val context = LocalContext.current

    when (currentScreen) {
        Screen.MENU -> MenuScreen(
            onVideoSelected = { uri, duration ->
                viewModel.setVideoUri(uri, duration)
                currentScreen = Screen.EDIT
            }
        )
        
        Screen.EDIT -> EditScreen(
            state = viewModel.state,
            onTrimChange = { start, end -> viewModel.setTrimRange(start, end) },
            onFilterChange = { filter -> viewModel.setFilter(filter) },
            onPreview = { currentScreen = Screen.PLAYER },
            onBack = { currentScreen = Screen.MENU }
        )
        
        Screen.PLAYER -> PlayerScreen(
            state = viewModel.state,
            onExport = { folderUri -> viewModel.exportVideo(context, folderUri) {} },
            onBack = { currentScreen = Screen.EDIT },
            onNewVideo = {
                viewModel.reset()
                currentScreen = Screen.MENU
            }
        )
    }
}
