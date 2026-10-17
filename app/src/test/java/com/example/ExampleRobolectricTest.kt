package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.SamplePhotoGenerator
import com.example.domain.ZoomImageProcessor
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Galaxy Zoom AI", appName)
    }

    @Test
    fun `test 2x and 4x super resolution on real zoomed photographs`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val sampleSpec = SamplePhotoGenerator.getBuiltInSamples().first()
        val zoomedPhoto = sampleSpec.generator(240, 180)

        val beforeDiagnostics = ZoomImageProcessor.analyzeDiagnostics(zoomedPhoto, 10.0f)
        assertTrue(beforeDiagnostics.blurScore in 10..95)
        assertTrue(beforeDiagnostics.detailLossScore in 10..98)

        // Test 2x Super-Resolution Detail Mode
        val result2x = ZoomImageProcessor.processDetailMode(
            context = context,
            sourceBitmap = zoomedPhoto,
            requestedScale = 2,
            strength = 1.2f,
            preserveFacesAndSkin = true,
            onProgress = { _, _ -> }
        )
        assertEquals(480, result2x.outputWidth)
        assertEquals(360, result2x.outputHeight)
        assertTrue(File(result2x.outputFilePath).exists())

        // Verify enhanced photo diagnostics are within valid range and provenance stamp works
        val afterDiagnostics = ZoomImageProcessor.analyzeDiagnostics(result2x.enhancedBitmap, 10.0f)
        assertTrue(afterDiagnostics.blurScore in 10..95)

        val stamped = ZoomImageProcessor.stampGenerativeAiProvenanceBadge(result2x.enhancedBitmap)
        assertEquals(480, stamped.width)
        assertEquals(360, stamped.height)

        // Test 4x Super-Resolution Detail Mode
        val result4x = ZoomImageProcessor.processDetailMode(
            context = context,
            sourceBitmap = zoomedPhoto,
            requestedScale = 4,
            strength = 1.2f,
            preserveFacesAndSkin = true,
            onProgress = { _, _ -> }
        )
        assertEquals(960, result4x.outputWidth)
        assertEquals(720, result4x.outputHeight)
        assertTrue(File(result4x.outputFilePath).exists())
    }
}
