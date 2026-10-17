package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.EnhancementMode
import com.example.data.PhotoRecord
import com.example.data.PhotoRepository
import com.example.domain.GeminiEnhancementService
import com.example.domain.ZoomImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class StudioDestination {
    PRO_CAMERA,
    AI_ZOOM_EDITOR,
    HISTORY_GALLERY
}

data class CameraProTelemetryState(
    val actualZoomRatio: Float = 1.0f,
    val minZoomRatio: Float = 1.0f,
    val maxZoomRatio: Float = 20.0f,
    val opticalThresholdMax: Float = 3.0f,
    val focusPeakingEnabled: Boolean = true,
    val liveHistogramEnabled: Boolean = true,
    val pipZoomLockEnabled: Boolean = true,
    val capture4kUhdEnabled: Boolean = true,
    val lumaHistogramBins: FloatArray = FloatArray(32) { 0.15f },
    val focusPeakingNormalizedPoints: List<Offset> = emptyList(),
    val isCapturing: Boolean = false
) {
    val isOpticalOrSensorZoom: Boolean
        get() = actualZoomRatio <= opticalThresholdMax || (actualZoomRatio in 4.9f..5.1f)
}

data class EditorUiState(
    val activeRecord: PhotoRecord? = null,
    val originalBitmap: Bitmap? = null,
    val enhancedBitmap: Bitmap? = null,
    val selectedMode: EnhancementMode = EnhancementMode.DETAIL_MODE,
    val selectedUpscaleFactor: Int = ZoomImageProcessor.SCALE_4K_UHD,
    val sharpnessStrength: Float = 1.20f,
    val preserveFaceIdentity: Boolean = true,
    val splitFraction: Float = 0.50f,
    val magnifierLoupeEnabled: Boolean = false,
    val loupeNormalizedPosition: Offset = Offset(0.5f, 0.45f),
    val isProcessing: Boolean = false,
    val processingProgress: Float = 0f,
    val processingStageLabel: String = "",
    val showPhysicsSheet: Boolean = false,
    val statusMessage: String? = null
) {
    val selectedScaleDisplayLabel: String
        get() = if (selectedUpscaleFactor == ZoomImageProcessor.SCALE_4K_UHD) {
            "4K UHD"
        } else {
            "${selectedUpscaleFactor}x"
        }
}

