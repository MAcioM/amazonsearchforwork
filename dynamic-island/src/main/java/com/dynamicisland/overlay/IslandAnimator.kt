package com.dynamicisland.overlay

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.animation.TimeInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp

class IslandAnimator {

    private var currentAnimator: ValueAnimator? = null

    var currentWidth = 0f
        private set
    var currentHeight = 0f
        private set
    var currentCornerRadius = 0f
        private set
    var currentAlpha = 0f
        private set

    fun animateTo(
        targetWidth: Float,
        targetHeight: Float,
        targetCornerRadius: Float,
        targetAlpha: Float,
        durationMs: Long = 300L,
        onUpdate: (width: Float, height: Float, cornerRadius: Float, alpha: Float) -> Unit,
        onEnd: (() -> Unit)? = null
    ) {
        currentAnimator?.cancel()

        val startWidth = currentWidth
        val startHeight = currentHeight
        val startRadius = currentCornerRadius
        val startAlpha = currentAlpha

        currentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = SpringInterpolator(0.75f)

            addUpdateListener { anim ->
                val fraction = anim.animatedFraction
                currentWidth = lerp(startWidth, targetWidth, fraction)
                currentHeight = lerp(startHeight, targetHeight, fraction)
                currentCornerRadius = lerp(startRadius, targetCornerRadius, fraction)
                currentAlpha = lerp(startAlpha, targetAlpha, fraction)
                onUpdate(currentWidth, currentHeight, currentCornerRadius, currentAlpha)
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd?.invoke()
                }
            })

            start()
        }
    }

    fun setImmediate(width: Float, height: Float, cornerRadius: Float, alpha: Float) {
        currentAnimator?.cancel()
        currentWidth = width
        currentHeight = height
        currentCornerRadius = cornerRadius
        currentAlpha = alpha
    }

    fun cancel() {
        currentAnimator?.cancel()
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction
}

class SpringInterpolator(private val damping: Float) : TimeInterpolator {
    override fun getInterpolation(t: Float): Float {
        return (1.0 - exp(-damping * t * 10.0) * cos(t * PI * 2)).toFloat()
            .coerceIn(0f, 1f)
    }
}
