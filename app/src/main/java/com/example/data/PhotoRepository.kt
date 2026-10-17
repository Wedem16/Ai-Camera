package com.example.data

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.domain.EnhancementResult
import com.example.domain.ZoomImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class PhotoRepository(
    private val context: Context,
    private val dao: PhotoRecordDao
) {
    val allRecords: Flow<List<PhotoRecord>> = dao.observeAllRecords()

    suspend fun ensureSeededWithRealZoomSamples() = withContext(Dispatchers.IO) {
        if (dao.getRecordCount() > 0) return@withContext

        val originalsDir = File(context.filesDir, "zoom_originals").apply { mkdirs() }
        val samples = SamplePhotoGenerator.getBuiltInSamples()

        samples.forEachIndexed { index, spec ->
            val file = File(originalsDir, "sample_${index + 1}.jpg")
            val bmp = spec.generator(800, 600)
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            val diagnostics = ZoomImageProcessor.analyzeDiagnostics(bmp, spec.zoomRatio)
            val w = bmp.width
            val h = bmp.height
            bmp.recycle()

            dao.insertRecord(
                PhotoRecord(
                    title = spec.title,
                    originalPath = file.absolutePath,
                    enhancedPath = null,
                    capturedZoomRatio = spec.zoomRatio,
                    isOpticalCapture = spec.isOptical,
                    enhancementMode = EnhancementMode.NONE.name,
                    upscaleFactor = 1,
                    originalWidth = w,
                    originalHeight = h,
                    enhancedWidth = w,
                    enhancedHeight = h,
                    blurScore = diagnostics.blurScore,
                    noiseScore = diagnostics.noiseScore,
                    compressionScore = diagnostics.compressionScore,
                    detailLossScore = diagnostics.detailLossScore,
                    isGenerativeAiLabeled = false,
                    aiReconstructionNotes = diagnostics.summaryRecommendation,
                    timestamp = System.currentTimeMillis() - (samples.size - index) * 60_000L
                )
            )
        }
    }

    suspend fun registerCapturedPhoto(
        file: File,
        zoomRatio: Float,
        isOptical: Boolean,
        title: String = "Pro Camera ${String.format("%.1fx", zoomRatio)} Capture"
    ): PhotoRecord? = withContext(Dispatchers.IO) {
        val bmp = ZoomImageProcessor.decodeMemorySafeBitmap(file.absolutePath, 1600) ?: return@withContext null
        val diagnostics = ZoomImageProcessor.analyzeDiagnostics(bmp, zoomRatio)
        val w = bmp.width
        val h = bmp.height
        bmp.recycle()

        val record = PhotoRecord(
            title = title,
            originalPath = file.absolutePath,
            enhancedPath = null,
            capturedZoomRatio = zoomRatio,
            isOpticalCapture = isOptical,
            enhancementMode = EnhancementMode.NONE.name,
            upscaleFactor = 1,
            originalWidth = w,
            originalHeight = h,
            enhancedWidth = w,
            enhancedHeight = h,
            blurScore = diagnostics.blurScore,
            noiseScore = diagnostics.noiseScore,
            compressionScore = diagnostics.compressionScore,
            detailLossScore = diagnostics.detailLossScore,
            isGenerativeAiLabeled = false,
            aiReconstructionNotes = diagnostics.summaryRecommendation
        )
        val id = dao.insertRecord(record)
        dao.getRecordById(id)
    }

    suspend fun importFromUri(
        uri: Uri,
        userSpecifiedZoomRatio: Float? = null
    ): PhotoRecord? = withContext(Dispatchers.IO) {
        val imported = ZoomImageProcessor.importUriToInternalFile(context, uri) ?: return@withContext null
        val (file, exifZoom) = imported
        val finalZoom = userSpecifiedZoomRatio ?: exifZoom
        registerCapturedPhoto(
            file = file,
            zoomRatio = finalZoom,
            isOptical = finalZoom <= 3.0f,
            title = "Imported ${String.format("%.1fx", finalZoom)} Zoom Photo"
        )
    }

    suspend fun applyEnhancementResult(
        record: PhotoRecord,
        mode: EnhancementMode,
        result: EnhancementResult
    ): PhotoRecord = withContext(Dispatchers.IO) {
        val updated = record.copy(
            enhancedPath = result.outputFilePath,
            enhancementMode = mode.name,
            upscaleFactor = result.appliedScaleFactor,
            enhancedWidth = result.outputWidth,
            enhancedHeight = result.outputHeight,
            isGenerativeAiLabeled = result.isGenerativeAi,
            aiReconstructionNotes = result.reconstructionNotes,
            timestamp = System.currentTimeMillis()
        )
        dao.updateRecord(updated)
        updated
    }

    /**
     * Keeps the original photo completely unchanged and reverts any applied AI super-resolution or generative pass.
     */
    suspend fun revertToOriginal(record: PhotoRecord): PhotoRecord = withContext(Dispatchers.IO) {
        record.enhancedPath?.let { path ->
            runCatching { File(path).delete() }
        }
        val reverted = record.copy(
            enhancedPath = null,
            enhancementMode = EnhancementMode.NONE.name,
            upscaleFactor = 1,
            enhancedWidth = record.originalWidth,
            enhancedHeight = record.originalHeight,
            isGenerativeAiLabeled = false,
            aiReconstructionNotes = "Reverted to original untouched sensor capture."
        )
        dao.updateRecord(reverted)
        reverted
    }

    suspend fun updateZoomTag(record: PhotoRecord, newZoomRatio: Float): PhotoRecord = withContext(Dispatchers.IO) {
        val bmp = ZoomImageProcessor.decodeMemorySafeBitmap(record.originalPath, 800)
        val diagnostics = if (bmp != null) {
            val diag = ZoomImageProcessor.analyzeDiagnostics(bmp, newZoomRatio)
            bmp.recycle()
            diag
        } else {
            null
        }
        val updated = record.copy(
            capturedZoomRatio = newZoomRatio,
            isOpticalCapture = newZoomRatio <= 3.0f,
            detailLossScore = diagnostics?.detailLossScore ?: record.detailLossScore,
            aiReconstructionNotes = diagnostics?.summaryRecommendation ?: record.aiReconstructionNotes
        )
        dao.updateRecord(updated)
        updated
    }

    suspend fun deleteRecord(record: PhotoRecord) = withContext(Dispatchers.IO) {
        runCatching { File(record.originalPath).delete() }
        record.enhancedPath?.let { runCatching { File(it).delete() } }
        dao.deleteRecordById(record.id)
    }

    /**
     * Exports the active photo (Enhanced if available, otherwise Original) to the device's MediaStore Pictures gallery.
     */
    suspend fun exportToDeviceGallery(record: PhotoRecord): Boolean = withContext(Dispatchers.IO) {
        val sourcePath = record.enhancedPath ?: record.originalPath
        val sourceFile = File(sourcePath)
        if (!sourceFile.exists()) return@withContext false

        val suffix = if (record.isGenerativeAiLabeled) "AI_Enhanced_Gen" else "SR_${record.upscaleFactor}x"
        val fileName = "GalaxyZoom_${suffix}_${System.currentTimeMillis()}.jpg"

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/GalaxyZoomAI")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val itemUri = resolver.insert(collection, values) ?: return@withContext false
        return@withContext try {
            resolver.openOutputStream(itemUri)?.use { out ->
                FileInputStream(sourceFile).use { input ->
                    input.copyTo(out)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
