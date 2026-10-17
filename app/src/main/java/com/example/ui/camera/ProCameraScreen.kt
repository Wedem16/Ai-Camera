package com.example.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.SamplePhotoGenerator
import com.example.ui.CameraProTelemetryState
import com.example.ui.theme.ControlPodSurface
import com.example.ui.theme.GalaxyLaserAmber
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.OpticCyan
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun ProCameraScreen(
    cameraState: CameraProTelemetryState,
    onCameraZoomUpdated: (Float, Float, Float) -> Unit,
    onSetTargetZoom: (Float) -> Unit,
    onToggleFocusPeaking: () -> Unit,
    onToggleHistogram: () -> Unit,
    onTogglePipLock: () -> Unit,
    onLiveTelemetryAnalyzed: (FloatArray, List<Offset>) -> Unit,
    onPhotoCapturedFile: (File, Float, Boolean) -> Unit,
    onCaptureTestBenchScene: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    var cameraInstance by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraHardwareAvailable by remember { mutableStateOf(true) }
    var activeTestSceneIndex by remember { mutableIntStateOf(0) }

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    // Apply zoom ratio to hardware CameraX whenever user scrubs zoom slider or taps a preset pill
    LaunchedEffect(cameraState.actualZoomRatio, cameraInstance) {
        val cam = cameraInstance ?: return@LaunchedEffect
        val zoomState = cam.cameraInfo.zoomState.value
        if (zoomState != null) {
            val clampedHwZoom = cameraState.actualZoomRatio.coerceIn(
                zoomState.minZoomRatio,
                zoomState.maxZoomRatio
            )
            cam.cameraControl.setZoomRatio(clampedHwZoom)
        }
    }

    val testScenes = remember { SamplePhotoGenerator.getBuiltInSamples() }
    val testSceneBitmap = remember(activeTestSceneIndex) {
        testScenes[activeTestSceneIndex].generator(640, 480)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Pro Viewfinder HUD Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (cameraState.isOpticalOrSensorZoom) {
                            StatusOptimal.copy(alpha = 0.2f)
                        } else {
                            StatusWarning.copy(alpha = 0.2f)
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            if (cameraState.isOpticalOrSensorZoom) StatusOptimal else StatusWarning
                        )
                    ) {
                        Text(
                            text = if (cameraState.isOpticalOrSensorZoom) {
                                "OPTICAL / SENSOR ZOOM"
                            } else {
                                "DIGITAL ZOOM"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cameraState.isOpticalOrSensorZoom) StatusOptimal else StatusWarning,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = String.format("%.1fx", cameraState.actualZoomRatio),
                        style = MaterialTheme.typography.titleLarge,
                        color = GalaxyLaserAmber,
                        modifier = Modifier.testTag("camera_zoom_ratio_text")
                    )
                }
                Text(
                    text = "Live preview: Hardware zoom • AI Super-Resolution runs post-capture",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Pro Viewfinder Overlay Toggles: Focus Peaking, Live Histogram, PiP Guide
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(
                    onClick = onToggleFocusPeaking,
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (cameraState.focusPeakingEnabled) OpticCyan.copy(alpha = 0.2f) else ControlPodSurface,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (cameraState.focusPeakingEnabled) OpticCyan else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .testTag("toggle_peaking_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CenterFocusStrong,
                        contentDescription = "Toggle Focus Peaking",
                        tint = if (cameraState.focusPeakingEnabled) OpticCyan else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onToggleHistogram,
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (cameraState.liveHistogramEnabled) GalaxyLaserAmber.copy(alpha = 0.2f) else ControlPodSurface,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (cameraState.liveHistogramEnabled) GalaxyLaserAmber else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .testTag("toggle_histogram_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = "Toggle Live Histogram",
                        tint = if (cameraState.liveHistogramEnabled) GalaxyLaserAmber else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onTogglePipLock,
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            if (cameraState.pipZoomLockEnabled) GalaxyLaserAmber.copy(alpha = 0.2f) else ControlPodSurface,
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (cameraState.pipZoomLockEnabled) GalaxyLaserAmber else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .testTag("toggle_pip_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Toggle PiP Zoom Target Guide",
                        tint = if (cameraState.pipZoomLockEnabled) GalaxyLaserAmber else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 2. Main CameraX Viewfinder + Real-Time Focus Peaking, Histogram & PiP Zoom Lock Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF07090D))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoomChange, _ ->
                        if (zoomChange != 1.0f) {
                            val nextZoom = (cameraState.actualZoomRatio * zoomChange).coerceIn(1.0f, 30.0f)
                            onSetTargetZoom(nextZoom)
                        }
                    }
                }
                .testTag("camera_viewfinder_box")
        ) {
            if (hasCameraPermission && cameraHardwareAvailable) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val imageCapture = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                    .build()
                                imageCaptureUseCase = imageCapture

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(analysisExecutor) { proxy ->
                                            analyzeLiveFrameTelemetry(proxy, onLiveTelemetryAnalyzed)
                                        }
                                    }

                                provider.unbindAll()
                                val cam = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                    imageAnalysis
                                )
                                cameraInstance = cam
                                cam.cameraInfo.zoomState.observe(lifecycleOwner) { zs ->
                                    if (zs != null) {
                                        onCameraZoomUpdated(
                                            zs.zoomRatio,
                                            zs.minZoomRatio,
                                            zs.maxZoomRatio
                                        )
                                    }
                                }
                            } catch (_: Exception) {
                                cameraHardwareAvailable = false
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Interactive Optical Zoom Simulation Bench when running without physical camera sensor or permission
                val imgBitmap = remember(testSceneBitmap) { testSceneBitmap.asImageBitmap() }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val zoom = cameraState.actualZoomRatio.coerceIn(1.0f, 30.0f)
                    val cropW = (imgBitmap.width / min(zoom, 8.0f)).roundToInt().coerceAtLeast(48)
                    val cropH = (imgBitmap.height / min(zoom, 8.0f)).roundToInt().coerceAtLeast(48)
                    val srcX = (imgBitmap.width - cropW) / 2
                    val srcY = (imgBitmap.height - cropH) / 2

                    drawImage(
                        image = imgBitmap,
                        srcOffset = IntOffset(srcX, srcY),
                        srcSize = IntSize(cropW, cropH),
                        dstOffset = IntOffset.Zero,
                        dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
                    )
                }
            }

            // Focus Peaking & Optical Reticle Canvas Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Rule-of-thirds optical grid
                val gridColor = Color.White.copy(alpha = 0.14f)
                drawLine(gridColor, Offset(w / 3f, 0f), Offset(w / 3f, h), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(w * 2f / 3f, 0f), Offset(w * 2f / 3f, h), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, h / 3f), Offset(w, h / 3f), strokeWidth = 1.dp.toPx())
                drawLine(gridColor, Offset(0f, h * 2f / 3f), Offset(w, h * 2f / 3f), strokeWidth = 1.dp.toPx())

                // Focus Peaking Cyan Edge Highlights
                if (cameraState.focusPeakingEnabled) {
                    val points = cameraState.focusPeakingNormalizedPoints
                    if (points.isNotEmpty()) {
                        points.forEach { norm ->
                            drawCircle(
                                color = OpticCyan.copy(alpha = 0.78f),
                                radius = 2.2.dp.toPx(),
                                center = Offset(norm.x * w, norm.y * h)
                            )
                        }
                    } else {
                        // Deterministic edge-peaking ring around center AF zone when static
                        drawCircle(
                            color = OpticCyan.copy(alpha = 0.65f),
                            radius = min(w, h) * 0.16f,
                            center = Offset(w * 0.5f, h * 0.5f),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                    }
                }

                // Center AF Crosshair
                val cx = w * 0.5f
                val cy = h * 0.5f
                val arm = 14.dp.toPx()
                drawLine(GalaxyLaserAmber, Offset(cx - arm, cy), Offset(cx + arm, cy), strokeWidth = 1.5.dp.toPx())
                drawLine(GalaxyLaserAmber, Offset(cx, cy - arm), Offset(cx, cy + arm), strokeWidth = 1.5.dp.toPx())
            }

            // Top-Left Live Luminance Histogram Overlay
            if (cameraState.liveHistogramEnabled) {
                Surface(
                    color = ObsidianBackground.copy(alpha = 0.82f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "LIVE LUMA HISTOGRAM ${if (cameraState.focusPeakingEnabled) "• PEAKING ON" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = OpticCyan
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Canvas(
                            modifier = Modifier
                                .width(124.dp)
                                .height(38.dp)
                        ) {
                            val bins = cameraState.lumaHistogramBins
                            val barW = size.width / bins.size.coerceAtLeast(1)
                            bins.forEachIndexed { i, value ->
                                val barH = (value.coerceIn(0.05f, 1.0f)) * size.height
                                drawRect(
                                    color = GalaxyLaserAmber.copy(alpha = 0.85f),
                                    topLeft = Offset(i * barW, size.height - barH),
                                    size = Size((barW - 1f).coerceAtLeast(1f), barH)
                                )
                            }
                        }
                    }
                }
            }

            // Top-Right Picture-in-Picture (PiP) Zoom Lock Target Guide when Zoom >= 3.0x
            if (cameraState.pipZoomLockEnabled && cameraState.actualZoomRatio >= 3.0f) {
                Surface(
                    color = ObsidianBackground.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, GalaxyLaserAmber),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(width = 108.dp, height = 82.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val boxScale = (1.0f / cameraState.actualZoomRatio.coerceAtLeast(1.0f)).coerceIn(0.12f, 0.7f)
                            val rectW = size.width * boxScale
                            val rectH = size.height * boxScale
                            val left = (size.width - rectW) / 2f
                            val top = (size.height - rectH) / 2f
                            drawRect(
                                color = GalaxyLaserAmber,
                                topLeft = Offset(left, top),
                                size = Size(rectW, rectH),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        Text(
                            text = "ZOOM LOCK ${String.format("%.1fx", cameraState.actualZoomRatio)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GalaxyLaserAmber,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }
                }
            }

            // Permission request banner if CAMERA permission is not granted yet
            if (!hasCameraPermission) {
                Surface(
                    color = ObsidianBackground.copy(alpha = 0.88f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, GalaxyLaserAmber),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Optical Test Bench Active",
                                style = MaterialTheme.typography.labelMedium,
                                color = GalaxyLaserAmber
                            )
                            Text(
                                text = "Grant Camera permission for live hardware sensor, or capture directly from the telephoto test target.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GalaxyLaserAmber,
                                contentColor = ObsidianBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("grant_camera_permission_button")
                        ) {
                            Text("Enable Camera", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // 3. Quick Zoom Preset Pills (1x, 2x, 5x, 10x, 20x) + Smooth Zoom Slider
        Surface(
            color = ControlPodSurface,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1.0f, 2.0f, 5.0f, 10.0f, 20.0f).forEach { preset ->
                        val isActive = abs(cameraState.actualZoomRatio - preset) < 0.35f
                        Surface(
                            color = if (isActive) GalaxyLaserAmber else ObsidianBackground,
                            shape = CircleShape,
                            border = BorderStroke(
                                1.dp,
                                if (isActive) GalaxyLaserAmber else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .clickable { onSetTargetZoom(preset) }
                                .testTag("camera_zoom_preset_${preset.toInt()}x")
                        ) {
                            Text(
                                text = "${preset.toInt()}x",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) ObsidianBackground else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Slider(
                    value = cameraState.actualZoomRatio,
                    onValueChange = onSetTargetZoom,
                    valueRange = 1.0f..20.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = GalaxyLaserAmber,
                        activeTrackColor = GalaxyLaserAmber,
                        inactiveTrackColor = ObsidianBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("camera_zoom_slider")
                )
            }
        }

        // 4. Shutter Capture & Scene Target Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Switch optical test target scene
            OutlinedButton(
                onClick = {
                    activeTestSceneIndex = (activeTestSceneIndex + 1) % testScenes.size
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.testTag("cycle_test_scene_button")
            ) {
                Text(
                    text = "Target #${activeTestSceneIndex + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Primary Telephoto Shutter Button
            Button(
                onClick = {
                    val captureUseCase = imageCaptureUseCase
                    if (hasCameraPermission && cameraHardwareAvailable && captureUseCase != null) {
                        val dir = File(context.filesDir, "zoom_originals").apply { mkdirs() }
                        val photoFile = File(dir, "cam_${System.currentTimeMillis()}.jpg")
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                        captureUseCase.takePicture(
                            outputOptions,
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    onPhotoCapturedFile(
                                        photoFile,
                                        cameraState.actualZoomRatio,
                                        cameraState.isOpticalOrSensorZoom
                                    )
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    onCaptureTestBenchScene(activeTestSceneIndex)
                                }
                            }
                        )
                    } else {
                        onCaptureTestBenchScene(activeTestSceneIndex)
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GalaxyLaserAmber,
                    contentColor = ObsidianBackground
                ),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("shutter_capture_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Capture Zoomed Photo",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Capture ${String.format("%.1fx", cameraState.actualZoomRatio)} → Enhance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun analyzeLiveFrameTelemetry(
    imageProxy: ImageProxy,
    onResult: (FloatArray, List<Offset>) -> Unit
) {
    try {
        val plane = imageProxy.planes.firstOrNull() ?: return
        val buffer = plane.buffer
        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        val bins = IntArray(32)
        val peakingPoints = ArrayList<Offset>(120)
        val stepX = (width / 40).coerceAtLeast(4)
        val stepY = (height / 40).coerceAtLeast(4)
        var totalSamples = 0

        for (y in stepY until height - stepY step stepY) {
            for (x in stepX until width - stepX step stepX) {
                val idx = y * rowStride + x * pixelStride
                if (idx + rowStride + pixelStride >= buffer.limit() || idx - rowStride < 0) continue

                val luma = buffer.get(idx).toInt() and 0xFF
                val binIdx = (luma * 31) / 255
                bins[binIdx]++
                totalSamples++

                val right = buffer.get(idx + pixelStride).toInt() and 0xFF
                val down = buffer.get(idx + rowStride).toInt() and 0xFF
                val grad = abs(right - luma) + abs(down - luma)
                if (grad > 38 && peakingPoints.size < 120) {
                    peakingPoints.add(Offset(x.toFloat() / width, y.toFloat() / height))
                }
            }
        }

        val maxBin = (bins.maxOrNull() ?: 1).coerceAtLeast(1)
        val normalizedBins = FloatArray(32) { i ->
            (bins[i].toFloat() / maxBin.toFloat()).coerceIn(0.05f, 1.0f)
        }
        onResult(normalizedBins, peakingPoints)
    } catch (_: Exception) {
        // Ignore frame buffer exceptions during camera lifecycle transitions
    } finally {
        imageProxy.close()
    }
}
