package com.example.ui.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.PhotoRecord
import com.example.ui.theme.ControlPodSurface
import com.example.ui.theme.GalaxyLaserAmber
import com.example.ui.theme.GenerativeViolet
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.OpticCyan
import com.example.ui.theme.StatusOptimal
import java.io.File

@Composable
fun StudioGalleryScreen(
    records: List<PhotoRecord>,
    onOpenInEditor: (PhotoRecord) -> Unit,
    onRevertRecord: (PhotoRecord) -> Unit,
    onDeleteRecord: (PhotoRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column {
            Text(
                text = "NON-DESTRUCTIVE ZOOM ARCHIVE",
                style = MaterialTheme.typography.labelMedium,
                color = GalaxyLaserAmber
            )
            Text(
                text = "Studio Gallery & Provenance Log",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Original captures remain untouched on disk. Revert or re-upscale any zoomed photo at 2x or 4x resolution.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No zoomed photos in Studio yet. Capture from Pro Camera or import a photo.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("gallery_list")
            ) {
                items(records, key = { it.id }) { record ->
                    GalleryPhotoHistoryCard(
                        record = record,
                        onOpenInEditor = { onOpenInEditor(record) },
                        onRevert = { onRevertRecord(record) },
                        onDelete = { onDeleteRecord(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GalleryPhotoHistoryCard(
    record: PhotoRecord,
    onOpenInEditor: () -> Unit,
    onRevert: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when {
        record.isGenerativeAiLabeled -> GenerativeViolet
        record.isEnhanced -> OpticCyan
        else -> GalaxyLaserAmber
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ControlPodSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenInEditor)
            .testTag("gallery_item_card_${record.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 104.dp, height = 82.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ObsidianBackground)
                ) {
                    AsyncImage(
                        model = File(record.enhancedPath ?: record.originalPath),
                        contentDescription = record.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = ObsidianBackground.copy(alpha = 0.86f),
                        shape = RoundedCornerShape(bottomEnd = 8.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                        Text(
                            text = String.format("%.1fx", record.capturedZoomRatio),
                            style = MaterialTheme.typography.labelSmall,
                            color = GalaxyLaserAmber,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (record.isGenerativeAiLabeled) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Enhanced",
                                tint = GenerativeViolet,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = record.parsedMode.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = record.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Original: ${record.originalWidth}×${record.originalHeight} → Output: ${record.enhancedWidth}×${record.enhancedHeight} (${record.upscaleFactor}x)",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (record.isEnhanced) StatusOptimal else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Blur ${record.blurScore}% • Noise ${record.noiseScore}% • Detail Loss ${record.detailLossScore}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (record.aiReconstructionNotes.isNotBlank()) {
                Text(
                    text = record.aiReconstructionNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onOpenInEditor,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GalaxyLaserAmber,
                        contentColor = ObsidianBackground
                    ),
                    modifier = Modifier.testTag("open_editor_button_${record.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (record.isEnhanced) "Inspect Before / After" else "Enhance in Studio",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (record.isEnhanced) {
                        OutlinedButton(
                            onClick = onRevert,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.testTag("revert_record_button_${record.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Revert to Original",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Undo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_record_button_${record.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Photo Record",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
