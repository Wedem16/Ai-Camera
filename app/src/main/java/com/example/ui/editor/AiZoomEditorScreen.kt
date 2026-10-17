package com.example.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FaceRetouchingNatural
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.EnhancementMode
import com.example.data.PhotoRecord
import com.example.ui.EditorUiState
import com.example.ui.components.BeforeAfterComparisonView
import com.example.ui.theme.ControlPodSurface
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.GalaxyLaserAmber
import com.example.ui.theme.GenerativeViolet
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.OpticCyan
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiZoomEditorScreen(
    editorState: EditorUiState,
    allRecords: List<PhotoRecord>,
    onSelectRecord: (PhotoRecord) -> Unit,
    onImportPhotoUri: (android.net.Uri) -> Unit,
    onSelectMode: (EnhancementMode) -> Unit,
    onSelectUpscaleFactor: (Int) -> Unit,
    onSharpnessStrengthChange: (Float) -> Unit,
    onPreserveFaceIdentityChange: (Boolean) -> Unit,
    onSplitFractionChange: (Float) -> Unit,
    onToggleLoupe: () -> Unit,
    onLoupePositionChange: (androidx.compose.ui.geometry.Offset) -> Unit,
    onUpdateZoomLevelTag: (Float) -> Unit,
    onEnhanceZoomClick: () -> Unit,
    onUndoEnhancementClick: () -> Unit,
    onExportGalleryClick: () -> Unit,
    onTogglePhysicsSheet: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImportPhotoUri(uri)
        }
    }

    val activeRecord = editorState.activeRecord
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Header & Import Action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI SUPER-RESOLUTION STUDIO",
                    style = MaterialTheme.typography.labelMedium,
                    color = GalaxyLaserAmber
                )
                Text(
                    text = activeRecord?.title ?: "Select a Zoomed Photo",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, GalaxyLaserAmber),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("import_photo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = stringResource(R.string.import_photo),
                    tint = GalaxyLaserAmber,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.import_photo),
                    style = MaterialTheme.typography.labelLarge,
                    color = GalaxyLaserAmber
                )
            }
        }

        // 2. Horizontal Filmstrip Selector for Real Zoomed Photos
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(allRecords, key = { it.id }) { record ->
                val isSelected = record.id == activeRecord?.id
                val borderColor = when {
                    isSelected -> GalaxyLaserAmber
                    record.isGenerativeAiLabeled -> GenerativeViolet.copy(alpha = 0.7f)
                    record.isEnhanced -> OpticCyan.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.outline
                }
                Box(
                    modifier = Modifier
                        .width(114.dp)
                        .height(74.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
                        .clickable { onSelectRecord(record) }
                        .testTag("sample_photo_card_${record.id}")
                ) {
                    AsyncImage(
                        model = File(record.enhancedPath ?: record.originalPath),
                        contentDescription = record.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Top-left zoom ratio pill
                    Surface(
                        color = ObsidianBackground.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = "${String.format("%.0fx", record.capturedZoomRatio)}${if (record.isEnhanced) " • ${record.upscaleFactor}x SR" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (record.isEnhanced) OpticCyan else GalaxyLaserAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // 3. Interactive Before-and-After Comparison Slider Canvas
        BeforeAfterComparisonView(
            originalBitmap = editorState.originalBitmap,
            enhancedBitmap = editorState.enhancedBitmap,
            splitFraction = editorState.splitFraction,
            onSplitFractionChange = onSplitFractionChange,
            zoomRatio = activeRecord?.capturedZoomRatio ?: 5.0f,
            isOptical = activeRecord?.isOpticalCapture ?: false,
            enhancementMode = activeRecord?.parsedMode ?: EnhancementMode.NONE,
            upscaleFactor = activeRecord?.upscaleFactor ?: 1,
            isGenerativeAiLabeled = activeRecord?.isGenerativeAiLabeled ?: false,
            loupeEnabled = editorState.magnifierLoupeEnabled,
            loupePosition = editorState.loupeNormalizedPosition,
            onToggleLoupe = onToggleLoupe,
            onLoupePositionChange = onLoupePositionChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )

        // Interactive Split Slider Bar below image when enhanced
        if (editorState.enhancedBitmap != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ControlPodSurface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "◀ BEFORE (ORIGINAL)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "SPLIT ${(editorState.splitFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = GalaxyLaserAmber
                    )
                    Text(
                        text = if (activeRecord?.isGenerativeAiLabeled == true) {
                            "AFTER (AI-ENHANCED) ▶"
                        } else {
                            "AFTER (${activeRecord?.upscaleFactor ?: 2}x SR) ▶"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activeRecord?.isGenerativeAiLabeled == true) GenerativeViolet else OpticCyan
                    )
                }
                Slider(
                    value = editorState.splitFraction,
                    onValueChange = onSplitFractionChange,
                    valueRange = 0.02f..0.98f,
                    colors = SliderDefaults.colors(
                        thumbColor = GalaxyLaserAmber,
                        activeTrackColor = GalaxyLaserAmber,
                        inactiveTrackColor = OpticCyan.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("comparison_slider_control")
                )
            }
        }

        // Status / Provenance Banner
        AnimatedVisibility(visible = editorState.statusMessage != null || activeRecord?.isGenerativeAiLabeled == true) {
            val isGenAi = activeRecord?.isGenerativeAiLabeled == true
            Surface(
                color = if (isGenAi) GenerativeViolet.copy(alpha = 0.16f) else OpticCyan.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, if (isGenAi) GenerativeViolet else OpticCyan),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isGenAi) Icons.Default.AutoAwesome else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (isGenAi) GenerativeViolet else OpticCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        if (isGenAi) {
                            Text(
                                text = stringResource(R.string.generative_ai_badge),
                                style = MaterialTheme.typography.labelMedium,
                                color = GenerativeViolet
                            )
                        }
                        Text(
                            text = editorState.statusMessage ?: activeRecord?.aiReconstructionNotes.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 4. Asynchronous Progress Indicator during Processing
        AnimatedVisibility(visible = editorState.isProcessing) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ElevatedCardSurface),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("processing_progress_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = editorState.processingStageLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(editorState.processingProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = GalaxyLaserAmber
                        )
                    }
                    LinearProgressIndicator(
                        progress = { editorState.processingProgress },
                        color = GalaxyLaserAmber,
                        trackColor = ObsidianBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }

        // 5. Primary Action Bar: ENHANCE ZOOM + Undo + Export
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onEnhanceZoomClick,
                enabled = !editorState.isProcessing && editorState.originalBitmap != null,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (editorState.selectedMode == EnhancementMode.GENERATIVE_AI_MODE) {
                        GenerativeViolet
                    } else {
                        GalaxyLaserAmber
                    },
                    contentColor = Color(0xFF101216)
                ),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 16.dp),
                modifier = Modifier
                    .weight(1.6f)
                    .testTag("enhance_zoom_button")
            ) {
                Icon(
                    imageVector = if (editorState.selectedMode == EnhancementMode.GENERATIVE_AI_MODE) {
                        Icons.Default.AutoAwesome
                    } else {
                        Icons.Default.AutoFixHigh
                    },
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${stringResource(R.string.enhance_zoom_button)} (${editorState.selectedUpscaleFactor}x)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activeRecord?.isEnhanced == true) {
                OutlinedButton(
                    onClick = onUndoEnhancementClick,
                    enabled = !editorState.isProcessing,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    contentPadding = PaddingValues(vertical = 14.dp, horizontal = 12.dp),
                    modifier = Modifier.testTag("undo_enhancement_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = stringResource(R.string.undo_enhancement),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            OutlinedButton(
                onClick = onExportGalleryClick,
                enabled = !editorState.isProcessing && activeRecord != null,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, OpticCyan),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 12.dp),
                modifier = Modifier.testTag("export_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = stringResource(R.string.export_gallery),
                    tint = OpticCyan
                )
            }
        }

        // 6. Two Enhancement Modes Selector (Detail Mode vs Generative AI Mode)
        Card(
            colors = CardDefaults.cardColors(containerColor = ControlPodSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = GalaxyLaserAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ENHANCEMENT MODE & UPSCALING",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${editorState.selectedUpscaleFactor}x OUTPUT",
                        style = MaterialTheme.typography.labelMedium,
                        color = OpticCyan
                    )
                }

                // Mode Cards Side by Side
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    EnhancementModeOptionCard(
                        title = "Detail Mode",
                        subtitle = "On-device SR & edge clarity. Preserves exact faces, skin & text.",
                        badge = "FAITHFUL • ON-DEVICE",
                        selected = editorState.selectedMode == EnhancementMode.DETAIL_MODE,
                        accentColor = OpticCyan,
                        onClick = { onSelectMode(EnhancementMode.DETAIL_MODE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_detail_button")
                    )

                    EnhancementModeOptionCard(
                        title = "Generative AI",
                        subtitle = "Reconstructs heavily degraded zoom crops. Labeled AI-enhanced.",
                        badge = "AI RECONSTRUCT",
                        selected = editorState.selectedMode == EnhancementMode.GENERATIVE_AI_MODE,
                        accentColor = GenerativeViolet,
                        onClick = { onSelectMode(EnhancementMode.GENERATIVE_AI_MODE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_generative_button")
                    )
                }

                // 1x / 2x / 4x Output Resolution Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "AI Super-Resolution Output Scale",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            1 to "1x Clarity",
                            2 to "2x Upscale (SR)",
                            4 to "4x Upscale (Ultra SR)"
                        ).forEach { (scale, label) ->
                            val isSelected = editorState.selectedUpscaleFactor == scale
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectUpscaleFactor(scale) },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = GalaxyLaserAmber.copy(alpha = 0.22f),
                                    selectedLabelColor = GalaxyLaserAmber
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    selectedBorderColor = GalaxyLaserAmber,
                                    borderColor = MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("upscale_${scale}x_button")
                            )
                        }
                    }
                }

                // Face & Skin Identity Protection + Edge Strength
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FaceRetouchingNatural,
                            contentDescription = null,
                            tint = StatusOptimal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Natural Face & Skin Protection",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Prevents artificial facial features & harsh skin halos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = editorState.preserveFaceIdentity,
                        onCheckedChange = onPreserveFaceIdentityChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianBackground,
                            checkedTrackColor = StatusOptimal
                        ),
                        modifier = Modifier.testTag("face_protection_switch")
                    )
                }

                // Micro-Contrast & Edge Recovery Strength
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Texture & Edge Recovery Intensity",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${(editorState.sharpnessStrength * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = OpticCyan
                        )
                    }
                    Slider(
                        value = editorState.sharpnessStrength,
                        onValueChange = onSharpnessStrengthChange,
                        valueRange = 0.6f..1.8f,
                        colors = SliderDefaults.colors(
                            thumbColor = OpticCyan,
                            activeTrackColor = OpticCyan
                        ),
                        modifier = Modifier.testTag("strength_slider")
                    )
                }
            }
        }

        // 7. AI Zoom Clarity Diagnostics & Zoom Level Calibration
        if (activeRecord != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ControlPodSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = OpticCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ZOOM CLARITY DIAGNOSTICS",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${activeRecord.originalWidth}×${activeRecord.originalHeight} → ${activeRecord.enhancedWidth}×${activeRecord.enhancedHeight}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GalaxyLaserAmber
                        )
                    }

                    // Capture Zoom Level Selector Pills (2x, 5x, 10x, 20x, 30x)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Zoom Tag:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        listOf(2.0f, 5.0f, 10.0f, 20.0f, 30.0f).forEach { preset ->
                            val isTagged = kotlin.math.abs(activeRecord.capturedZoomRatio - preset) < 0.4f
                            Surface(
                                color = if (isTagged) GalaxyLaserAmber else ObsidianBackground,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isTagged) GalaxyLaserAmber else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .clickable { onUpdateZoomLevelTag(preset) }
                                    .testTag("zoom_tag_${preset.toInt()}x")
                            ) {
                                Text(
                                    text = "${preset.toInt()}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isTagged) ObsidianBackground else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // 4 Diagnostic Gauges: Blur, Sensor Noise, Compression, Detail Loss
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DiagnosticMetricBox(
                            label = "Blur Index",
                            score = activeRecord.blurScore,
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticMetricBox(
                            label = "ISO Noise",
                            score = activeRecord.noiseScore,
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticMetricBox(
                            label = "Artifacts",
                            score = activeRecord.compressionScore,
                            modifier = Modifier.weight(1f)
                        )
                        DiagnosticMetricBox(
                            label = "Detail Loss",
                            score = activeRecord.detailLossScore,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Optical vs Digital Zoom Physics Explanation Banner
                    Surface(
                        color = ObsidianBackground,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePhysicsSheet(true) }
                            .testTag("digital_zoom_physics_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Optical Physics Notice",
                                tint = GalaxyLaserAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Why Digital Zoom Cannot Recover Uncaptured Optical Detail",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GalaxyLaserAmber
                                )
                                Text(
                                    text = stringResource(R.string.digital_zoom_physics_notice),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editorState.showPhysicsSheet) {
        ModalBottomSheet(
            onDismissRequest = { onTogglePhysicsSheet(false) },
            containerColor = ControlPodSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Optical Zoom vs. Digital Zoom & AI Super-Resolution",
                    style = MaterialTheme.typography.titleLarge,
                    color = GalaxyLaserAmber
                )
                Text(
                    text = "1. Optical & Sensor-Crop Zoom: Uses physical telephoto glass elements (e.g., 3x or 5x periscope optics) or native lossless center-sensor readout to capture real light rays and true spatial frequencies.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "2. Digital Zoom Limitation: Beyond the optical focal length, digital zoom crops and interpolates existing sensor pixels. Digital zoom cannot physically recover optical information that the camera lens and sensor never captured.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "3. Detail Mode vs. Generative AI Mode: Detail Mode uses deterministic on-device super-resolution and bilateral denoising to sharpen existing edges while preserving 100% of original facial identity and text. Generative AI Mode reconstructs plausible micro-textures in heavily degraded areas and explicitly labels the result as AI-Enhanced.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun EnhancementModeOptionCard(
    title: String,
    subtitle: String,
    badge: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (selected) accentColor.copy(alpha = 0.14f) else ObsidianBackground,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accentColor else MaterialTheme.colorScheme.outline),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DiagnosticMetricBox(
    label: String,
    score: Int,
    modifier: Modifier = Modifier
) {
    val indicatorColor = when {
        score >= 65 -> StatusCritical
        score >= 38 -> StatusWarning
        else -> StatusOptimal
    }
    Surface(
        color = ObsidianBackground,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "$score%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = indicatorColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
