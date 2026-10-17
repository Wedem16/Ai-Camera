package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.EnhancementMode
import com.example.domain.ZoomImageProcessor
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.GalaxyLaserAmber
import com.example.ui.theme.GenerativeViolet
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianDeep
import com.example.ui.theme.OpticCyan
import com.example.ui.theme.TitaniumHighlight
import com.example.ui.theme.Ultra4kEmerald
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun BeforeAfterComparisonView(
    originalBitmap: Bitmap?,
    enhancedBitmap: Bitmap?,
    splitFraction: Float,
    onSplitFractionChange: (Float) -> Unit,
    zoomRatio: Float,
    isOptical: Boolean,
    enhancementMode: EnhancementMode,
    upscaleFactor: Int,
    isGenerativeAiLabeled: Boolean,
    loupeEnabled: Boolean,
    loupePosition: Offset,
    onToggleLoupe: () -> Unit,
    onLoupePositionChange: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val origImageBitmap = remember(originalBitmap) { originalBitmap?.asImageBitmap() }
    val enhImageBitmap = remember(enhancedBitmap) { enhancedBitmap?.asImageBitmap() }
    val is4kMaster = remember(enhancedBitmap, upscaleFactor) {
        upscaleFactor == ZoomImageProcessor.SCALE_4K_UHD ||
            (enhancedBitmap != null && max(enhancedBitmap.width, enhancedBitmap.height) >= 3840)
    }

    val frameBorderBrush = remember(is4kMaster, isGenerativeAiLabeled) {
        when {
            isGenerativeAiLabeled -> Brush.linearGradient(
                listOf(GenerativeViolet, OpticCyan, ChampagneGold)
            )
            is4kMaster -> Brush.linearGradient(
                listOf(GalaxyLaserAmber, OpticCyan, ChampagneGold)
            )
            else -> Brush.linearGradient(
                listOf(TitaniumHighlight, GalaxyLaserAmber.copy(alpha = 0.55f), TitaniumHighlight)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(ObsidianDeep)
            .border(1.5.dp, frameBorderBrush, RoundedCornerShape(22.dp))
            .testTag("before_after_slider")
    ) {
        if (origImageBitmap == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Select or import a zoomed photo to begin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(enhImageBitmap != null, loupeEnabled) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            val h = size.height.toFloat().coerceAtLeast(1f)
                            if (loupeEnabled) {
                                onLoupePositionChange(
                                    Offset(
                                        (change.position.x / w).coerceIn(0.08f, 0.92f),
                                        (change.position.y / h).coerceIn(0.08f, 0.92f)
                                    )
                                )
                            } else {
                                onSplitFractionChange((change.position.x / w).coerceIn(0.02f, 0.98f))
                            }
                        }
                    }
                    .pointerInput(enhImageBitmap != null) {
                        detectTapGestures { offset ->
                            val w = size.width.toFloat().coerceAtLeast(1f)
                            onSplitFractionChange((offset.x / w).coerceIn(0.02f, 0.98f))
                        }
                    }
            ) {
                val canvasW = size.width
                val canvasH = size.height

                val imgAspect = origImageBitmap.width.toFloat() / origImageBitmap.height.toFloat().coerceAtLeast(1f)
                val canvasAspect = canvasW / canvasH.coerceAtLeast(1f)

                val drawW: Int
                val drawH: Int
                if (imgAspect > canvasAspect) {
                    drawW = canvasW.roundToInt()
                    drawH = (canvasW / imgAspect).roundToInt()
                } else {
                    drawH = canvasH.roundToInt()
                    drawW = (canvasH * imgAspect).roundToInt()
                }
                val dstLeft = ((canvasW - drawW) / 2f).roundToInt()
                val dstTop = ((canvasH - drawH) / 2f).roundToInt()
                val dstOffset = IntOffset(dstLeft, dstTop)
                val dstSize = IntSize(drawW, drawH)

                if (enhImageBitmap == null) {
                    drawImage(
                        image = origImageBitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(origImageBitmap.width, origImageBitmap.height),
                        dstOffset = dstOffset,
                        dstSize = dstSize
                    )
                } else {
                    val splitX = (canvasW * splitFraction).coerceIn(0f, canvasW)

                    // Right side: Enhanced 2x/4x/4K UHD output
                    drawImage(
                        image = enhImageBitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(enhImageBitmap.width, enhImageBitmap.height),
                        dstOffset = dstOffset,
                        dstSize = dstSize
                    )

                    // Left side: Clipped Original
                    clipRect(left = 0f, top = 0f, right = splitX, bottom = canvasH) {
                        drawImage(
                            image = origImageBitmap,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(origImageBitmap.width, origImageBitmap.height),
                            dstOffset = dstOffset,
                            dstSize = dstSize
                        )
                    }

                    // Soft golden aura + laser divider line
                    drawLine(
                        color = GalaxyLaserAmber.copy(alpha = 0.28f),
                        start = Offset(splitX, 0f),
                        end = Offset(splitX, canvasH),
                        strokeWidth = 9.dp.toPx()
                    )
                    drawLine(
                        color = ChampagneGold,
                        start = Offset(splitX, 0f),
                        end = Offset(splitX, canvasH),
                        strokeWidth = 2.2.dp.toPx()
                    )

                    // Luxury Titanium & Gold Split Handle
                    val handleCenter = Offset(splitX, canvasH * 0.5f)
                    val handleRadius = 20.dp.toPx()
                    drawCircle(
                        color = ObsidianBackground.copy(alpha = 0.94f),
                        radius = handleRadius,
                        center = handleCenter
                    )
                    drawCircle(
                        color = GalaxyLaserAmber,
                        radius = handleRadius,
                        center = handleCenter,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    val arrowOffset = 6.5.dp.toPx()
                    drawCircle(
                        color = ChampagneGold,
                        radius = 2.6.dp.toPx(),
                        center = Offset(handleCenter.x - arrowOffset, handleCenter.y)
                    )
                    drawCircle(
                        color = OpticCyan,
                        radius = 2.6.dp.toPx(),
                        center = Offset(handleCenter.x + arrowOffset, handleCenter.y)
                    )
                }

                // 3x Side-by-Side Pixel Inspection Loupe
                if (loupeEnabled && originalBitmap != null) {
                    val targetX = (loupePosition.x * canvasW).coerceIn(40f, canvasW - 40f)
                    val targetY = (loupePosition.y * canvasH).coerceIn(40f, canvasH - 40f)

                    drawCircle(
                        color = OpticCyan,
                        radius = 15.dp.toPx(),
                        center = Offset(targetX, targetY),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    val boxW = min(canvasW * 0.44f, 154.dp.toPx())
                    val boxH = 94.dp.toPx()
                    val pad = 10.dp.toPx()
                    val loupeTop = if (loupePosition.y > 0.55f) pad + 40.dp.toPx() else canvasH - boxH - pad

                    val origSrcW = (origImageBitmap.width * 0.12f).roundToInt().coerceAtLeast(16)
                    val origSrcH = (origImageBitmap.height * 0.12f).roundToInt().coerceAtLeast(16)
                    val origSrcX = ((loupePosition.x * origImageBitmap.width).roundToInt() - origSrcW / 2)
                        .coerceIn(0, (origImageBitmap.width - origSrcW).coerceAtLeast(0))
                    val origSrcY = ((loupePosition.y * origImageBitmap.height).roundToInt() - origSrcH / 2)
                        .coerceIn(0, (origImageBitmap.height - origSrcH).coerceAtLeast(0))

                    val leftBoxOffset = IntOffset(pad.roundToInt(), loupeTop.roundToInt())
                    val boxIntSize = IntSize(boxW.roundToInt(), boxH.roundToInt())
                    drawRoundRect(
                        color = ObsidianBackground,
                        topLeft = Offset(pad - 2f, loupeTop - 2f),
                        size = Size(boxW + 4f, boxH + 4f),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                    clipRect(pad, loupeTop, pad + boxW, loupeTop + boxH) {
                        drawImage(
                            image = origImageBitmap,
                            srcOffset = IntOffset(origSrcX, origSrcY),
                            srcSize = IntSize(origSrcW, origSrcH),
                            dstOffset = leftBoxOffset,
                            dstSize = boxIntSize
                        )
                    }
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.7f),
                        topLeft = Offset(pad, loupeTop),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    val activeEnh = enhImageBitmap ?: origImageBitmap
                    val enhSrcW = (activeEnh.width * 0.12f).roundToInt().coerceAtLeast(16)
                    val enhSrcH = (activeEnh.height * 0.12f).roundToInt().coerceAtLeast(16)
                    val enhSrcX = ((loupePosition.x * activeEnh.width).roundToInt() - enhSrcW / 2)
                        .coerceIn(0, (activeEnh.width - enhSrcW).coerceAtLeast(0))
                    val enhSrcY = ((loupePosition.y * activeEnh.height).roundToInt() - enhSrcH / 2)
                        .coerceIn(0, (activeEnh.height - enhSrcH).coerceAtLeast(0))

                    val rightBoxLeft = canvasW - boxW - pad
                    clipRect(rightBoxLeft, loupeTop, rightBoxLeft + boxW, loupeTop + boxH) {
                        drawImage(
                            image = activeEnh,
                            srcOffset = IntOffset(enhSrcX, enhSrcY),
                            srcSize = IntSize(enhSrcW, enhSrcH),
                            dstOffset = IntOffset(rightBoxLeft.roundToInt(), loupeTop.roundToInt()),
                            dstSize = boxIntSize
                        )
                    }
                    drawRoundRect(
                        color = if (isGenerativeAiLabeled) GenerativeViolet else OpticCyan,
                        topLeft = Offset(rightBoxLeft, loupeTop),
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Top Luxury Glassmorphic Telemetry Badges
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    color = ObsidianBackground.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = "BEFORE • ${String.format("%.1fx", zoomRatio)} ${if (isOptical) "OPTICAL" else "DIGITAL"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (originalBitmap != null) {
                            Text(
                                text = "${originalBitmap.width}×${originalBitmap.height} px",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (enhancedBitmap != null) {
                    val accentColor = when {
                        isGenerativeAiLabeled -> GenerativeViolet
                        is4kMaster -> GalaxyLaserAmber
                        else -> OpticCyan
                    }
                    Surface(
                        color = ObsidianBackground.copy(alpha = 0.90f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, accentColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (is4kMaster) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = "4K UHD",
                                        tint = GalaxyLaserAmber,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                } else if (isGenerativeAiLabeled) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "AI Enhanced",
                                        tint = accentColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = when {
                                        isGenerativeAiLabeled && is4kMaster -> "AFTER • 4K UHD AI-ENHANCED"
                                        isGenerativeAiLabeled -> "AFTER • AI-ENHANCED (${upscaleFactor}x)"
                                        is4kMaster -> "AFTER • 4K ULTRA HD SR"
                                        else -> "AFTER • DETAIL ${upscaleFactor}x SR"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                            Text(
                                text = "${enhancedBitmap.width}×${enhancedBitmap.height} px${if (is4kMaster) " • UHD MASTER" else ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (is4kMaster) Ultra4kEmerald else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Bottom Floating Controls
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (enhancedBitmap != null) {
                    IconButton(
                        onClick = {
                            val next = if (splitFraction in 0.45f..0.55f) 0.15f else 0.50f
                            onSplitFractionChange(next)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(ObsidianBackground.copy(alpha = 0.88f), CircleShape)
                            .border(1.dp, GalaxyLaserAmber.copy(alpha = 0.7f), CircleShape)
                            .testTag("reset_split_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Center Before/After Split",
                            tint = GalaxyLaserAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleLoupe,
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (loupeEnabled) OpticCyan.copy(alpha = 0.24f) else ObsidianBackground.copy(alpha = 0.88f),
                            CircleShape
                        )
                        .border(
                            1.dp,
                            if (loupeEnabled) OpticCyan else MaterialTheme.colorScheme.outline,
                            CircleShape
                        )
                        .testTag("toggle_loupe_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Toggle 3x Detail Loupe",
                        tint = if (loupeEnabled) OpticCyan else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
