package pl.librus.mobile.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.compositionLocalOf
import kotlin.math.roundToInt

/**
 * Global motion specification managing animation speed multiplier and Material 3 expressive easing curves.
 */
data class MotionSpec(
    val speedMultiplier: Float = 1.0f
) {
    val isAnimationsEnabled: Boolean
        get() = speedMultiplier > 0.05f

    fun duration(baseMillis: Int): Int {
        if (!isAnimationsEnabled) return 0
        return (baseMillis / speedMultiplier).roundToInt().coerceAtLeast(1)
    }

    fun <T> tween(
        baseMillis: Int,
        delayMillis: Int = 0,
        easing: Easing = Emphasized
    ): TweenSpec<T> {
        val dur = duration(baseMillis)
        val del = if (!isAnimationsEnabled) 0 else (delayMillis / speedMultiplier).roundToInt()
        return androidx.compose.animation.core.tween(
            durationMillis = dur,
            delayMillis = del,
            easing = easing
        )
    }

    fun <T> spring(
        dampingRatio: Float = Spring.DampingRatioLowBouncy,
        stiffness: Float = Spring.StiffnessMediumLow
    ): SpringSpec<T> {
        if (!isAnimationsEnabled) {
            return androidx.compose.animation.core.spring(
                dampingRatio = dampingRatio,
                stiffness = Spring.StiffnessHigh * 10f
            )
        }
        val adjustedStiffness = (stiffness * speedMultiplier * speedMultiplier).coerceIn(50f, 100_000f)
        return androidx.compose.animation.core.spring(
            dampingRatio = dampingRatio,
            stiffness = adjustedStiffness
        )
    }

    companion object {
        const val KEY_ANIMATION_SPEED = "saved_animation_speed"
        const val DEFAULT_SPEED = 1.0f

        // Material 3 Expressive Easing Curves
        val Emphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
        val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
        val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)
        val Standard: Easing = FastOutSlowInEasing
        val StandardDecelerate: Easing = LinearOutSlowInEasing
        val StandardAccelerate: Easing = FastOutLinearInEasing

        fun getSpeedLabel(speed: Float, isPl: Boolean): String {
            return when {
                speed <= 0.05f -> if (isPl) "Wyłączone (0x)" else "Off (0x)"
                speed in 0.4f..0.6f -> if (isPl) "Spokojne (0.5x)" else "Relaxed (0.5x)"
                speed in 0.9f..1.1f -> if (isPl) "Standardowe (1.0x)" else "Standard (1.0x)"
                speed in 1.4f..1.6f -> if (isPl) "Szybkie (1.5x)" else "Fast (1.5x)"
                speed >= 1.9f -> if (isPl) "Błyskawiczne (2.0x)" else "Instant (2.0x)"
                else -> "${((speed * 100).roundToInt() / 100.0)}x"
            }
        }
    }
}

val LocalMotionSpec = compositionLocalOf { MotionSpec() }
