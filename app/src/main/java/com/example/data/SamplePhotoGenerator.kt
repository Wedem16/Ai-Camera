package com.example.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object SamplePhotoGenerator {

    data class SampleSpec(
        val title: String,
        val zoomRatio: Float,
        val isOptical: Boolean,
        val generator: (Int, Int) -> Bitmap
    )

    fun getBuiltInSamples(): List<SampleSpec> = listOf(
        SampleSpec(
            title = "10x Heritage Clock Tower & Masonry",
            zoomRatio = 10.0f,
            isOptical = false,
            generator = ::generateClockTowerScene
        ),
        SampleSpec(
            title = "5x Telephoto Portrait (Natural Skin & Eyes)",
            zoomRatio = 5.0f,
            isOptical = true,
            generator = ::generatePortraitScene
        ),
        SampleSpec(
            title = "20x Alpine Observatory Telemetry Sign",
            zoomRatio = 20.0f,
            isOptical = false,
            generator = ::generateObservatorySignScene
        ),
        SampleSpec(
            title = "2x Botanical Leaf Venation Macro",
            zoomRatio = 2.0f,
            isOptical = true,
            generator = ::generateBotanicalMacroScene
        )
    )

    /**
     * Simulates real camera zoom degradation (digital crop interpolation blur + sensor ISO luma/chroma noise)
     * so that on-device Super-Resolution and Detail Mode recover crisp, measurable clarity.
     */
    private fun applyRealisticTelephotoSensorDegradation(
        sharp: Bitmap,
        downsampleFactor: Float,
        noiseAmplitude: Int
    ): Bitmap {
        val w = sharp.width
        val h = sharp.height
        val smallW = (w / downsampleFactor).toInt().coerceAtLeast(120)
        val smallH = (h / downsampleFactor).toInt().coerceAtLeast(120)

        // Simulate digital sensor crop + bilinear interpolation softness
        val sensorCrop = Bitmap.createScaledBitmap(sharp, smallW, smallH, true)
        val upsampled = Bitmap.createScaledBitmap(sensorCrop, w, h, true)
        if (sensorCrop !== sharp) sensorCrop.recycle()

        val pixels = IntArray(w * h)
        upsampled.getPixels(pixels, 0, w, 0, 0, w, h)
        val rng = Random(42)

        for (i in pixels.indices) {
            val c = pixels[i]
            val lumaNoise = rng.nextInt(-noiseAmplitude, noiseAmplitude + 1)
            val chromaNoiseR = rng.nextInt(-noiseAmplitude / 2, noiseAmplitude / 2 + 1)
            val chromaNoiseB = rng.nextInt(-noiseAmplitude / 2, noiseAmplitude / 2 + 1)

            val r = (Color.red(c) + lumaNoise + chromaNoiseR).coerceIn(0, 255)
            val g = (Color.green(c) + lumaNoise).coerceIn(0, 255)
            val b = (Color.blue(c) + lumaNoise + chromaNoiseB).coerceIn(0, 255)
            pixels[i] = Color.rgb(r, g, b)
        }

        // Apply a 3x3 optical diffraction / digital interpolation softness pass
        val blurredPixels = IntArray(w * h)
        for (y in 0 until h) {
            val y0 = (y - 1).coerceAtLeast(0) * w
            val y1 = y * w
            val y2 = (y + 1).coerceAtMost(h - 1) * w
            for (x in 0 until w) {
                val x0 = (x - 1).coerceAtLeast(0)
                val x2 = (x + 1).coerceAtMost(w - 1)
                val c00 = pixels[y0 + x0]; val c01 = pixels[y0 + x]; val c02 = pixels[y0 + x2]
                val c10 = pixels[y1 + x0]; val c11 = pixels[y1 + x]; val c12 = pixels[y1 + x2]
                val c20 = pixels[y2 + x0]; val c21 = pixels[y2 + x]; val c22 = pixels[y2 + x2]

                val r = (Color.red(c00) + Color.red(c01) * 2 + Color.red(c02) +
                    Color.red(c10) * 2 + Color.red(c11) * 4 + Color.red(c12) * 2 +
                    Color.red(c20) + Color.red(c21) * 2 + Color.red(c22)) shr 4
                val g = (Color.green(c00) + Color.green(c01) * 2 + Color.green(c02) +
                    Color.green(c10) * 2 + Color.green(c11) * 4 + Color.green(c12) * 2 +
                    Color.green(c20) + Color.green(c21) * 2 + Color.green(c22)) shr 4
                val b = (Color.blue(c00) + Color.blue(c01) * 2 + Color.blue(c02) +
                    Color.blue(c10) * 2 + Color.blue(c11) * 4 + Color.blue(c12) * 2 +
                    Color.blue(c20) + Color.blue(c21) * 2 + Color.blue(c22)) shr 4
                blurredPixels[y1 + x] = Color.rgb(r, g, b)
            }
        }

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        out.setPixels(blurredPixels, 0, w, 0, 0, w, h)
        upsampled.recycle()
        return out
    }

    private fun generateClockTowerScene(width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Golden hour sky gradient
        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(
                Color.rgb(24, 38, 68),
                Color.rgb(66, 86, 122),
                Color.rgb(198, 134, 88)
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Stone tower body
        val towerLeft = width * 0.16f
        val towerRight = width * 0.84f
        val towerTop = height * 0.10f
        paint.color = Color.rgb(198, 178, 152)
        canvas.drawRect(towerLeft, towerTop, towerRight, height.toFloat(), paint)

        // Fine stone ashlar masonry courses and mortar lines
        val mortarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(132, 115, 94)
            strokeWidth = 2.2f
            style = Paint.Style.STROKE
        }
        val brickH = 26f
        val brickW = 58f
        var row = 0
        var y = towerTop
        while (y < height) {
            canvas.drawLine(towerLeft, y, towerRight, y, mortarPaint)
            val offset = if (row % 2 == 0) 0f else brickW / 2f
            var x = towerLeft + offset
            while (x < towerRight) {
                canvas.drawLine(x, y, x, (y + brickH).coerceAtMost(height.toFloat()), mortarPaint)
                x += brickW
            }
            y += brickH
            row++
        }

        // Architectural cornice and arch
        paint.color = Color.rgb(156, 136, 112)
        canvas.drawRect(towerLeft - 14f, towerTop, towerRight + 14f, towerTop + 34f, paint)

        // Ornate Clock Dial
        val cx = width * 0.5f
        val cy = height * 0.46f
        val radius = width * 0.24f

        paint.color = Color.rgb(52, 44, 38)
        canvas.drawCircle(cx, cy, radius + 14f, paint)
        paint.color = Color.rgb(218, 176, 78)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        canvas.drawCircle(cx, cy, radius + 8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(244, 239, 228)
        canvas.drawCircle(cx, cy, radius, paint)

        // Minute ticks & Roman Numerals
        val numerals = arrayOf("XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI")
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(28, 26, 24)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textSize = radius * 0.18f
        }
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(45, 40, 35)
            strokeWidth = 2.5f
        }

        for (i in 0 until 60) {
            val angle = Math.toRadians((i * 6 - 90).toDouble())
            val inner = if (i % 5 == 0) radius * 0.86f else radius * 0.92f
            val outer = radius * 0.97f
            canvas.drawLine(
                (cx + cos(angle) * inner).toFloat(),
                (cy + sin(angle) * inner).toFloat(),
                (cx + cos(angle) * outer).toFloat(),
                (cy + sin(angle) * outer).toFloat(),
                tickPaint
            )
        }

        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30 - 90).toDouble())
            val tx = (cx + cos(angle) * (radius * 0.70f)).toFloat()
            val ty = (cy + sin(angle) * (radius * 0.70f) + textPaint.textSize * 0.35f).toFloat()
            canvas.drawText(numerals[i], tx, ty, textPaint)
        }

        // Clock hands
        val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(22, 22, 26)
            strokeWidth = 7f
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawLine(cx, cy, cx + radius * 0.42f, cy - radius * 0.28f, handPaint)
        handPaint.strokeWidth = 4.5f
        canvas.drawLine(cx, cy, cx - radius * 0.12f, cy - radius * 0.68f, handPaint)

        // Carved stone inscription plaque below clock
        val plaqueTop = cy + radius + 36f
        paint.color = Color.rgb(74, 66, 58)
        canvas.drawRoundRect(
            RectF(width * 0.25f, plaqueTop, width * 0.75f, plaqueTop + 68f),
            8f, 8f, paint
        )
        textPaint.color = Color.rgb(236, 214, 168)
        textPaint.textSize = 18f
        canvas.drawText("ANNO DOMINI MDCCCLXXXIV", cx, plaqueTop + 28f, textPaint)
        textPaint.textSize = 13f
        canvas.drawText("ROYAL ASTRONOMICAL MERIDIAN TOWER", cx, plaqueTop + 50f, textPaint)

        val degraded = applyRealisticTelephotoSensorDegradation(bmp, downsampleFactor = 2.2f, noiseAmplitude = 11)
        bmp.recycle()
        return degraded
    }

    private fun generatePortraitScene(width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Creamy telephoto bokeh background
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.rgb(34, 52, 48), Color.rgb(64, 88, 76), Color.rgb(42, 58, 66)),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Bokeh circles
        val bokehColors = intArrayOf(
            Color.argb(48, 255, 224, 160),
            Color.argb(38, 180, 235, 210),
            Color.argb(44, 255, 198, 140)
        )
        for (i in 0..14) {
            paint.color = bokehColors[i % bokehColors.size]
            val bx = (width * ((i * 37 % 100) / 100f))
            val by = (height * ((i * 53 % 100) / 100f))
            canvas.drawCircle(bx, by, 28f + (i % 4) * 16f, paint)
        }

        val cx = width * 0.5f
        val cy = height * 0.45f
        val headRx = width * 0.22f
        val headRy = height * 0.27f

        // Back hair volume
        paint.color = Color.rgb(38, 28, 24)
        canvas.drawOval(
            RectF(cx - headRx * 1.25f, cy - headRy * 1.15f, cx + headRx * 1.25f, cy + headRy * 1.35f),
            paint
        )

        // Shoulders & woven linen blazer
        paint.color = Color.rgb(218, 208, 194)
        canvas.drawRoundRect(
            RectF(cx - width * 0.36f, cy + headRy * 0.88f, cx + width * 0.36f, height.toFloat() + 40f),
            64f, 64f, paint
        )
        // Woven fabric texture lines on jacket
        val fabricPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(188, 176, 162)
            strokeWidth = 1.6f
        }
        var fy = (cy + headRy * 0.92f).toInt()
        while (fy < height) {
            canvas.drawLine(cx - width * 0.34f, fy.toFloat(), cx + width * 0.34f, fy.toFloat(), fabricPaint)
            fy += 6
        }

        // Neck with natural skin shading
        paint.color = Color.rgb(212, 164, 136)
        canvas.drawRoundRect(
            RectF(cx - headRx * 0.36f, cy + headRy * 0.65f, cx + headRx * 0.36f, cy + headRy * 1.18f),
            20f, 20f, paint
        )

        // Natural face oval with warm radial skin gradient
        paint.shader = RadialGradient(
            cx - headRx * 0.15f,
            cy - headRy * 0.15f,
            headRy * 1.1f,
            intArrayOf(
                Color.rgb(238, 194, 166),
                Color.rgb(222, 174, 146),
                Color.rgb(196, 146, 118)
            ),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawOval(RectF(cx - headRx, cy - headRy, cx + headRx, cy + headRy), paint)
        paint.shader = null

        // Eyes with detailed iris & catchlights
        val eyeY = cy - headRy * 0.10f
        val eyeOffsetX = headRx * 0.42f
        val eyePositions = floatArrayOf(cx - eyeOffsetX, cx + eyeOffsetX)
        for (ex in eyePositions) {
            paint.color = Color.rgb(248, 246, 244)
            canvas.drawOval(RectF(ex - 24f, eyeY - 11f, ex + 24f, eyeY + 11f), paint)
            // Hazel/brown iris
            paint.color = Color.rgb(98, 68, 44)
            canvas.drawCircle(ex, eyeY, 10.5f, paint)
            // Pupil
            paint.color = Color.rgb(18, 18, 20)
            canvas.drawCircle(ex, eyeY, 5.2f, paint)
            // Catchlight
            paint.color = Color.WHITE
            canvas.drawCircle(ex - 3.5f, eyeY - 3.5f, 2.4f, paint)
        }

        // Eyebrows & eyelashes
        val hairStrandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(46, 34, 28)
            strokeWidth = 2.2f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
        canvas.drawArc(RectF(cx - eyeOffsetX - 28f, eyeY - 26f, cx - eyeOffsetX + 28f, eyeY - 4f), 200f, 140f, false, hairStrandPaint)
        canvas.drawArc(RectF(cx + eyeOffsetX - 28f, eyeY - 26f, cx + eyeOffsetX + 28f, eyeY - 4f), 200f, 140f, false, hairStrandPaint)

        // Nose bridge & soft smile
        hairStrandPaint.color = Color.rgb(178, 128, 102)
        hairStrandPaint.strokeWidth = 2.5f
        canvas.drawLine(cx, eyeY + 6f, cx - 4f, cy + headRy * 0.24f, hairStrandPaint)
        canvas.drawArc(RectF(cx - 14f, cy + headRy * 0.18f, cx + 14f, cy + headRy * 0.30f), 20f, 140f, false, hairStrandPaint)

        // Natural lips
        val lipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(198, 116, 108)
            style = Paint.Style.FILL
        }
        canvas.drawOval(RectF(cx - 32f, cy + headRy * 0.42f, cx + 32f, cy + headRy * 0.56f), lipPaint)

        // Individual hair strands framing face
        hairStrandPaint.color = Color.rgb(54, 40, 32)
        hairStrandPaint.strokeWidth = 2.0f
        for (s in -18..18) {
            val sx = cx + s * (headRx / 18f)
            val path = Path().apply {
                moveTo(sx, cy - headRy * 1.05f)
                quadTo(
                    sx + (if (s < 0) -36f else 36f),
                    cy - headRy * 0.45f,
                    sx + (if (s < 0) -headRx * 0.55f else headRx * 0.55f),
                    cy - headRy * 0.15f
                )
            }
            canvas.drawPath(path, hairStrandPaint)
        }

        val degraded = applyRealisticTelephotoSensorDegradation(bmp, downsampleFactor = 1.85f, noiseAmplitude = 8)
        bmp.recycle()
        return degraded
    }

    private fun generateObservatorySignScene(width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // High-altitude twilight sky
        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(Color.rgb(14, 22, 38), Color.rgb(34, 54, 82), Color.rgb(72, 96, 126)),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Granite mountain ridge
        val ridgePath = Path().apply {
            moveTo(0f, height * 0.65f)
            lineTo(width * 0.22f, height * 0.42f)
            lineTo(width * 0.48f, height * 0.54f)
            lineTo(width * 0.78f, height * 0.36f)
            lineTo(width.toFloat(), height * 0.52f)
            lineTo(width.toFloat(), height.toFloat())
            lineTo(0f, height.toFloat())
            close()
        }
        paint.color = Color.rgb(62, 70, 82)
        canvas.drawPath(ridgePath, paint)

        // Observatory Dome & Telemetry Station Board
        val boardLeft = width * 0.14f
        val boardRight = width * 0.86f
        val boardTop = height * 0.24f
        val boardBottom = height * 0.76f

        paint.color = Color.rgb(232, 236, 242)
        canvas.drawRoundRect(RectF(boardLeft, boardTop, boardRight, boardBottom), 14f, 14f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = Color.rgb(24, 36, 54)
        canvas.drawRoundRect(RectF(boardLeft + 10f, boardTop + 10f, boardRight - 10f, boardBottom - 10f), 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        // Header stripe
        paint.color = Color.rgb(18, 42, 76)
        canvas.drawRect(boardLeft + 14f, boardTop + 14f, boardRight - 14f, boardTop + 78f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(255, 196, 0)
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 23f
        }
        val cx = width * 0.5f
        canvas.drawText("ALPINE OPTICS STATION 3420M", cx, boardTop + 54f, textPaint)

        // Fine resolution calibration text & Siemens star target
        textPaint.color = Color.rgb(22, 28, 36)
        textPaint.textSize = 18f
        canvas.drawText("SECTOR NORTH-04 • LAT 46°32'54\"N", cx, boardTop + 114f, textPaint)
        textPaint.textSize = 15f
        canvas.drawText("HIGH-ALTITUDE INTERFEROMETRY ARRAY", cx, boardTop + 144f, textPaint)

        // Siemens Star Optical Resolution Chart on the sign
        val starCx = width * 0.34f
        val starCy = boardTop + 235f
        val starR = 64f
        val spokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(20, 24, 30)
            style = Paint.Style.FILL
        }
        for (i in 0 until 24) {
            if (i % 2 == 0) {
                canvas.drawArc(
                    RectF(starCx - starR, starCy - starR, starCx + starR, starCy + starR),
                    i * 15f,
                    7.5f,
                    true,
                    spokePaint
                )
            }
        }

        // Line-pair resolution bars on the right half of the sign
        val barStartX = width * 0.54f
        var barY = boardTop + 178f
        for (group in 1..5) {
            val thickness = (7 - group).toFloat()
            for (line in 0..3) {
                canvas.drawRect(
                    barStartX,
                    barY,
                    barStartX + 110f,
                    barY + thickness,
                    spokePaint
                )
                barY += thickness * 2f
            }
            barY += 8f
        }

        val degraded = applyRealisticTelephotoSensorDegradation(bmp, downsampleFactor = 2.65f, noiseAmplitude = 15)
        bmp.recycle()
        return degraded
    }

    private fun generateBotanicalMacroScene(width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Deep forest macro backdrop
        paint.shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(Color.rgb(12, 34, 24), Color.rgb(28, 78, 48), Color.rgb(18, 52, 34)),
            null,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Leaf surface and intricate venation network
        val veinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(142, 224, 118)
            strokeWidth = 4.5f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.STROKE
        }
        // Central midrib
        canvas.drawLine(width * 0.08f, height * 0.88f, width * 0.92f, height * 0.12f, veinPaint)

        // Secondary & tertiary veins
        for (i in 1..9) {
            val t = i / 10f
            val vx = width * (0.08f + 0.84f * t)
            val vy = height * (0.88f - 0.76f * t)
            veinPaint.strokeWidth = 2.6f
            veinPaint.color = Color.rgb(118, 206, 96)
            canvas.drawLine(vx, vy, vx - 120f, vy - 95f, veinPaint)
            canvas.drawLine(vx, vy, vx + 115f, vy + 85f, veinPaint)

            veinPaint.strokeWidth = 1.3f
            veinPaint.color = Color.rgb(92, 178, 76)
            for (sub in 1..4) {
                val sx = vx - 26f * sub
                val sy = vy - 20f * sub
                canvas.drawLine(sx, sy, sx - 22f, sy + 28f, veinPaint)
                canvas.drawLine(sx, sy, sx + 24f, sy - 24f, veinPaint)
            }
        }

        // Water droplets with specular highlights
        val droplets = arrayOf(
            Triple(width * 0.36f, height * 0.42f, 26f),
            Triple(width * 0.62f, height * 0.56f, 34f),
            Triple(width * 0.48f, height * 0.28f, 19f)
        )
        for ((dx, dy, dr) in droplets) {
            paint.color = Color.argb(110, 190, 245, 220)
            canvas.drawCircle(dx, dy, dr, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(dx - dr * 0.3f, dy - dr * 0.3f, dr * 0.22f, paint)
        }

        val degraded = applyRealisticTelephotoSensorDegradation(bmp, downsampleFactor = 1.65f, noiseAmplitude = 6)
        bmp.recycle()
        return degraded
    }
}
