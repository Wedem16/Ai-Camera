package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.StudioDestination
import com.example.ui.ZoomStudioViewModel
import com.example.ui.camera.ProCameraScreen
import com.example.ui.editor.AiZoomEditorScreen
import com.example.ui.gallery.StudioGalleryScreen
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.GalaxyLaserAmber
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBackground

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                GalaxyZoomApp()
            }
        }
    }
}

@Composable
fun GalaxyZoomApp(
    viewModel: ZoomStudioViewModel = viewModel(
        factory = ZoomStudioViewModel.provideFactory(LocalContext.current)
    )
) {
    val currentDestination by viewModel.currentDestination.collectAsStateWithLifecycle()
    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val allRecords by viewModel.allPhotoRecords.collectAsStateWithLifecycle()

    if (currentDestination != StudioDestination.AI_ZOOM_EDITOR) {
        BackHandler {
            viewModel.navigateTo(StudioDestination.AI_ZOOM_EDITOR)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        val isExpandedScreen = maxWidth >= 640.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianBackground,
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = CharcoalSurface,
                        tonalElevation = 4.dp
                    ) {
                        NavigationBarItem(
                            selected = currentDestination == StudioDestination.PRO_CAMERA,
                            onClick = { viewModel.navigateTo(StudioDestination.PRO_CAMERA) },
                            icon = {
                                Icon(
                                    imageVector = if (currentDestination == StudioDestination.PRO_CAMERA) {
                                        Icons.Filled.CameraAlt
                                    } else {
                                        Icons.Outlined.CameraAlt
                                    },
                                    contentDescription = stringResource(R.string.tab_camera)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_camera)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianBackground,
                                selectedTextColor = GalaxyLaserAmber,
                                indicatorColor = GalaxyLaserAmber
                            ),
                            modifier = Modifier.testTag("nav_tab_camera")
                        )

                        NavigationBarItem(
                            selected = currentDestination == StudioDestination.AI_ZOOM_EDITOR,
                            onClick = { viewModel.navigateTo(StudioDestination.AI_ZOOM_EDITOR) },
                            icon = {
                                Icon(
                                    imageVector = if (currentDestination == StudioDestination.AI_ZOOM_EDITOR) {
                                        Icons.Filled.AutoFixHigh
                                    } else {
                                        Icons.Outlined.AutoFixHigh
                                    },
                                    contentDescription = stringResource(R.string.tab_studio)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_studio)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianBackground,
                                selectedTextColor = GalaxyLaserAmber,
                                indicatorColor = GalaxyLaserAmber
                            ),
                            modifier = Modifier.testTag("nav_tab_studio")
                        )

                        NavigationBarItem(
                            selected = currentDestination == StudioDestination.HISTORY_GALLERY,
                            onClick = { viewModel.navigateTo(StudioDestination.HISTORY_GALLERY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentDestination == StudioDestination.HISTORY_GALLERY) {
                                        Icons.Filled.PhotoLibrary
                                    } else {
                                        Icons.Outlined.PhotoLibrary
                                    },
                                    contentDescription = stringResource(R.string.tab_gallery)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_gallery)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = ObsidianBackground,
                                selectedTextColor = GalaxyLaserAmber,
                                indicatorColor = GalaxyLaserAmber
                            ),
                            modifier = Modifier.testTag("nav_tab_gallery")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = CharcoalSurface
                    ) {
                        NavigationRailItem(
                            selected = currentDestination == StudioDestination.PRO_CAMERA,
                            onClick = { viewModel.navigateTo(StudioDestination.PRO_CAMERA) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = stringResource(R.string.tab_camera)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_camera)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ObsidianBackground,
                                selectedTextColor = GalaxyLaserAmber,
                                indicatorColor = GalaxyLaserAmber
                            ),
                            modifier = Modifier.testTag("nav_rail_camera")
                        )

                        NavigationRailItem(
                            selected = currentDestination == StudioDestination.AI_ZOOM_EDITOR,
                            onClick = { viewModel.navigateTo(StudioDestination.AI_ZOOM_EDITOR) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.AutoFixHigh,
                                    contentDescription = stringResource(R.string.tab_studio)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_studio)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ObsidianBackground,
                                selectedTextColor = GalaxyLaserAmber,
                                indicatorColor = GalaxyLaserAmber
                            ),
                            modifier = Modifier.testTag("nav_rail_studio")
                        )

                        NavigationRailItem(
                            selected = currentDestination == StudioDestination.HISTORY_GALLERY,
                            onClick = { viewModel.navigateTo(StudioDestination.HISTORY_GALLERY) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = stringResource(R.string.tab_gallery)
                                )
                            },
                            label = { Text(stringResource(R.string.tab_gallery)) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = ObsidianBackground,
                                selectedTextColor = GalaxyLaserAmber,
                                indicatorColor = GalaxyLaserAmber
                            ),
                            modifier = Modifier.testTag("nav_rail_gallery")
                        )
                    }
                }

                when (currentDestination) {
                    StudioDestination.PRO_CAMERA -> {
                        ProCameraScreen(
                            cameraState = cameraState,
                            onCameraZoomUpdated = viewModel::onCameraZoomStateUpdated,
                            onSetTargetZoom = viewModel::setSimulatedOrTargetZoomRatio,
                            onToggleFocusPeaking = viewModel::toggleFocusPeaking,
                            onToggleHistogram = viewModel::toggleLiveHistogram,
                            onTogglePipLock = viewModel::togglePipZoomLock,
                            onLiveTelemetryAnalyzed = viewModel::updateLiveFrameTelemetry,
                            onPhotoCapturedFile = viewModel::onCameraPhotoCapturedFile,
                            onCaptureTestBenchScene = viewModel::captureProTestBenchPhoto,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    StudioDestination.AI_ZOOM_EDITOR -> {
                        AiZoomEditorScreen(
                            editorState = editorState,
                            allRecords = allRecords,
                            onSelectRecord = { viewModel.selectPhotoRecord(it) },
                            onImportPhotoUri = viewModel::importPhotoFromPicker,
                            onSelectMode = viewModel::setEnhancementMode,
                            onSelectUpscaleFactor = viewModel::setUpscaleFactor,
                            onSharpnessStrengthChange = viewModel::setSharpnessStrength,
                            onPreserveFaceIdentityChange = viewModel::setPreserveFaceIdentity,
                            onSplitFractionChange = viewModel::setSplitFraction,
                            onToggleLoupe = viewModel::toggleMagnifierLoupe,
                            onLoupePositionChange = viewModel::updateLoupePosition,
                            onUpdateZoomLevelTag = viewModel::updatePhotoZoomLevelTag,
                            onEnhanceZoomClick = viewModel::runEnhanceZoom,
                            onUndoEnhancementClick = viewModel::undoEnhancement,
                            onExportGalleryClick = viewModel::exportActivePhotoToGallery,
                            onTogglePhysicsSheet = viewModel::setShowPhysicsSheet,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    StudioDestination.HISTORY_GALLERY -> {
                        StudioGalleryScreen(
                            records = allRecords,
                            onOpenInEditor = { record ->
                                viewModel.selectPhotoRecord(record, openEditor = true)
                            },
                            onRevertRecord = { record ->
                                viewModel.selectPhotoRecord(record)
                                viewModel.undoEnhancement()
                            },
                            onDeleteRecord = viewModel::deletePhotoRecord,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
