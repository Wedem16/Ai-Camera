package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EnhancementMode(val displayName: String, val badgeText: String) {
    NONE("Original Capture", "UNENHANCED CAPTURE"),
    DETAIL_MODE("Detail Mode (On-Device SR)", "DETAIL MODE • ON-DEVICE SUPER RESOLUTION"),
    GENERATIVE_AI_MODE("Generative AI Mode", "AI-ENHANCED • GENERATIVE RECONSTRUCTION")
}

@Entity(tableName = "photo_records")
data class PhotoRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val originalPath: String,
    val enhancedPath: String? = null,
    val capturedZoomRatio: Float = 5.0f,
    val isOpticalCapture: Boolean = false,
    val enhancementMode: String = EnhancementMode.NONE.name,
    val upscaleFactor: Int = 1,
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    val enhancedWidth: Int = 0,
    val enhancedHeight: Int = 0,
    val blurScore: Int = 0,
    val noiseScore: Int = 0,
    val compressionScore: Int = 0,
    val detailLossScore: Int = 0,
    val isGenerativeAiLabeled: Boolean = false,
    val aiReconstructionNotes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    val parsedMode: EnhancementMode
        get() = try {
            EnhancementMode.valueOf(enhancementMode)
        } catch (_: Exception) {
            EnhancementMode.NONE
        }

    val isEnhanced: Boolean
        get() = !enhancedPath.isNullOrBlank() && parsedMode != EnhancementMode.NONE
}
