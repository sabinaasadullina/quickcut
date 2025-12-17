package com.example.quickcut

import android.net.Uri

data class VideoEditorState(
    val videoUri: Uri? = null,
    val trimStartMs: Long = 0,
    val trimEndMs: Long = 0,
    val videoDurationMs: Long = 0,
    val selectedFilter: VideoFilter = VideoFilter.NONE,
    val isExporting: Boolean = false,
    val exportProgress: Float = 0f,
    val exportedUri: Uri? = null
)

enum class VideoFilter(val displayName: String) {
    NONE("Оригинал"),
    BLACK_WHITE("Чёрнобелый"),
    DARK("Тёмный"),
    SEPIA("Сепия"),
    INVERTED("Инвертация"),
    HIGH_CONTRAST("Контраcт"),
    WARM("Тёплый")
}
