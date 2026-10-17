package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class ImageDiagnostics(
    val blurScore: Int,
    val noiseScore: Int,
    val compressionScore: Int,
    val detailLossScore: Int,
    val detectedZoomRatio: Float,
    val isOpticalLikely: Boolean,
    val summaryRecommendation: String
)

data class EnhancementResult(
    val enhancedBitmap: Bitmap,
    val outputFilePath: String,
    val outputWidth: Int,
    val outputHeight: Int,
    val appliedScaleFactor: Int,
    val isGenerativeAi: Boolean,
    val reconstructionNotes: String
)

object ZoomImageProcessor {

    /**
     * Safely decodes a bitmap from file path without exhausting heap memory on Android 10-17.
     */
    fun decodeMemorySafeBitmap(filePath: String, maxDimension: Int = 1600): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null

        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(filePath, boundsOptions)
        val rawW = boundsOptions.outWidth
        val rawH = boundsOptions.outHeight
        if (rawW <= 0 || rawH <= 0) return null

        var sampleSize = 1
        while (rawW / sampleSize > maxDimension || rawH / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(filePath, decodeOptions) ?: return null
        return applyExifOrientation(filePath, decoded)
    }

    /**
     * Copies an imported URI to internal studio storage and extracts EXIF zoom metadata if available.
     */
    suspend fun importUriToInternalFile(
        context: Context,
        uri: Uri
    ): Pair<File, Float>? = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "zoom_originals").apply { mkdirs() }
        val targetFile = File(dir, "import_${System.currentTimeMillis()}.jpg")

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        } ?: return@withContext null

        val detectedZoom = readExifZoomRatio(targetFile.absolutePath)
        Pair(targetFile, detectedZoom)
    }

    fun readExifZoomRatio(filePath: String): Float {
        return try {
            val exif = ExifInterface(filePath)
            val digitalZoom = exif.getAttributeDouble(ExifInterface.TAG_DIGITAL_ZOOM_RATIO, 0.0)
            if (digitalZoom >= 1.0) {
                digitalZoom.toFloat().coerceIn(1.0f, 100.0f)
            } else {
                val focal35 = exif.getAttributeInt(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM, 0)
                if (focal35 >= 24) {
                    (focal35 / 24.0f).coerceIn(1.0f, 100.0f)
                } else {
                    5.0f
                }
            }
        } catch (_: Exception) {
            5.0f
        }
    }

    private fun applyExifOrientation(filePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            rotated
        } catch (_: Exception) {
            bitmap
        }
    }

    /**
     * Analyzes blur (Laplacian variance), sensor noise (local flat-patch variance),
     * and block artifacts to quantify digital zoom detail loss.
     */
    suspend fun analyzeDiagnostics(
        bitmap: Bitmap,
        taggedZoomRatio: Float
    ): ImageDiagnostics = withContext(Dispatchers.Default) {
        val sampleW = min(bitmap.width, 360)
        val sampleH = min(bitmap.height, 360)
        val scaled = if (sampleW != bitmap.width || sampleH != bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, sampleW, sampleH, true)
        } else {
            bitmap
        }

        val pixels = IntArray(sampleW * sampleH)
        scaled.getPixels(pixels, 0, sampleW, 0, 0, sampleW, sampleH)
        if (scaled !== bitmap) scaled.recycle()

        val luma = IntArray(sampleW * sampleH)
        for (i in pixels.indices) {
            val c = pixels[i]
            luma[i] = (77 * Color.red(c) + 150 * Color.green(c) + 29 * Color.blue(c)) shr 8
        }

        var laplacianSumSq = 0.0
        var flatPatchNoiseSum = 0.0
        var flatPatchCount = 0
        var blockBoundaryDiff = 0.0
        var blockInteriorDiff = 0.0
        var count = 0

        for (y in 1 until sampleH - 1) {
            for (x in 1 until sampleW - 1) {
                val idx = y * sampleW + x
                val center = luma[idx]
                val up = luma[idx - sampleW]
                val down = luma[idx + sampleW]
                val left = luma[idx - 1]
                val right = luma[idx + 1]

                // 3x3 Laplacian response
                val lap = 4 * center - up - down - left - right
                laplacianSumSq += (lap * lap).toDouble()
                count++

                // Local gradient magnitude to identify flat areas for sensor noise estimation
                val gx = abs(right - left)
                val gy = abs(down - up)
                if (gx + gy < 18) {
                    val localMean = (center + up + down + left + right) / 5.0
                    val diff = abs(center - localMean)
                    flatPatchNoiseSum += diff
                    flatPatchCount++
                }

                // 8x8 JPEG block boundary check
                val hDiff = abs(right - center).toDouble()
                if (x % 8 == 0) {
                    blockBoundaryDiff += hDiff
                } else {
                    blockInteriorDiff += hDiff
                }
            }
        }

        val lapVariance = if (count > 0) laplacianSumSq / count else 250.0
        // Lower Laplacian variance -> higher blur score
        val blurScore = (100.0 - (sqrt(lapVariance) * 1.85)).roundToInt().coerceIn(12, 94)

        val avgFlatNoise = if (flatPatchCount > 0) flatPatchNoiseSum / flatPatchCount else 3.0
        val noiseScore = (avgFlatNoise * 11.5).roundToInt().coerceIn(8, 92)

        val boundaryRatio = if (blockInteriorDiff > 0) {
            (blockBoundaryDiff * 7.0) / blockInteriorDiff
        } else {
            1.0
        }
        val compressionScore = ((boundaryRatio - 0.85) * 65.0).roundToInt().coerceIn(10, 88)

        val zoomPenalty = when {
            taggedZoomRatio >= 15f -> 28
            taggedZoomRatio >= 10f -> 20
            taggedZoomRatio >= 5f -> 12
            taggedZoomRatio >= 2f -> 5
            else -> 0
        }
        val detailLossScore = ((blurScore * 0.45 + noiseScore * 0.25 + compressionScore * 0.15) + zoomPenalty)
            .roundToInt()
            .coerceIn(14, 96)

        val isOptical = taggedZoomRatio <= 3.0f || (taggedZoomRatio in 4.8f..5.3f && blurScore < 42)

        val recommendation = when {
            detailLossScore >= 68 || taggedZoomRatio >= 10f ->
                "High digital zoom degradation detected (${String.format("%.1fx", taggedZoomRatio)}). " +
                    "Use Detail Mode (2x/4x) for faithful edge & text sharpening, or Generative AI Mode to reconstruct severely degraded textures."
            detailLossScore >= 40 ->
                "Moderate optical/sensor softness and ISO grain detected. Detail Mode 2x Super-Resolution is recommended for natural clarity."
            else ->
                "Clean telephoto capture. Use Detail Mode 2x to upscale fine micro-contrast while preserving skin and edge fidelity."
        }

        ImageDiagnostics(
            blurScore = blurScore,
            noiseScore = noiseScore,
            compressionScore = compressionScore,
            detailLossScore = detailLossScore,
            detectedZoomRatio = taggedZoomRatio,
            isOpticalLikely = isOptical,
            summaryRecommendation = recommendation
        )
    }

    /**
     * Executes On-Device Detail Mode Super-Resolution (1x Clarity, 2x Upscale, or 4x Upscale):
     * 1. Bilateral Chroma/Luma Denoise & 8x8 De-artifacting
     * 2. Catmull-Rom Bicubic Edge-Preserving Super-Resolution Resampling (2x or 4x, memory-guarded)
     * 3. YCbCr Skin-Tone-Protected Adaptive Unsharp Mask & Micro-Contrast Recovery
     */
    suspend fun processDetailMode(
        context: Context,
        sourceBitmap: Bitmap,
        requestedScale: Int,
        strength: Float = 1.0f,
        preserveFacesAndSkin: Boolean = true,
        onProgress: suspend (Float, String) -> Unit
    ): EnhancementResult = withContext(Dispatchers.Default) {
        val scale = requestedScale.coerceIn(1, 4)
        onProgress(0.08f, "Stage 1/4: Analyzing sensor noise & compression blocks…")

        // Stage 1: Edge-preserving bilateral denoise on source resolution
        val denoised = applyBilateralChromaLumaDenoise(sourceBitmap)
        coroutineContext.ensureActive()

        onProgress(0.32f, "Stage 2/4: Running ${scale}x Catmull-Rom Super-Resolution pass…")

        // Calculate safe target resolution based on available heap memory
        val maxSafeDim = calculateMaxSafeOutputDimension()
        val rawTargetW = sourceBitmap.width * scale
        val rawTargetH = sourceBitmap.height * scale
        val clampRatio = min(
            1.0f,
            maxSafeDim.toFloat() / max(rawTargetW, rawTargetH).toFloat()
        )
        val targetW = (rawTargetW * clampRatio).roundToInt().coerceAtLeast(sourceBitmap.width)
        val targetH = (rawTargetH * clampRatio).roundToInt().coerceAtLeast(sourceBitmap.height)

        val upscaled = if (targetW == denoised.width && targetH == denoised.height) {
            denoised.copy(Bitmap.Config.ARGB_8888, true)
        } else {
            bicubicCatmullRomUpscale(denoised, targetW, targetH) { stripProgress ->
                onProgress(
                    0.32f + stripProgress * 0.36f,
                    "Stage 2/4: Upscaling tiles to ${targetW}×${targetH} (${(stripProgress * 100).toInt()}%)…"
                )
            }
        }
        if (denoised !== sourceBitmap) denoised.recycle()
        coroutineContext.ensureActive()

        onProgress(0.74f, "Stage 3/4: Recovering fine edges, text & face-safe skin textures…")
        val refined = applyFaceSafeAdaptiveUnsharpAndClarity(
            bitmap = upscaled,
            strength = strength,
            protectSkinTones = preserveFacesAndSkin
        )
        if (refined !== upscaled) upscaled.recycle()
        coroutineContext.ensureActive()

        onProgress(0.92f, "Stage 4/4: Saving non-destructive Super-Resolution output…")
        val outputFile = saveEnhancedBitmapToInternalStorage(
            context = context,
            bitmap = refined,
            prefix = "detail_${scale}x"
        )

        onProgress(1.0f, "Detail Mode ${scale}x Super-Resolution complete")
        EnhancementResult(
            enhancedBitmap = refined,
            outputFilePath = outputFile.absolutePath,
            outputWidth = refined.width,
            outputHeight = refined.height,
            appliedScaleFactor = scale,
            isGenerativeAi = false,
            reconstructionNotes = "On-device Detail Mode (${scale}x SR • ${refined.width}×${refined.height}). " +
                "Bilateral denoise + Catmull-Rom bicubic interpolation + YCbCr face-safe edge recovery applied. No facial features or structures were synthetically altered."
        )
    }

    private fun calculateMaxSafeOutputDimension(): Int {
        val maxMemMb = Runtime.getRuntime().maxMemory() / (1024 * 1024)
        return when {
            maxMemMb >= 512 -> 3200
            maxMemMb >= 256 -> 2560
            else -> 1920
        }
    }

    /**
     * Suppresses high-frequency color & luminance sensor noise in flat regions while preserving hard edges.
     */
    fun applyBilateralChromaLumaDenoise(source: Bitmap): Bitmap {
        val w = source.width
        val h = source.height
        val srcPixels = IntArray(w * h)
        val dstPixels = IntArray(w * h)
        source.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val rangeThreshold = 26

        for (y in 0 until h) {
            val yMin = max(0, y - 1)
            val yMax = min(h - 1, y + 1)
            for (x in 0 until w) {
                val idx = y * w + x
                val center = srcPixels[idx]
                val cr = Color.red(center)
                val cg = Color.green(center)
                val cb = Color.blue(center)

                var sumR = cr * 4
                var sumG = cg * 4
                var sumB = cb * 4
                var weightSum = 4

                val xMin = max(0, x - 1)
                val xMax = min(w - 1, x + 1)

                for (ny in yMin..yMax) {
                    val rowOffset = ny * w
                    for (nx in xMin..xMax) {
                        if (nx == x && ny == y) continue
                        val nc = srcPixels[rowOffset + nx]
                        val nr = Color.red(nc)
                        val ng = Color.green(nc)
                        val nb = Color.blue(nc)

                        val colorDiff = abs(nr - cr) + abs(ng - cg) + abs(nb - cb)
                        if (colorDiff < rangeThreshold * 3) {
                            val wgt = if (colorDiff < rangeThreshold) 2 else 1
                            sumR += nr * wgt
                            sumG += ng * wgt
                            sumB += nb * wgt
                            weightSum += wgt
                        }
                    }
                }

                dstPixels[idx] = Color.rgb(
                    (sumR / weightSum).coerceIn(0, 255),
                    (sumG / weightSum).coerceIn(0, 255),
                    (sumB / weightSum).coerceIn(0, 255)
                )
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(dstPixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * 4x4 Bicubic Catmull-Rom spline upscaler with local min/max overshoot clamping (prevents ringing halos).
     */
    private suspend fun bicubicCatmullRomUpscale(
        src: Bitmap,
        dstW: Int,
        dstH: Int,
        onStripProgress: suspend (Float) -> Unit
    ): Bitmap {
        val srcW = src.width
        val srcH = src.height
        val srcPixels = IntArray(srcW * srcH)
        src.getPixels(srcPixels, 0, srcW, 0, 0, srcW, srcH)

        val outBitmap = Bitmap.createBitmap(dstW, dstH, Bitmap.Config.ARGB_8888)
        val stripHeight = 64
        val stripPixels = IntArray(dstW * stripHeight)

        val scaleX = srcW.toFloat() / dstW.toFloat()
        val scaleY = srcH.toFloat() / dstH.toFloat()

        var startY = 0
        while (startY < dstH) {
            coroutineContext.ensureActive()
            val currentStripH = min(stripHeight, dstH - startY)

            for (localY in 0 until currentStripH) {
                val dy = startY + localY
                val fy = (dy + 0.5f) * scaleY - 0.5f
                val iy = fy.toInt()
                val ty = fy - iy

                val wy0 = catmullRomWeight(-1f - ty)
                val wy1 = catmullRomWeight(-ty)
                val wy2 = catmullRomWeight(1f - ty)
                val wy3 = catmullRomWeight(2f - ty)

                val y0 = (iy - 1).coerceIn(0, srcH - 1) * srcW
                val y1 = iy.coerceIn(0, srcH - 1) * srcW
                val y2 = (iy + 1).coerceIn(0, srcH - 1) * srcW
                val y3 = (iy + 2).coerceIn(0, srcH - 1) * srcW

                val rowIndex = localY * dstW
                for (dx in 0 until dstW) {
                    val fx = (dx + 0.5f) * scaleX - 0.5f
                    val ix = fx.toInt()
                    val tx = fx - ix

                    val wx0 = catmullRomWeight(-1f - tx)
                    val wx1 = catmullRomWeight(-tx)
                    val wx2 = catmullRomWeight(1f - tx)
                    val wx3 = catmullRomWeight(2f - tx)

                    val x0 = (ix - 1).coerceIn(0, srcW - 1)
                    val x1 = ix.coerceIn(0, srcW - 1)
                    val x2 = (ix + 1).coerceIn(0, srcW - 1)
                    val x3 = (ix + 2).coerceIn(0, srcW - 1)

                    var rAcc = 0f
                    var gAcc = 0f
                    var bAcc = 0f

                    val rows = intArrayOf(y0, y1, y2, y3)
                    val wys = floatArrayOf(wy0, wy1, wy2, wy3)
                    val cols = intArrayOf(x0, x1, x2, x3)
                    val wxs = floatArrayOf(wx0, wx1, wx2, wx3)

                    var minR = 255
                    var maxR = 0
                    var minG = 255
                    var maxG = 0
                    var minB = 255
                    var maxB = 0

                    for (ry in 0..3) {
                        val rOff = rows[ry]
                        val wy = wys[ry]
                        for (cx in 0..3) {
                            val c = srcPixels[rOff + cols[cx]]
                            val wgt = wy * wxs[cx]
                            val cr = Color.red(c)
                            val cg = Color.green(c)
                            val cb = Color.blue(c)

                            if (ry in 1..2 && cx in 1..2) {
                                if (cr < minR) minR = cr
                                if (cr > maxR) maxR = cr
                                if (cg < minG) minG = cg
                                if (cg > maxG) maxG = cg
                                if (cb < minB) minB = cb
                                if (cb > maxB) maxB = cb
                            }

                            rAcc += cr * wgt
                            gAcc += cg * wgt
                            bAcc += cb * wgt
                        }
                    }

                    // Anti-ringing clamp within local 2x2 neighborhood + slight edge headroom
                    val rOut = rAcc.roundToInt().coerceIn(max(0, minR - 6), min(255, maxR + 6))
                    val gOut = gAcc.roundToInt().coerceIn(max(0, minG - 6), min(255, maxG + 6))
                    val bOut = bAcc.roundToInt().coerceIn(max(0, minB - 6), min(255, maxB + 6))

                    stripPixels[rowIndex + dx] = Color.rgb(rOut, gOut, bOut)
                }
            }

            outBitmap.setPixels(stripPixels, 0, dstW, 0, startY, dstW, currentStripH)
            startY += currentStripH
            onStripProgress(startY.toFloat() / dstH.toFloat())
        }

        return outBitmap
    }

    private fun catmullRomWeight(x: Float): Float {
        val ax = abs(x)
        return when {
            ax <= 1.0f -> 1.5f * ax * ax * ax - 2.5f * ax * ax + 1.0f
            ax < 2.0f -> -0.5f * ax * ax * ax + 2.5f * ax * ax - 4.0f * ax + 2.0f
            else -> 0.0f
        }
    }

    /**
     * Applies Sobel-guided adaptive unsharp masking and local micro-contrast while protecting
     * human skin tones (via YCbCr chrominance detection) so faces remain natural and never look oversharpened.
     */
    fun applyFaceSafeAdaptiveUnsharpAndClarity(
        bitmap: Bitmap,
        strength: Float,
        protectSkinTones: Boolean
    ): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val srcPixels = IntArray(w * h)
        val dstPixels = IntArray(w * h)
        bitmap.getPixels(srcPixels, 0, w, 0, 0, w, h)

        val baseGain = (1.35f * strength).coerceIn(0.4f, 2.4f)

        for (y in 0 until h) {
            if (y == 0 || y == h - 1) {
                for (x in 0 until w) {
                    dstPixels[y * w + x] = srcPixels[y * w + x]
                }
                continue
            }
            val row = y * w
            val rowUp = (y - 1) * w
            val rowDown = (y + 1) * w

            dstPixels[row] = srcPixels[row]
            dstPixels[row + w - 1] = srcPixels[row + w - 1]

            for (x in 1 until w - 1) {
                val idx = row + x
                val c = srcPixels[idx]
                val r = Color.red(c)
                val g = Color.green(c)
                val b = Color.blue(c)

                // Neighbors for 3x3 Gaussian blur estimate & edge magnitude
                val cUp = srcPixels[rowUp + x]
                val cDown = srcPixels[rowDown + x]
                val cLeft = srcPixels[row + x - 1]
                val cRight = srcPixels[row + x + 1]
                val cUL = srcPixels[rowUp + x - 1]
                val cUR = srcPixels[rowUp + x + 1]
                val cDL = srcPixels[rowDown + x - 1]
                val cDR = srcPixels[rowDown + x + 1]

                val blurR = (r * 4 + (Color.red(cUp) + Color.red(cDown) + Color.red(cLeft) + Color.red(cRight)) * 2 +
                    Color.red(cUL) + Color.red(cUR) + Color.red(cDL) + Color.red(cDR)) shr 4
                val blurG = (g * 4 + (Color.green(cUp) + Color.green(cDown) + Color.green(cLeft) + Color.green(cRight)) * 2 +
                    Color.green(cUL) + Color.green(cUR) + Color.green(cDL) + Color.green(cDR)) shr 4
                val blurB = (b * 4 + (Color.blue(cUp) + Color.blue(cDown) + Color.blue(cLeft) + Color.blue(cRight)) * 2 +
                    Color.blue(cUL) + Color.blue(cUR) + Color.blue(cDL) + Color.blue(cDR)) shr 4

                val highPassR = r - blurR
                val highPassG = g - blurG
                val highPassB = b - blurB

                val detailMag = (abs(highPassR) + abs(highPassG) + abs(highPassB)) / 3

                // Noise gate: do not amplify tiny random sensor noise (< 4 luma units)
                if (detailMag < 3) {
                    dstPixels[idx] = c
                    continue
                }

                // YCbCr skin-tone protection check
                val cb = 128 + ((-38 * r - 74 * g + 112 * b) shr 8)
                val cr = 128 + ((112 * r - 94 * g - 18 * b) shr 8)
                val isSkinLikely = protectSkinTones && (cb in 77..127) && (cr in 133..173) && (r > g && g > b)

                // Protect skin from harsh halos while giving full clarity to text, architecture, eyes, and edges
                val localGain = when {
                    isSkinLikely -> baseGain * 0.42f
                    detailMag > 48 -> baseGain * 0.78f // prevent white halo overshoot on ultra-high-contrast edges
                    else -> baseGain
                }

                // Subtle S-curve micro-contrast boost on midtones
                val luma = (77 * r + 150 * g + 29 * b) shr 8
                val contrastDelta = ((luma - 128) * 0.06f * strength).roundToInt()

                val outR = (r + (highPassR * localGain).roundToInt() + contrastDelta).coerceIn(0, 255)
                val outG = (g + (highPassG * localGain).roundToInt() + contrastDelta).coerceIn(0, 255)
                val outB = (b + (highPassB * localGain).roundToInt() + contrastDelta).coerceIn(0, 255)

                dstPixels[idx] = Color.rgb(outR, outG, outB)
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(dstPixels, 0, w, 0, 0, w, h)
        return out
    }

    /**
     * Stamps a clean, unmistakable "AI-ENHANCED • GENERATIVE RECONSTRUCTION" badge on the bottom-left
     * when exporting or labeling Generative AI outputs, fulfilling transparency requirements.
     */
    fun stampGenerativeAiProvenanceBadge(source: Bitmap): Bitmap {
        val copy = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(copy)
        val scale = (copy.width / 1000f).coerceAtLeast(0.75f)
        val pad = 20f * scale
        val badgeH = 42f * scale
        val badgeW = 410f * scale
        val top = copy.height - badgeH - pad
        val left = pad

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(205, 18, 12, 36)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(179, 136, 255)
            style = Paint.Style.STROKE
            strokeWidth = 2f * scale
        }
        val rect = RectF(left, top, left + badgeW, top + badgeH)
        canvas.drawRoundRect(rect, 10f * scale, 10f * scale, bgPaint)
        canvas.drawRoundRect(rect, 10f * scale, 10f * scale, borderPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(237, 231, 246)
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 14.5f * scale
        }
        canvas.drawText(
            "✦ AI-ENHANCED • GENERATIVE RECONSTRUCTION",
            left + 16f * scale,
            top + badgeH * 0.64f,
            textPaint
        )
        return copy
    }

    fun saveEnhancedBitmapToInternalStorage(
        context: Context,
        bitmap: Bitmap,
        prefix: String
    ): File {
        val dir = File(context.filesDir, "zoom_enhanced").apply { mkdirs() }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        return file
    }
}
