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
import androidx.compose.ui.graphics.Brush
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
import com.example.domain.ZoomImageProcessor
import com.example.ui.EditorUiState
import com.example.ui.components.BeforeAfterComparisonView
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ControlPodSurface
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.GalaxyLaserAmber
import com.example.ui.theme.GenerativeViolet
import com.example.ui.theme.LuxuryCardBrush
import com.example.ui.theme.LuxuryHeaderBrush
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.OpticCyan
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusOptimal
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TitaniumHighlight
import com.example.ui.theme.Ultra4kEmerald
import java.io.File
import kotlin.math.max

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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Flagship Galaxy S-Ultra Luxury Studio Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(LuxuryHeaderBrush)
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(GalaxyLaserAmber.copy(alpha = 0.7f), TitaniumHighlight, OpticCyan.copy(alpha = 0.6f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = GalaxyLaserAmber.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, GalaxyLaserAmber)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HighQuality,
                                    contentDescription = "4K Ultra HD",
                                    tint = GalaxyLaserAmber,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "PRO VISUAL ENGINE • 4K UHD",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = ChampagneGold
                                )
                            }
                        }
                    }

                    Text(
                        text = activeRecord?.title ?: "Galaxy AI Zoom Studio",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Super-Resolution 2x / 4x / 4K (3840p) • Optical & AI Clarity",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, GalaxyLaserAmber),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier.testTag("import_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = stringResource(R.string.import_photo),
                        tint = ChampagneGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.import_photo),
                        style = MaterialTheme.typography.labelLarge,
                        color = ChampagneGold
                    )
                }
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
                val isRecord4k = max(record.enhancedWidth, record.enhancedHeight) >= 3840 ||
                    record.upscaleFactor == ZoomImageProcessor.SCALE_4K_UHD
                val borderColor = when {
                    isSelected -> GalaxyLaserAmber
                    record.isGenerativeAiLabeled -> GenerativeViolet.copy(alpha = 0.75f)
                    isRecord4k -> Ultra4kEmerald.copy(alpha = 0.8f)
                    record.isEnhanced -> OpticCyan.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.outline
                }
                Box(
                    modifier = Modifier
                        .width(122.dp)
                        .height(78.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(if (isSelected) 2.2.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable { onSelectRecord(record) }
                        .testTag("sample_photo_card_${record.id}")
                ) {
                    AsyncImage(
                        model = File(record.enhancedPath ?: record.originalPath),
                        contentDescription = record.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = ObsidianDeep.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(bottomEnd = 9.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        val badgeLabel = when {
                            isRecord4k && record.isEnhanced -> "${String.format("%.0fx", record.capturedZoomRatio)} • 4K UHD"
                            record.isEnhanced -> "${String.format("%.0fx", record.capturedZoomRatio)} • ${record.upscaleFactor}x SR"
                            else -> "${String.format("%.0fx", record.capturedZoomRatio)} RAW"
                        }
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isRecord4k && record.isEnhanced -> ChampagneGold
                                record.isEnhanced -> OpticCyan
                                else -> GalaxyLaserAmber
                            },
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
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
                .height(310.dp)
        )

        // Interactive Split Slider Bar below image when enhanced
        if (editorState.enhancedBitmap != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LuxuryCardBrush)
                    .border(1.dp, TitaniumHighlight, RoundedCornerShape(14.dp))
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
                        text = "COMPARE SPLIT ${(editorState.splitFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = ChampagneGold
                    )
                    val is4kOut = max(editorState.enhancedBitmap.width, editorState.enhancedBitmap.height) >= 3840
                    Text(
                        text = when {
                            activeRecord?.isGenerativeAiLabeled == true -> "AFTER (AI-ENHANCED) ▶"
                            is4kOut -> "AFTER (4K UHD 3840p) ▶"
                            else -> "AFTER (${activeRecord?.upscaleFactor ?: 2}x SR) ▶"
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
                        thumbColor = ChampagneGold,
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
                shape = RoundedCornerShape(14.dp),
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
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GalaxyLaserAmber),
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
                            color = ChampagneGold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { editorState.processingProgress },
                        color = GalaxyLaserAmber,
                        trackColor = ObsidianDeep,
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
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (editorState.selectedMode == EnhancementMode.GENERATIVE_AI_MODE) {
                        GenerativeViolet
                    } else {
                        GalaxyLaserAmber
                    },
                    contentColor = Color(0xFF0A0C10)
                ),
                contentPadding = PaddingValues(vertical = 15.dp, horizontal = 16.dp),
                modifier = Modifier
                    .weight(1.7f)
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
                    text = "${stringResource(R.string.enhance_zoom_button)} • ${editorState.selectedScaleDisplayLabel}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (activeRecord?.isEnhanced == true) {
                OutlinedButton(
                    onClick = onUndoEnhancementClick,
                    enabled = !editorState.isProcessing,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TitaniumHighlight),
                    contentPadding = PaddingValues(vertical = 15.dp, horizontal = 12.dp),
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
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.2.dp, OpticCyan),
                contentPadding = PaddingValues(vertical = 15.dp, horizontal = 12.dp),
                modifier = Modifier.testTag("export_gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = stringResource(R.string.export_gallery),
                    tint = OpticCyan
                )
            }
        }

        // 6. Luxury Resolution & Enhancement Mode Pod (1x, 2x, 4x, and 4K UHD 3840p)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(LuxuryCardBrush)
                .border(1.dp, TitaniumHighlight, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.HighQuality,
                            contentDescription = null,
                            tint = ChampagneGold,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DUAL AI ENGINE & 4K SUPER-RESOLUTION",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        color = if (editorState.selectedUpscaleFactor == ZoomImageProcessor.SCALE_4K_UHD) {
                            GalaxyLaserAmber.copy(alpha = 0.2f)
                        } else {
                            OpticCyan.copy(alpha = 0.16f)
                        },
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(
                            1.dp,
                            if (editorState.selectedUpscaleFactor == ZoomImageProcessor.SCALE_4K_UHD) GalaxyLaserAmber else OpticCyan
                        )
                    ) {
                        Text(
                            text = if (editorState.selectedUpscaleFactor == ZoomImageProcessor.SCALE_4K_UHD) {
                                "3840p 4K MASTER"
                            } else {
                                "${editorState.selectedUpscaleFactor}x OUTPUT"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (editorState.selectedUpscaleFactor == ZoomImageProcessor.SCALE_4K_UHD) ChampagneGold else OpticCyan,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
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
                        badge = "GEMINI 4K AI",
                        selected = editorState.selectedMode == EnhancementMode.GENERATIVE_AI_MODE,
                        accentColor = GenerativeViolet,
                        onClick = { onSelectMode(EnhancementMode.GENERATIVE_AI_MODE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mode_generative_button")
                    )
                }

                // Output Resolution Selector Cards: 1x, 2x, 4x, and 4K UHD (3840p)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Target Output Resolution",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ResolutionTierCard(
                            title = "1x",
                            subtitle = "Denoise",
                            isSelected = editorState.selectedUpscaleFactor == 1,
                            isUltra4k = false,
                            onClick = { onSelectUpscaleFactor(1) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upscale_1x_button")
                        )
                        ResolutionTierCard(
                            title = "2x SR",
                            subtitle = "HD Clarity",
                            isSelected = editorState.selectedUpscaleFactor == 2,
                            isUltra4k = false,
                            onClick = { onSelectUpscaleFactor(2) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upscale_2x_button")
                        )
                        ResolutionTierCard(
                            title = "4x SR",
                            subtitle = "Quad Scale",
                            isSelected = editorState.selectedUpscaleFactor == 4,
                            isUltra4k = false,
                            onClick = { onSelectUpscaleFactor(4) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("upscale_4x_button")
                        )
                        ResolutionTierCard(
                            title = "4K UHD",
                            subtitle = "3840p Pro",
                            isSelected = editorState.selectedUpscaleFactor == ZoomImageProcessor.SCALE_4K_UHD,
                            isUltra4k = true,
                            onClick = { onSelectUpscaleFactor(ZoomImageProcessor.SCALE_4K_UHD) },
                            modifier = Modifier
                                .weight(1.15f)
                                .testTag("upscale_4k_button")
                        )
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(LuxuryCardBrush)
                    .border(1.dp, TitaniumHighlight, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                text = "ZOOM CLARITY TELEMETRY",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = "${activeRecord.originalWidth}×${activeRecord.originalHeight} → ${activeRecord.enhancedWidth}×${activeRecord.enhancedHeight}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneGold
                        )
                    }

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
                                color = if (isTagged) GalaxyLaserAmber else ObsidianDeep,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (isTagged) ChampagneGold else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .clickable { onUpdateZoomLevelTag(preset) }
                                    .testTag("zoom_tag_${preset.toInt()}x")
                            ) {
                                Text(
                                    text = "${preset.toInt()}x",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTagged) ObsidianDeep else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

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

                    Surface(
                        color = ObsidianDeep,
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
                                tint = ChampagneGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Why Digital Zoom Cannot Recover Uncaptured Optical Detail",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ChampagneGold
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
                    text = "Optical Zoom vs. Digital Zoom & 4K AI Super-Resolution",
                    style = MaterialTheme.typography.titleLarge,
                    color = ChampagneGold
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
                    text = "3. 4K UHD Detail Mode vs. Generative AI Mode: Detail Mode uses deterministic on-device super-resolution (up to 3840p 4K UHD) and bilateral denoising to sharpen existing edges while preserving 100% of original facial identity and text. Generative AI Mode reconstructs plausible micro-textures in heavily degraded areas and explicitly labels the result as AI-Enhanced.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun ResolutionTierCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isUltra4k: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeAccent = if (isUltra4k) GalaxyLaserAmber else OpticCyan
    Surface(
        color = when {
            isSelected && isUltra4k -> GalaxyLaserAmber.copy(alpha = 0.22f)
            isSelected -> OpticCyan.copy(alpha = 0.18f)
            else -> ObsidianDeep
        },
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (isSelected) 1.8.dp else 1.dp,
            if (isSelected) activeAccent else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) activeAccent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected && isUltra4k) ChampagneGold else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
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
        color = if (selected) accentColor.copy(alpha = 0.15f) else ObsidianDeep,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accentColor else MaterialTheme.colorScheme.outline),
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
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
        color = ObsidianDeep,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
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
