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
    /** damping >= 1 时振荡公式退化，必须走临界/过阻尼分支 */
    private val nonOscillating: Boolean
    private val decayRate: Double
    private val normalizer: Double

    init {
        val omega = 2.0 * PI / response
        val k = omega * omega
        val c = damping * 4.0 * PI / response
        val discriminant = 4.0 * k - c * c

        r = (-c / 2.0).toFloat()
        if (discriminant > 1e-6) {
            // 欠阻尼：原来的弹簧曲线
            w = (sqrt(discriminant) / 2.0).toFloat()
            c2 = r / w
            nonOscillating = false
            decayRate = 0.0
            normalizer = 1.0
        } else {
            // 临界/过阻尼：判别式 <= 0，原来的 w=0 会让 c2 = r/w = -Inf，
            // 进而 transform() 返回 NaN（sqrt 负数则直接 NaN）。
            // 动画拿到 NaN 位移会在页面切换时直接崩掉，这里换成单调的
            // 临界阻尼阶跃响应 1 - e^(-a t) * (1 + a t)，并在 t=1 归一化到 1。
            w = 0f
            c2 = 0f
            nonOscillating = true
            decayRate = c / 2.0
            normalizer = (1.0 - exp(-decayRate) * (1.0 + decayRate))
                .takeIf { it > 1e-6 } ?: 1.0
        }
    }

    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        val t = fraction.toDouble()
        val value = if (nonOscillating) {
            (1.0 - exp(-decayRate * t) * (1.0 + decayRate * t)) / normalizer
        } else {
            exp(r * t) * (-cos(w * t) + c2 * sin(w * t)) + 1.0
        }
        // 兜底：任何参数都不能把 NaN / 无穷交给 Compose 的动画系统
        return if (value.isFinite()) value.toFloat().coerceIn(0f, 1f) else fraction
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
    /** 页面进出时长，对齐 HyperOS 的转场节奏；原来 500ms 明显拖沓。 */
    private const val TransitionDuration = 300
    /** 侧滑返回的收尾动画，跟手之后快速落位即可。 */
    private const val PredictivePopDuration = 350

    /** 临界阻尼曲线：快速落位、不回弹、没有长尾巴，且不会像阻尼=1 的旧弹簧公式那样算出 NaN。 */
    private val NavAnimationEasing = NavTransitionEasing(0.5f, 1.0f)

    // 注：miuix 曾用一个 navigation3 fork 提供 NavDisplayTransitionEffects（页面切换时的
    // 圆角裁剪、背后场景变暗、切换期间拦截触摸）。该 fork 在 0.9.4 下线，官方
    // androidx.navigation3 的 NavDisplay 没有对应参数，这层特效暂时缺失，过渡动画本身不受影响。

    fun <T : Any> normalTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val slideDirectionSign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            slideInHorizontally(
                initialOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = TransitionDuration, easing = NavAnimationEasing),
            ),
            slideOutHorizontally(
                targetOffsetX = { -it / 4 * slideDirectionSign },
                animationSpec = tween(durationMillis = TransitionDuration, easing = NavAnimationEasing),
            ),
        )
    }

    fun <T : Any> normalPopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val slideDirectionSign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            slideInHorizontally(
                initialOffsetX = { -it / 4 * slideDirectionSign },
                animationSpec = tween(durationMillis = TransitionDuration, easing = NavAnimationEasing),
            ),
            slideOutHorizontally(
                targetOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = TransitionDuration, easing = NavAnimationEasing),
            ),
        )
    }

    fun <T : Any> normalPredictivePopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
        val slideDirectionSign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            slideInHorizontally(
                initialOffsetX = { -it / 4 * slideDirectionSign },
                animationSpec = tween(durationMillis = PredictivePopDuration, easing = LinearEasing),
            ),
            slideOutHorizontally(
                targetOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = PredictivePopDuration, easing = LinearEasing),
            ),
        )
    }

    fun <T : Any> splitTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val slideDirectionSign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            slideInHorizontally(
                initialOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = TransitionDuration, easing = NavAnimationEasing),
            ),
            slideOutHorizontally(
                targetOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = TransitionDuration, easing = NavAnimationEasing),
            )
        )
    }

    fun <T : Any> splitPredictivePopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
        val slideDirectionSign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            slideInHorizontally(
                initialOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = PredictivePopDuration, easing = LinearEasing),
            ),
            slideOutHorizontally(
                targetOffsetX = { it * slideDirectionSign },
                animationSpec = tween(durationMillis = PredictivePopDuration, easing = LinearEasing),
            ),
        )
    }
}