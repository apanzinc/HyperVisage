package dev.lackluster.hyperx.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.scene.Scene
import androidx.navigationevent.NavigationEvent
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

@Immutable
class NavTransitionEasing(
    response: Float,
    damping: Float,
) : Easing {
    private val r: Float
    private val w: Float
    private val c2: Float

    init {
        val omega = 2.0 * PI / response
        val k = omega * omega
        val c = damping * 4.0 * PI / response

        w = (sqrt(4.0 * k - c * c) / 2.0).toFloat()
        r = (-c / 2.0).toFloat()
        c2 = r / w
    }

    override fun transform(fraction: Float): Float {
        val t = fraction.toDouble()
        val decay = exp(r * t)
        return (decay * (-cos(w * t) + c2 * sin(w * t)) + 1.0).toFloat()
    }

    fun inverseTransform(fraction: Float, tolerance: Float = 1e-6f): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f

        var low = 0f
        var high = 1f
        var mid = 0f

        repeat(16) {
            mid = (low + high) / 2f
            val value = transform(mid)
            if (abs(value - fraction) < tolerance) return mid

            if (value < fraction) {
                low = mid
            } else {
                high = mid
            }
        }
        return mid
    }
}

object HyperXNavTransitions {
    private val NavAnimationEasing = NavTransitionEasing(0.8f, 0.95f)

    // 注：miuix 曾用一个 navigation3 fork 提供 NavDisplayTransitionEffects（页面切换时的
    // 圆角裁剪、背后场景变暗、切换期间拦截触摸）。该 fork 在 0.9.4 下线，官方
    // androidx.navigation3 的 NavDisplay 没有对应参数，这层特效暂时缺失，过渡动画本身不受影响。

    fun <T : Any> normalTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val slideDirectionSign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            slideInHorizontally(
                initialOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
            ),
            slideOutHorizontally(
                targetOffsetX = { -it / 4 * slideDirectionSign },
                animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
            ),
        )
    }

    fun <T : Any> normalPopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val slideDirectionSign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            slideInHorizontally(
                initialOffsetX = { -it / 4 * slideDirectionSign },
                animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
            ),
            slideOutHorizontally(
                targetOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
            ),
        )
    }

    fun <T : Any> normalPredictivePopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
        val slideDirectionSign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            slideInHorizontally(
                initialOffsetX = { -it / 4 * slideDirectionSign },
                animationSpec = tween(durationMillis = 550, easing = LinearEasing),
            ),
            slideOutHorizontally(
                targetOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = 550, easing = LinearEasing),
            ),
        )
    }

    fun <T : Any> splitTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val slideDirectionSign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            slideInHorizontally(
                initialOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
            ),
            slideOutHorizontally(
                targetOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = 500, easing = NavAnimationEasing),
            )
        )
    }

    fun <T : Any> splitPredictivePopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
        val slideDirectionSign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            slideInHorizontally(
                initialOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = 550, easing = LinearEasing),
            ),
            slideOutHorizontally(
                targetOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = 550, easing = LinearEasing),
            ),
        )
    }
}