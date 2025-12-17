package com.example.quickcut.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.quickcut.VideoEditorState
import com.example.quickcut.VideoFilter
import com.example.quickcut.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    state: VideoEditorState,
    onTrimChange: (Long, Long) -> Unit,
    onFilterChange: (VideoFilter) -> Unit,
    onPreview: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    
    LaunchedEffect(state.videoUri) {
        state.videoUri?.let { uri ->
            player?.release()
            player = ExoPlayer.Builder(context).build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                prepare()
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrightCyan.copy(alpha = 0.1f), LightBackground)
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
                    text = "Редактировать",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.width(60.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Video Preview
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
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Trim Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Обрезать видео",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "Начало: ${formatTime(state.trimStartMs)}",
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Slider(
                        value = state.trimStartMs.toFloat(),
                        onValueChange = { 
                            val newStart = it.toLong()
                            if (newStart < state.trimEndMs - 1000) {
                                onTrimChange(newStart, state.trimEndMs)
                            }
                        },
                        valueRange = 0f..state.videoDurationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = BrightOrange,
                            activeTrackColor = BrightOrange
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Конец: ${formatTime(state.trimEndMs)}",
                        fontSize = 14.sp,
                        color = TextDark
                    )
                    Slider(
                        value = state.trimEndMs.toFloat(),
                        onValueChange = { 
                            val newEnd = it.toLong()
                            if (newEnd > state.trimStartMs + 1000) {
                                onTrimChange(state.trimStartMs, newEnd)
                            }
                        },
                        valueRange = 0f..state.videoDurationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = BrightPink,
                            activeTrackColor = BrightPink
                        )
                    )
                    
                    Text(
                        text = "Длительность: ${formatTime(state.trimEndMs - state.trimStartMs)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = BrightGreen
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Filters Section
            Text(
                text = "Фильтры",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(VideoFilter.entries) { filter ->
                    FilterChip(
                        filter = filter,
                        isSelected = state.selectedFilter == filter,
                        onClick = { onFilterChange(filter) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Preview Button
            Button(
                onClick = onPreview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrightCyan),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Превью и экспорт →", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
        }
    }
}

@Composable
private fun FilterChip(
    filter: VideoFilter,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (filter) {
        VideoFilter.NONE -> Color(0xFF9E9E9E)
        VideoFilter.BLACK_WHITE -> Color(0xFF212121)
        VideoFilter.DARK -> Color(0xFF1A1A1A)
        VideoFilter.SEPIA -> Color(0xFFD4A574)
        VideoFilter.INVERTED -> Color(0xFF00BCD4)
        VideoFilter.HIGH_CONTRAST -> Color(0xFFFF5722)
        VideoFilter.WARM -> Color(0xFFFF9800)
    }
    
    val textColor = when (filter) {
        VideoFilter.SEPIA, VideoFilter.WARM -> TextDark
        else -> TextLight
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(
                if (isSelected) Modifier.border(3.dp, BrightPink, RoundedCornerShape(12.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = filter.displayName,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}

private fun formatTime(ms: Long): String {
    val seconds = (ms / 1000) % 60
    val minutes = (ms / 1000 / 60) % 60
    return String.format("%d:%02d", minutes, seconds)
}
