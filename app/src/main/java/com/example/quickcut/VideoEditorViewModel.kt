package com.example.quickcut

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.RgbMatrix
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@UnstableApi
class VideoEditorViewModel : ViewModel() {
    var state by mutableStateOf(VideoEditorState())
        private set

    fun setVideoUri(uri: Uri, durationMs: Long) {
        state = state.copy(
            videoUri = uri,
            videoDurationMs = durationMs,
            trimStartMs = 0,
            trimEndMs = durationMs,
            selectedFilter = VideoFilter.NONE,
            exportedUri = null
        )
    }

    fun setTrimRange(startMs: Long, endMs: Long) {
        state = state.copy(trimStartMs = startMs, trimEndMs = endMs)
    }

    fun setFilter(filter: VideoFilter) {
        state = state.copy(selectedFilter = filter)
    }

    fun exportVideo(context: Context, outputFolderUri: Uri, onComplete: (Uri?) -> Unit) {
        val videoUri = state.videoUri ?: return
        
        viewModelScope.launch {
            state = state.copy(isExporting = true, exportProgress = 0f)
            
            try {
                val tempFile = withContext(Dispatchers.IO) {
                    File(context.cacheDir, "quickcut_${System.currentTimeMillis()}.mp4")
                }
                
                val clippingConfig = MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(state.trimStartMs)
                    .setEndPositionMs(state.trimEndMs)
                    .build()
                
                val mediaItem = MediaItem.Builder()
                    .setUri(videoUri)
                    .setClippingConfiguration(clippingConfig)
                    .build()
                
                val effects = createEffects(state.selectedFilter)
                val editedMediaItem = EditedMediaItem.Builder(mediaItem)
                    .setEffects(effects)
                    .build()
                
                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            viewModelScope.launch {
                                try {
                                    val finalUri = withContext(Dispatchers.IO) {
                                        val docFile = DocumentFile.fromTreeUri(context, outputFolderUri)
                                        val outputDoc = docFile?.createFile("video/mp4", "quickcut_${System.currentTimeMillis()}.mp4")
                                        outputDoc?.uri?.let { destUri ->
                                            context.contentResolver.openOutputStream(destUri)?.use { output ->
                                                tempFile.inputStream().use { input ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            tempFile.delete()
                                            destUri
                                        }
                                    }
                                    
                                    state = state.copy(
                                        isExporting = false,
                                        exportProgress = 1f,
                                        exportedUri = finalUri
                                    )
                                    onComplete(finalUri)
                                } catch (e: Exception) {
                                    state = state.copy(isExporting = false)
                                    onComplete(null)
                                }
                            }
                        }
                        
                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            state = state.copy(isExporting = false, exportProgress = 0f)
                            onComplete(null)
                        }
                    })
                    .build()
                
                transformer.start(editedMediaItem, tempFile.absolutePath)
            } catch (e: Exception) {
                state = state.copy(isExporting = false)
                onComplete(null)
            }
        }
    }

    fun createEffects(filter: VideoFilter): Effects {
        val videoEffects = when (filter) {
            VideoFilter.NONE -> emptyList()
            
            VideoFilter.BLACK_WHITE -> listOf(RgbFilter.createGrayscaleFilter())
            
            VideoFilter.DARK -> listOf(
                RgbMatrix { _, _ ->
                    floatArrayOf(
                        0.4f, 0f, 0f, 0f,
                        0f, 0.4f, 0f, 0f,
                        0f, 0f, 0.4f, 0f,
                        0f, 0f, 0f, 1f
                    )
                }
            )
            
            VideoFilter.SEPIA -> listOf(
                RgbMatrix { _, _ ->
                    floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f,
                        0.349f, 0.686f, 0.168f, 0f,
                        0.272f, 0.534f, 0.131f, 0f,
                        0f, 0f, 0f, 1f
                    )
                }
            )
            
            VideoFilter.INVERTED -> listOf(RgbFilter.createInvertedFilter())
            
            VideoFilter.HIGH_CONTRAST -> listOf(Contrast(0.5f))
            
            VideoFilter.WARM -> listOf(
                RgbMatrix { _, _ ->
                    floatArrayOf(
                        1.3f, 0f, 0f, 0f,
                        0f, 1.1f, 0f, 0f,
                        0f, 0f, 0.8f, 0f,
                        0f, 0f, 0f, 1f
                    )
                }
            )
        }
        return Effects(emptyList(), videoEffects)
    }

    fun reset() {
        state = VideoEditorState()
    }
}
