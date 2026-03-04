package com.dynamicisland.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils

class IslandContentRenderer(private val context: Context) {

    private val density = context.resources.displayMetrics.density

    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f * density
        typeface = Typeface.DEFAULT_BOLD
    }

    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3FFFFFF.toInt()
        textSize = 12f * density
    }

    private val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x99FFFFFF.toInt()
        textSize = 11f * density
    }

    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF30D158.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val progressBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33FFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f * density
        strokeCap = Paint.Cap.ROUND
    }

    private val controlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Punch-hole exclusion zone (center of the pill in compact mode)
    private val punchHoleRadiusDp = 20f
    private val punchHoleRadius get() = punchHoleRadiusDp * density

    fun renderCompact(canvas: Canvas, state: IslandState, bounds: RectF) {
        when (state) {
            is IslandState.Notification -> renderNotificationCompact(canvas, state, bounds)
            is IslandState.Music -> renderMusicCompact(canvas, state, bounds)
            is IslandState.PhoneCall -> renderCallCompact(canvas, state, bounds)
            is IslandState.Timer -> renderTimerCompact(canvas, state, bounds)
            else -> {}
        }
    }

    fun renderExpanded(canvas: Canvas, state: IslandState, bounds: RectF) {
        when (state) {
            is IslandState.Notification -> renderNotificationExpanded(canvas, state, bounds)
            is IslandState.Music -> renderMusicExpanded(canvas, state, bounds)
            is IslandState.PhoneCall -> renderCallExpanded(canvas, state, bounds)
            is IslandState.Timer -> renderTimerExpanded(canvas, state, bounds)
            else -> {}
        }
    }

    // --- Notification ---

    private fun renderNotificationCompact(canvas: Canvas, state: IslandState.Notification, bounds: RectF) {
        val padding = 10f * density
        val iconSize = 20f * density
        val centerX = bounds.centerX()

        // Draw icon on the left side (avoiding punch-hole center)
        state.icon?.let { icon ->
            val iconLeft = padding
            val iconTop = bounds.centerY() - iconSize / 2
            icon.setBounds(
                iconLeft.toInt(), iconTop.toInt(),
                (iconLeft + iconSize).toInt(), (iconTop + iconSize).toInt()
            )
            icon.draw(canvas)
        }

        // Draw title on the right side (avoiding punch-hole center)
        val textLeft = centerX + punchHoleRadius + 4f * density
        val textWidth = bounds.right - textLeft - padding
        if (textWidth > 0) {
            val truncated = TextUtils.ellipsize(
                state.title, titlePaint, textWidth, TextUtils.TruncateAt.END
            )
            canvas.drawText(
                truncated.toString(),
                textLeft,
                bounds.centerY() + titlePaint.textSize / 3,
                titlePaint
            )
        }
    }

    private fun renderNotificationExpanded(canvas: Canvas, state: IslandState.Notification, bounds: RectF) {
        val padding = 16f * density
        val iconSize = 24f * density

        // App icon + app name at top
        var y = bounds.top + padding + 24f * density

        state.icon?.let { icon ->
            icon.setBounds(
                padding.toInt(), (y - iconSize).toInt(),
                (padding + iconSize).toInt(), y.toInt()
            )
            icon.draw(canvas)
        }

        canvas.drawText(
            state.appName,
            padding + iconSize + 8f * density,
            y - iconSize / 4,
            subtitlePaint
        )

        // Title
        y += 8f * density
        val titleTruncated = TextUtils.ellipsize(
            state.title, titlePaint,
            bounds.width() - padding * 2, TextUtils.TruncateAt.END
        )
        canvas.drawText(titleTruncated.toString(), padding, y + titlePaint.textSize, titlePaint)

        // Body text (up to 2 lines)
        y += titlePaint.textSize + 8f * density
        val maxTextWidth = bounds.width() - padding * 2
        val lines = state.text.split("\n").take(2)
        for (line in lines) {
            val truncated = TextUtils.ellipsize(
                line, textPaint, maxTextWidth, TextUtils.TruncateAt.END
            )
            canvas.drawText(truncated.toString(), padding, y + textPaint.textSize, textPaint)
            y += textPaint.textSize + 4f * density
        }
    }

    // --- Music ---

    private fun renderMusicCompact(canvas: Canvas, state: IslandState.Music, bounds: RectF) {
        val padding = 8f * density
        val artSize = 22f * density
        val centerX = bounds.centerX()

        // Album art circle on the left
        state.albumArt?.let { art ->
            val artRect = RectF(
                padding, bounds.centerY() - artSize / 2,
                padding + artSize, bounds.centerY() + artSize / 2
            )
            canvas.drawBitmap(art, null, artRect, iconPaint)
        }

        // Music wave / play indicator on the right
        val indicatorX = centerX + punchHoleRadius + 4f * density
        if (state.isPlaying) {
            // Draw simple equalizer bars
            val barWidth = 3f * density
            val barSpacing = 2f * density
            val barHeights = floatArrayOf(8f, 14f, 10f, 16f)
            for (i in barHeights.indices) {
                val x = indicatorX + i * (barWidth + barSpacing)
                val barH = barHeights[i] * density
                canvas.drawRoundRect(
                    x, bounds.centerY() - barH / 2,
                    x + barWidth, bounds.centerY() + barH / 2,
                    barWidth / 2, barWidth / 2,
                    controlPaint
                )
            }
        } else {
            // Draw pause icon
            canvas.drawText("II", indicatorX, bounds.centerY() + textPaint.textSize / 3, titlePaint)
        }
    }

    private fun renderMusicExpanded(canvas: Canvas, state: IslandState.Music, bounds: RectF) {
        val padding = 16f * density
        val artSize = 64f * density

        // Album art on the left
        val artTop = bounds.top + padding + 16f * density
        state.albumArt?.let { art ->
            val artRect = RectF(
                padding, artTop,
                padding + artSize, artTop + artSize
            )
            canvas.drawBitmap(art, null, artRect, iconPaint)
        }

        // Song title and artist on the right
        val textLeft = padding + artSize + 12f * density
        val textWidth = bounds.right - textLeft - padding

        val titleTruncated = TextUtils.ellipsize(
            state.title, titlePaint, textWidth, TextUtils.TruncateAt.END
        )
        canvas.drawText(
            titleTruncated.toString(),
            textLeft, artTop + titlePaint.textSize + 4f * density,
            titlePaint
        )

        val artistTruncated = TextUtils.ellipsize(
            state.artist, textPaint, textWidth, TextUtils.TruncateAt.END
        )
        canvas.drawText(
            artistTruncated.toString(),
            textLeft, artTop + titlePaint.textSize + textPaint.textSize + 12f * density,
            textPaint
        )

        // Progress bar at bottom
        val progressY = bounds.bottom - padding - 24f * density
        val progressLeft = padding
        val progressRight = bounds.right - padding
        canvas.drawLine(progressLeft, progressY, progressRight, progressY, progressBgPaint)

        // Controls: previous, play/pause, next
        val controlY = bounds.bottom - padding - 4f * density
        val controlCenterX = bounds.centerX()
        val controlSpacing = 40f * density

        // Previous
        drawTriangle(canvas, controlCenterX - controlSpacing, controlY, 10f * density, true)
        // Play/Pause
        if (state.isPlaying) {
            // Pause bars
            val barW = 4f * density
            canvas.drawRect(
                controlCenterX - barW - 2 * density, controlY - 10 * density,
                controlCenterX - 2 * density, controlY + 2 * density,
                controlPaint
            )
            canvas.drawRect(
                controlCenterX + 2 * density, controlY - 10 * density,
                controlCenterX + barW + 2 * density, controlY + 2 * density,
                controlPaint
            )
        } else {
            drawTriangle(canvas, controlCenterX, controlY, 12f * density, false)
        }
        // Next
        drawTriangle(canvas, controlCenterX + controlSpacing, controlY, 10f * density, false)
    }

    private fun drawTriangle(canvas: Canvas, cx: Float, cy: Float, size: Float, pointLeft: Boolean) {
        val path = android.graphics.Path()
        if (pointLeft) {
            path.moveTo(cx + size / 2, cy - size / 2)
            path.lineTo(cx - size / 2, cy)
            path.lineTo(cx + size / 2, cy + size / 2)
        } else {
            path.moveTo(cx - size / 2, cy - size / 2)
            path.lineTo(cx + size / 2, cy)
            path.lineTo(cx - size / 2, cy + size / 2)
        }
        path.close()
        canvas.drawPath(path, controlPaint)
    }

    // --- Phone Call ---

    private fun renderCallCompact(canvas: Canvas, state: IslandState.PhoneCall, bounds: RectF) {
        val padding = 10f * density
        val centerX = bounds.centerX()

        // Green phone icon on the left
        val dotRadius = 5f * density
        val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF30D158.toInt() }
        canvas.drawCircle(padding + dotRadius, bounds.centerY(), dotRadius, greenPaint)

        // Caller info on the right
        val text = if (state.isIncoming) "Incoming..." else (state.callerName ?: state.callerNumber)
        val textLeft = centerX + punchHoleRadius + 4f * density
        val textWidth = bounds.right - textLeft - padding
        val truncated = TextUtils.ellipsize(text, titlePaint, textWidth, TextUtils.TruncateAt.END)
        canvas.drawText(
            truncated.toString(),
            textLeft,
            bounds.centerY() + titlePaint.textSize / 3,
            titlePaint
        )
    }

    private fun renderCallExpanded(canvas: Canvas, state: IslandState.PhoneCall, bounds: RectF) {
        val padding = 16f * density
        val centerX = bounds.centerX()

        // Caller name/number
        var y = bounds.top + padding + 32f * density
        val name = state.callerName ?: "Unknown"
        canvas.drawText(name, centerX - titlePaint.measureText(name) / 2, y, titlePaint)

        y += 8f * density
        canvas.drawText(
            state.callerNumber,
            centerX - textPaint.measureText(state.callerNumber) / 2,
            y + textPaint.textSize,
            textPaint
        )

        // Accept/Decline buttons for incoming calls
        if (state.isIncoming) {
            val btnY = bounds.bottom - padding - 20f * density
            val btnRadius = 18f * density

            // Decline (red)
            val redPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF3B30.toInt() }
            canvas.drawCircle(centerX - 50f * density, btnY, btnRadius, redPaint)

            // Accept (green)
            val greenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF30D158.toInt() }
            canvas.drawCircle(centerX + 50f * density, btnY, btnRadius, greenPaint)
        }
    }

    // --- Timer ---

    private fun renderTimerCompact(canvas: Canvas, state: IslandState.Timer, bounds: RectF) {
        val padding = 10f * density
        val text = formatTime(state.remainingMs)
        val centerX = bounds.centerX()

        // Timer icon (circular arc) on the left
        val arcSize = 18f * density
        val arcRect = RectF(
            padding, bounds.centerY() - arcSize / 2,
            padding + arcSize, bounds.centerY() + arcSize / 2
        )
        progressPaint.style = Paint.Style.STROKE
        canvas.drawArc(arcRect, -90f, 360f, false, progressBgPaint)
        canvas.drawArc(arcRect, -90f, 270f, false, progressPaint)

        // Time text on the right
        val textLeft = centerX + punchHoleRadius + 4f * density
        canvas.drawText(
            text, textLeft,
            bounds.centerY() + titlePaint.textSize / 3,
            titlePaint
        )
    }

    private fun renderTimerExpanded(canvas: Canvas, state: IslandState.Timer, bounds: RectF) {
        val padding = 16f * density
        val centerX = bounds.centerX()

        // Large time display
        val timeText = formatTime(state.remainingMs)
        val largePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 32f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            timeText,
            centerX - largePaint.measureText(timeText) / 2,
            bounds.centerY(),
            largePaint
        )

        // Label
        if (state.label.isNotEmpty()) {
            canvas.drawText(
                state.label,
                centerX - textPaint.measureText(state.label) / 2,
                bounds.centerY() + 28f * density,
                textPaint
            )
        }
    }

    private fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes > 0) String.format("%d:%02d", minutes, seconds)
        else String.format("0:%02d", seconds)
    }
}
