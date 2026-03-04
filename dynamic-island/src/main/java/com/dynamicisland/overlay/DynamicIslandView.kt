package com.dynamicisland.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.dynamicisland.settings.IslandPreferences

class DynamicIslandView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = context.resources.displayMetrics.density
    private val animator = IslandAnimator()
    private val contentRenderer = IslandContentRenderer(context)
    private val prefs = IslandPreferences(context)

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = prefs.getIslandColor()
        style = Paint.Style.FILL
    }

    private val backgroundRect = RectF()
    private var currentState: IslandState = IslandState.Idle
    private var isExpanded = false

    var onExpandRequest: (() -> Unit)? = null
    var onCollapseRequest: (() -> Unit)? = null

    init {
        // Set initial dimensions to compact size
        val compactW = dpToPx(prefs.getIslandWidthDp())
        val compactH = dpToPx(prefs.getIslandHeightDp())
        val cornerR = compactH / 2f
        animator.setImmediate(compactW, compactH, cornerR, 0f)
    }

    fun updateFromPreferences() {
        backgroundPaint.color = prefs.getIslandColor()
        invalidate()
    }

    fun setState(state: IslandState) {
        currentState = state
        if (state is IslandState.Idle) {
            animateToHidden()
        } else {
            animateToCompact()
        }
    }

    fun expand() {
        if (currentState is IslandState.Idle) return
        isExpanded = true
        val targetW = dpToPx(prefs.getExpandedWidthDp())
        val targetH = dpToPx(prefs.getExpandedHeightDp())
        val cornerR = dpToPx(24f)
        val duration = prefs.getAnimationDurationMs()

        animator.animateTo(
            targetWidth = targetW,
            targetHeight = targetH,
            targetCornerRadius = cornerR,
            targetAlpha = 1f,
            durationMs = duration,
            onUpdate = { _, _, _, _ -> invalidate() }
        )
    }

    fun collapse() {
        isExpanded = false
        animateToCompact()
    }

    private fun animateToCompact() {
        val targetW = dpToPx(prefs.getIslandWidthDp())
        val targetH = dpToPx(prefs.getIslandHeightDp())
        val cornerR = targetH / 2f
        val duration = prefs.getAnimationDurationMs()

        animator.animateTo(
            targetWidth = targetW,
            targetHeight = targetH,
            targetCornerRadius = cornerR,
            targetAlpha = 1f,
            durationMs = duration,
            onUpdate = { _, _, _, _ -> invalidate() }
        )
    }

    private fun animateToHidden() {
        isExpanded = false
        val compactW = dpToPx(prefs.getIslandWidthDp())
        val compactH = dpToPx(prefs.getIslandHeightDp())
        val cornerR = compactH / 2f
        val duration = prefs.getAnimationDurationMs()

        animator.animateTo(
            targetWidth = compactW,
            targetHeight = compactH,
            targetCornerRadius = cornerR,
            targetAlpha = 0f,
            durationMs = duration,
            onUpdate = { _, _, _, _ -> invalidate() }
        )
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Fixed maximum size to avoid constant layout changes
        val maxW = dpToPx(prefs.getExpandedWidthDp()).toInt() + dpToPx(8f).toInt()
        val maxH = dpToPx(prefs.getExpandedHeightDp()).toInt() + dpToPx(8f).toInt()
        setMeasuredDimension(maxW, maxH)
    }

    override fun onDraw(canvas: Canvas) {
        if (animator.currentAlpha <= 0.01f) return

        val w = animator.currentWidth
        val h = animator.currentHeight
        val r = animator.currentCornerRadius

        // Center the pill horizontally within the view
        val left = (width - w) / 2f
        val top = 0f

        backgroundRect.set(left, top, left + w, top + h)
        backgroundPaint.alpha = (animator.currentAlpha * 255).toInt()
        canvas.drawRoundRect(backgroundRect, r, r, backgroundPaint)

        // Draw content
        if (animator.currentAlpha > 0.5f) {
            canvas.save()
            canvas.translate(left, top)
            val contentBounds = RectF(0f, 0f, w, h)
            if (isExpanded) {
                contentRenderer.renderExpanded(canvas, currentState, contentBounds)
            } else {
                contentRenderer.renderCompact(canvas, currentState, contentBounds)
            }
            canvas.restore()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            if (currentState is IslandState.Idle) return false

            // Check if touch is within the pill bounds
            val touchX = event.x
            val touchY = event.y
            if (backgroundRect.contains(touchX, touchY)) {
                if (isExpanded) {
                    onCollapseRequest?.invoke()
                } else {
                    onExpandRequest?.invoke()
                }
                performClick()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun dpToPx(dp: Float): Float = dp * density
}