class ZoomStudioViewModel(
    private val appContext: Context,
    private val repository: PhotoRepository
) : ViewModel() {

    private val _currentDestination = MutableStateFlow(StudioDestination.AI_ZOOM_EDITOR)
    val currentDestination: StateFlow<StudioDestination> = _currentDestination.asStateFlow()

    private val _cameraState = MutableStateFlow(CameraProTelemetryState())
    val cameraState: StateFlow<CameraProTelemetryState> = _cameraState.asStateFlow()

    private val _editorState = MutableStateFlow(EditorUiState())
    val editorState: StateFlow<EditorUiState> = _editorState.asStateFlow()

    val allPhotoRecords: StateFlow<List<PhotoRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var processingJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureSeededWithRealZoomSamples()
            allPhotoRecords.collect { records ->
                if (_editorState.value.activeRecord == null && records.isNotEmpty()) {
                    selectPhotoRecord(records.first())
                } else {
                    val currentId = _editorState.value.activeRecord?.id
                    val refreshed = records.find { it.id == currentId }
                    if (refreshed != null && refreshed != _editorState.value.activeRecord) {
                        _editorState.update { it.copy(activeRecord = refreshed) }
                    }
                }
            }
        }
    }

    fun navigateTo(destination: StudioDestination) {
        _currentDestination.value = destination
    }

    fun selectPhotoRecord(record: PhotoRecord, openEditor: Boolean = false) {
        viewModelScope.launch {
            val origBmp = withContext(Dispatchers.IO) {
                ZoomImageProcessor.decodeMemorySafeBitmap(record.originalPath, 1600)
            }
            val enhBmp = withContext(Dispatchers.IO) {
                record.enhancedPath?.let { path ->
                    ZoomImageProcessor.decodeMemorySafeBitmap(path, 3840)
                }
            }
            _editorState.update { state ->
                state.copy(
                    activeRecord = record,
                    originalBitmap = origBmp,
                    enhancedBitmap = enhBmp,
                    selectedMode = if (record.parsedMode == EnhancementMode.GENERATIVE_AI_MODE) {
                        EnhancementMode.GENERATIVE_AI_MODE
                    } else {
                        EnhancementMode.DETAIL_MODE
                    },
                    selectedUpscaleFactor = when (record.upscaleFactor) {
                        ZoomImageProcessor.SCALE_4K_UHD -> ZoomImageProcessor.SCALE_4K_UHD
                        2, 4 -> record.upscaleFactor
                        else -> state.selectedUpscaleFactor
                    },
                    statusMessage = null
                )
            }
            if (openEditor) {
                _currentDestination.value = StudioDestination.AI_ZOOM_EDITOR
            }
        }
    }

    fun setEnhancementMode(mode: EnhancementMode) {
        _editorState.update { it.copy(selectedMode = mode) }
    }

    fun setUpscaleFactor(factor: Int) {
        val valid = if (factor == ZoomImageProcessor.SCALE_4K_UHD) {
            ZoomImageProcessor.SCALE_4K_UHD
        } else {
            factor.coerceIn(1, 4)
        }
        _editorState.update { it.copy(selectedUpscaleFactor = valid) }
    }

    fun setSharpnessStrength(strength: Float) {
        _editorState.update { it.copy(sharpnessStrength = strength.coerceIn(0.5f, 2.0f)) }
    }

    fun setPreserveFaceIdentity(preserve: Boolean) {
        _editorState.update { it.copy(preserveFaceIdentity = preserve) }
    }

    fun setSplitFraction(fraction: Float) {
        _editorState.update { it.copy(splitFraction = fraction.coerceIn(0.02f, 0.98f)) }
    }

    fun toggleMagnifierLoupe() {
        _editorState.update { it.copy(magnifierLoupeEnabled = !it.magnifierLoupeEnabled) }
    }

    fun updateLoupePosition(normalized: Offset) {
        _editorState.update {
            it.copy(
                loupeNormalizedPosition = Offset(
                    normalized.x.coerceIn(0.08f, 0.92f),
                    normalized.y.coerceIn(0.08f, 0.92f)
                )
            )
        }
    }

    fun setShowPhysicsSheet(show: Boolean) {
        _editorState.update { it.copy(showPhysicsSheet = show) }
    }

    fun updatePhotoZoomLevelTag(zoomRatio: Float) {
        val current = _editorState.value.activeRecord ?: return
        viewModelScope.launch {
            val updated = repository.updateZoomTag(current, zoomRatio)
            _editorState.update {
                it.copy(
                    activeRecord = updated,
                    statusMessage = "Updated capture zoom tag to ${String.format("%.1fx", zoomRatio)}"
                )
            }
        }
    }

    /**
     * Triggers AI Zoom Clarity / Super-Resolution (1x, 2x, 4x, or 4K UHD 3840p) asynchronously.
     */
    fun runEnhanceZoom() {
        val state = _editorState.value
        val record = state.activeRecord ?: return
        val sourceBitmap = state.originalBitmap ?: return
        if (state.isProcessing) return

        processingJob?.cancel()
        processingJob = viewModelScope.launch {
            _editorState.update {
                it.copy(
                    isProcessing = true,
                    processingProgress = 0.04f,
                    processingStageLabel = "Initializing ${state.selectedMode.displayName} (${state.selectedScaleDisplayLabel})…",
                    statusMessage = null
                )
            }

            try {
                val result = when (state.selectedMode) {
                    EnhancementMode.GENERATIVE_AI_MODE -> {
                        GeminiEnhancementService.processGenerativeAiMode(
                            context = appContext,
                            sourceBitmap = sourceBitmap,
                            requestedScale = state.selectedUpscaleFactor,
                            zoomRatio = record.capturedZoomRatio,
                            onProgress = { progress, stage ->
                                _editorState.update {
                                    it.copy(
                                        processingProgress = progress,
                                        processingStageLabel = stage
                                    )
                                }
                            }
                        )
                    }
                    else -> {
                        ZoomImageProcessor.processDetailMode(
                            context = appContext,
                            sourceBitmap = sourceBitmap,
                            requestedScale = state.selectedUpscaleFactor,
                            strength = state.sharpnessStrength,
                            preserveFacesAndSkin = state.preserveFaceIdentity,
                            onProgress = { progress, stage ->
                                _editorState.update {
                                    it.copy(
                                        processingProgress = progress,
                                        processingStageLabel = stage
                                    )
                                }
                            }
                        )
                    }
                }

                val updatedRecord = repository.applyEnhancementResult(
                    record = record,
                    mode = state.selectedMode,
                    result = result
                )

                val resTag = if (result.is4kOutput) "4K UHD • ${result.outputWidth}×${result.outputHeight}" else "${result.outputWidth}×${result.outputHeight}"
                _editorState.update {
                    it.copy(
                        activeRecord = updatedRecord,
                        enhancedBitmap = result.enhancedBitmap,
                        isProcessing = false,
                        processingProgress = 1.0f,
                        processingStageLabel = "",
                        statusMessage = if (result.isGenerativeAi) {
                            "Generative AI Reconstruction complete ($resTag) • Labeled AI-Enhanced"
                        } else {
                            "Detail Mode Super-Resolution complete ($resTag)"
                        }
                    )
                }
            } catch (e: Exception) {
                _editorState.update {
                    it.copy(
                        isProcessing = false,
                        processingProgress = 0f,
                        processingStageLabel = "",
                        statusMessage = "Enhancement interrupted: ${e.localizedMessage ?: "Unknown error"}"
                    )
                }
            }
        }
    }

    fun undoEnhancement() {
        val record = _editorState.value.activeRecord ?: return
        viewModelScope.launch {
            val reverted = repository.revertToOriginal(record)
            _editorState.update {
                it.copy(
                    activeRecord = reverted,
                    enhancedBitmap = null,
                    statusMessage = "Reverted to original untouched photograph."
                )
            }
        }
    }

    fun exportActivePhotoToGallery() {
        val record = _editorState.value.activeRecord ?: return
        viewModelScope.launch {
            val ok = repository.exportToDeviceGallery(record)
            _editorState.update {
                it.copy(
                    statusMessage = if (ok) {
                        "Exported master photo to Device Gallery (Pictures/GalaxyZoomAI)"
                    } else {
                        "Could not export to Device Gallery."
                    }
                )
            }
        }
    }

    fun importPhotoFromPicker(uri: Uri) {
        viewModelScope.launch {
            _editorState.update {
                it.copy(
                    isProcessing = true,
                    processingProgress = 0.35f,
                    processingStageLabel = "Importing photo & analyzing blur, noise & EXIF zoom…"
                )
            }
            val importedRecord = repository.importFromUri(uri)
            _editorState.update {
                it.copy(
                    isProcessing = false,
                    processingProgress = 0f,
                    processingStageLabel = ""
                )
            }
            if (importedRecord != null) {
                selectPhotoRecord(importedRecord, openEditor = true)
                _editorState.update {
                    it.copy(statusMessage = "Imported zoomed photo (${importedRecord.originalWidth}×${importedRecord.originalHeight})")
                }
            } else {
                _editorState.update {
                    it.copy(statusMessage = "Failed to decode selected photo.")
                }
            }
        }
    }

    fun deletePhotoRecord(record: PhotoRecord) {
        viewModelScope.launch {
            val wasActive = _editorState.value.activeRecord?.id == record.id
            repository.deleteRecord(record)
            if (wasActive) {
                val remaining = allPhotoRecords.value.filter { it.id != record.id }
                if (remaining.isNotEmpty()) {
                    selectPhotoRecord(remaining.first())
                } else {
                    _editorState.update {
                        it.copy(activeRecord = null, originalBitmap = null, enhancedBitmap = null)
                    }
                }
            }
        }
    }

    // --- CameraX Pro Telemetry & Capture Handlers ---

    fun onCameraZoomStateUpdated(actualRatio: Float, minRatio: Float, maxRatio: Float) {
        _cameraState.update {
            it.copy(
                actualZoomRatio = actualRatio.coerceIn(minRatio, maxRatio.coerceAtLeast(10f)),
                minZoomRatio = minRatio,
                maxZoomRatio = maxRatio.coerceAtLeast(20f)
            )
        }
    }

    fun setSimulatedOrTargetZoomRatio(targetRatio: Float) {
        _cameraState.update {
            it.copy(actualZoomRatio = targetRatio.coerceIn(it.minZoomRatio, it.maxZoomRatio))
        }
    }

    fun toggleFocusPeaking() {
        _cameraState.update { it.copy(focusPeakingEnabled = !it.focusPeakingEnabled) }
    }

    fun toggleLiveHistogram() {
        _cameraState.update { it.copy(liveHistogramEnabled = !it.liveHistogramEnabled) }
    }

    fun togglePipZoomLock() {
        _cameraState.update { it.copy(pipZoomLockEnabled = !it.pipZoomLockEnabled) }
    }

    fun toggleCapture4kUhd() {
        _cameraState.update { it.copy(capture4kUhdEnabled = !it.capture4kUhdEnabled) }
    }

    fun updateLiveFrameTelemetry(histogramBins: FloatArray, peakingPoints: List<Offset>) {
        _cameraState.update {
            it.copy(
                lumaHistogramBins = histogramBins,
                focusPeakingNormalizedPoints = peakingPoints
            )
        }
    }

    fun onCameraPhotoCapturedFile(file: File, zoomRatio: Float, isOptical: Boolean) {
        viewModelScope.launch {
            _cameraState.update { it.copy(isCapturing = false) }
            val record = repository.registerCapturedPhoto(
                file = file,
                zoomRatio = zoomRatio,
                isOptical = isOptical,
                title = "Pro Camera ${String.format("%.1fx", zoomRatio)} (${if (isOptical) "Optical/Sensor" else "Digital Zoom"})"
            )
            if (record != null) {
                selectPhotoRecord(record, openEditor = true)
            }
        }
    }

    fun captureProTestBenchPhoto(sceneIndex: Int = 0) {
        val cam = _cameraState.value
        if (cam.isCapturing) return
        _cameraState.update { it.copy(isCapturing = true) }

        viewModelScope.launch {
            val samples = com.example.data.SamplePhotoGenerator.getBuiltInSamples()
            val chosen = samples[sceneIndex.coerceIn(0, samples.lastIndex)]
            val w = if (cam.capture4kUhdEnabled) 1200 else 800
            val h = if (cam.capture4kUhdEnabled) 900 else 600
            val bmp = withContext(Dispatchers.Default) {
                chosen.generator(w, h)
            }
            val dir = File(appContext.filesDir, "zoom_originals").apply { mkdirs() }
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { out ->
                    bmp.compress(Bitmap.CompressFormat.JPEG, 94, out)
                }
                bmp.recycle()
            }
            onCameraPhotoCapturedFile(
                file = file,
                zoomRatio = cam.actualZoomRatio,
                isOptical = cam.isOpticalOrSensorZoom
            )
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val appContext = context.applicationContext
                    val db = AppDatabase.getInstance(appContext)
                    val repo = PhotoRepository(appContext, db.photoRecordDao())
                    return ZoomStudioViewModel(appContext, repo) as T
                }
            }
        }
    }
}
