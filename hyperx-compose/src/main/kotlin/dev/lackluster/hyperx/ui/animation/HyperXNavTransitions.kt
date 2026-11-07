// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.lackluster.hyperx.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigationevent.NavigationEvent
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 参考项目（以及 miuix navigation3 fork 内部）使用的欠阻尼弹簧曲线。
 *
 * 之前为了减少动画时长把参数从 (0.8, 0.95) 改成了 (0.5, 1.0)，正好落在临界阻尼上，
 * 判别式为 0 → w=0、c2=-Inf → transform() 全程 NaN，页面切换直接崩。
 * 这里恢复原参数，并对判别式非正的情况留一条临界阻尼分支，避免以后再调参数时踩同一个坑。
 */
@Immutable
class NavTransitionEasing(
    response: Float = 0.8f,
    damping: Float = 0.95f,
) : Easing {
    private val r: Float
    private val w: Float
    private val c2: Float
    private val criticallyDamped: Boolean
    private val a: Float
    private val normalizer: Float

    init {
        val omega = 2.0 * PI / response
        val k = omega * omega
        val c = damping * 4.0 * PI / response
        val discriminant = 4.0 * k - c * c

        if (discriminant > 1e-6) {
            criticallyDamped = false
            w = (sqrt(discriminant) / 2.0).toFloat()
            r = (-c / 2.0).toFloat()
            c2 = r / w
            a = 0f
            normalizer = 1f
        } else {
            criticallyDamped = true
            w = 0f
            r = 0f
            c2 = 0f
            a = (c / 2.0).toFloat()
            normalizer = (1.0 - exp(-a.toDouble()) * (1.0 + a)).toFloat().coerceAtLeast(1e-6f)
        }
    }

    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f

        val t = fraction.toDouble()
        val value = if (criticallyDamped) {
            (1.0 - exp(-a * t) * (1.0 + a * t)) / normalizer
        } else {
            exp(r * t) * (-cos(w * t) + c2 * sin(w * t)) + 1.0
        }
        return if (value.isFinite()) value.toFloat() else fraction
    }
}

object HyperXNavTransitions {
    /**
     * 与参考项目、miuix fork 的 defaultTransitionSpec / defaultPopTransitionSpec 完全一致。
     * 曾缩短到 350/250/180ms，配合 FastOutSlowIn 后可见滑动被压进前 100ms，观感是「闪一下」。
     */
    private const val EnterDuration = 500
    private const val PopDuration = 500
    /**
     * 跟手阶段线性映射手指进度；松手后 fork 会按 transition.totalDurationNanos
     * 和当前 fraction 折算剩余时长，所以这里保持原项目的 550ms。
     */
    private const val PredictivePopDuration = 550

    /** 原项目的欠阻尼弹簧，(0.8, 0.95) 判别式 > 0，不会算出 NaN。 */
    private val NavAnimationEasing: Easing = NavTransitionEasing(0.8f, 0.95f)
    /** 预测式返回跟手阶段要线性映射手指进度，不能用带缓动的曲线。 */
    private val PredictivePopEasing: Easing = LinearEasing

    // 页面切换的并排中间态由 miuix 的 navigation3 fork 提供：圆角裁剪、背后场景变暗、
    // 切换期间拦截触摸、跟手返回的方向处理。0.9.4 只是不再发布这个 artifact，
    // 工程仍保留 0.9.0 版本，对应 NavDisplay 多一个 transitionEffects 参数。
    /** 全屏：原项目默认特效。 */
    val NormalTransitionEffects = NavDisplayTransitionEffects.Default
    /** 分屏：右栏不裁圆角，变暗 0.5，切换期间拦截输入，返回方向不跟手性边缘。 */
    val SplitTransitionEffects = NavDisplayTransitionEffects(
        enableCornerClip = false,
        dimAmount = 0.5f,
        blockInputDuringTransition = true,
        popDirectionFollowsSwipeEdge = false,
    )

    private fun enterSlide(offset: (Int) -> Int, duration: Int, easing: Easing): EnterTransition =
        slideInHorizontally(
            initialOffsetX = offset,
            animationSpec = tween(durationMillis = duration, easing = easing),
        )

    private fun exitSlide(offset: (Int) -> Int, duration: Int, easing: Easing): ExitTransition =
        slideOutHorizontally(
            targetOffsetX = offset,
            animationSpec = tween(durationMillis = duration, easing = easing),
        )

    // fork 的 NavDisplay 会用自己算出的 targetZIndex 覆盖 ContentTransform.targetContentZIndex
    //（pop 时 target 场景 = initialZIndex - 1），所以这里不需要、也不应该再设 z-index。

    fun <T : Any> normalTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ it * sign }, EnterDuration, NavAnimationEasing),
            exitSlide({ -it / 4 * sign }, EnterDuration, NavAnimationEasing),
        )
    }

    fun <T : Any> normalPopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ -it / 4 * sign }, PopDuration, NavAnimationEasing),
            exitSlide({ it * sign }, PopDuration, NavAnimationEasing),
        )
    }

    fun <T : Any> normalPredictivePopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ -it / 4 * sign }, PredictivePopDuration, PredictivePopEasing),
            exitSlide({ it * sign }, PredictivePopDuration, PredictivePopEasing),
        )
    }

    fun <T : Any> splitTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ it * sign }, EnterDuration, NavAnimationEasing),
            exitSlide({ it * sign }, EnterDuration, NavAnimationEasing),
        )
    }

    /** 分屏返回要跟进入反向，原项目复用 splitTransitionSpec 导致方向错。 */
    fun <T : Any> splitPopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ -it * sign }, PopDuration, NavAnimationEasing),
            exitSlide({ -it * sign }, PopDuration, NavAnimationEasing),
        )
    }

    fun <T : Any> splitPredictivePopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ -it * sign }, PredictivePopDuration, PredictivePopEasing),
            exitSlide({ -it * sign }, PredictivePopDuration, PredictivePopEasing),
        )
    }
}
