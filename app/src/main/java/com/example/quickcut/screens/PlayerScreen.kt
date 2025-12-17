package com.example.quickcut.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.quickcut.VideoEditorState
import com.example.quickcut.VideoFilter
import com.example.quickcut.ui.theme.*

@UnstableApi
@Composable
fun PlayerScreen(
    state: VideoEditorState,
    onExport: (Uri) -> Unit,
    onBack: () -> Unit,
    onNewVideo: () -> Unit
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
    
    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { folderUri ->
            context.contentResolver.takePersistableUriPermission(
                folderUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            selectedFolderUri = folderUri
            onExport(folderUri)
        }
    }
    
    LaunchedEffect(state.videoUri, state.trimStartMs, state.trimEndMs) {
        state.videoUri?.let { uri ->
            player?.release()
            player = ExoPlayer.Builder(context).build().apply {
                val clippedItem = MediaItem.Builder()
                    .setUri(uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(state.trimStartMs)
                            .setEndPositionMs(state.trimEndMs)
                            .build()
                    )
                    .build()
                setMediaItem(clippedItem)
                prepare()
                playWhenReady = true
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }
    
    // Get color filter for preview overlay
    val colorMatrix = remember(state.selectedFilter) {
        when (state.selectedFilter) {
            VideoFilter.NONE -> null
            VideoFilter.BLACK_WHITE -> ColorMatrix().apply { setToSaturation(0f) }
            VideoFilter.DARK -> ColorMatrix(floatArrayOf(
                0.4f, 0f, 0f, 0f, 0f,
                0f, 0.4f, 0f, 0f, 0f,
                0f, 0f, 0.4f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            VideoFilter.SEPIA -> ColorMatrix(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            VideoFilter.INVERTED -> ColorMatrix(floatArrayOf(
                -1f, 0f, 0f, 0f, 255f,
                0f, -1f, 0f, 0f, 255f,
                0f, 0f, -1f, 0f, 255f,
                0f, 0f, 0f, 1f, 0f
            ))
            VideoFilter.HIGH_CONTRAST -> ColorMatrix(floatArrayOf(
                1.5f, 0f, 0f, 0f, -50f,
                0f, 1.5f, 0f, 0f, -50f,
                0f, 0f, 1.5f, 0f, -50f,
                0f, 0f, 0f, 1f, 0f
            ))
            VideoFilter.WARM -> ColorMatrix(floatArrayOf(
                1.3f, 0f, 0f, 0f, 0f,
                0f, 1.1f, 0f, 0f, 0f,
                0f, 0f, 0.8f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrightGreen.copy(alpha = 0.1f), LightBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Назад", color = BrightOrange, fontSize = 16.sp)
                }
                Text(
                    text = "Превью",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.width(60.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Video Preview with filter overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardDark)
            ) {
                player?.let { exoPlayer ->
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                this.player = exoPlayer
                                useController = true
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                // Filter overlay for preview
                if (colorMatrix != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                when (state.selectedFilter) {
                                    VideoFilter.DARK -> Color.Black.copy(alpha = 0.5f)
                                    VideoFilter.SEPIA -> Color(0x33D4A574)
                                    VideoFilter.WARM -> Color(0x22FF9800)
                                    else -> Color.Transparent
                                }
                            )
                    )
                }
                
                // Filter indicator
                if (state.selectedFilter != VideoFilter.NONE) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(BrightPurple, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = state.selectedFilter.displayName,
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Note about filter preview
            if (state.selectedFilter != VideoFilter.NONE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = BrightYellow.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "ℹ️ Фильтр \"${state.selectedFilter.displayName}\" будет применён после экспорта",
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp,
                        color = TextDark
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardLight)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Настройки экспорта",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Длительность:", color = TextDark.copy(alpha = 0.7f))
                        Text(
                            formatTime(state.trimEndMs - state.trimStartMs),
                            fontWeight = FontWeight.Medium,
                            color = BrightGreen
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Фильтр:", color = TextDark.copy(alpha = 0.7f))
                        Text(
                            state.selectedFilter.displayName,
                            fontWeight = FontWeight.Medium,
                            color = BrightPurple
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Export Progress or Button
            if (state.isExporting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrightYellow.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = BrightOrange,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Экспорт видео...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDark
                        )
                    }
                }
            } else if (state.exportedUri != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrightGreen.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Экспорт завершён!",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrightGreen
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                state.exportedUri?.let { uri ->
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "video/mp4"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Поделиться видео"))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Видео успешно отправлено!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Поделиться видео", color = TextDark)
                        }
                    }
                }
            } else {
                Button(
                    onClick = { folderLauncher.launch(null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrightGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Выбрать папку и экспортировать", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // New Video Button
            OutlinedButton(
                onClick = onNewVideo,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrightOrange)
            ) {
                Text("Начать новый проект", fontSize = 16.sp)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    return String.format("%d:%02d", minutes, seconds)
}
