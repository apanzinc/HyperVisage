// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.lackluster.hyperx.ui.animation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigationevent.NavigationEvent

object HyperXNavTransitions {
    /** 进入：留足可见的滑动时间，但整体不拖沓。 */
    private const val EnterDuration = 350
    /** 返回：比进入更干脆，避免松手后还在慢慢挪。 */
    private const val PopDuration = 250
    /** 侧滑返回松手后的收尾：跟手阶段已经走过大半，只需快速落位。 */
    private const val PredictivePopDuration = 180

    /**
     * 标准 FastOutSlowIn (0.4, 0, 0.2, 1)：起步、中段、收尾的位移分布均匀，
     * 0.5 时约 78%、0.7 时约 94%，整段动画都看得见。
     *
     * 之前手写的阻尼弹簧在 damping=1 时判别式为 0（w=0、c2=-Inf）会算出 NaN；
     * 换成临界阻尼阶跃曲线后虽然不崩了，但 300ms 里前 90ms 就走完 89%，
     * 剩下 210ms 几乎不动 —— 观感就是「进入没有过渡、返回拖着不结束」。
     * 这里不再用手写弹簧，直接用标准曲线。
     */
    private val NavAnimationEasing: Easing = FastOutSlowInEasing
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

    /** 新页面盖在旧页面之上。 */
    private const val PushZIndex = 0f
    /** 返回时让正在滑出的二级页面保持在上层，否则主页会直接盖住整个动画。 */
    private const val PopZIndex = -1f

    fun <T : Any> normalTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ it * sign }, EnterDuration, NavAnimationEasing),
            exitSlide({ -it / 4 * sign }, EnterDuration, NavAnimationEasing),
            targetContentZIndex = PushZIndex,
        )
    }

    fun <T : Any> normalPopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ -it / 4 * sign }, PopDuration, NavAnimationEasing),
            exitSlide({ it * sign }, PopDuration, NavAnimationEasing),
            targetContentZIndex = PopZIndex,
        )
    }

    fun <T : Any> normalPredictivePopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ -it / 4 * sign }, PredictivePopDuration, PredictivePopEasing),
            exitSlide({ it * sign }, PredictivePopDuration, PredictivePopEasing),
            targetContentZIndex = PopZIndex,
        )
    }

    fun <T : Any> splitTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ it * sign }, EnterDuration, NavAnimationEasing),
            exitSlide({ it * sign }, EnterDuration, NavAnimationEasing),
            targetContentZIndex = PushZIndex,
        )
    }

    /** 分屏返回要跟进入反向，之前复用 splitTransitionSpec 导致方向错。 */
    fun <T : Any> splitPopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ -it * sign }, PopDuration, NavAnimationEasing),
            exitSlide({ -it * sign }, PopDuration, NavAnimationEasing),
            targetContentZIndex = PopZIndex,
        )
    }

    fun <T : Any> splitPredictivePopTransitionSpec(layoutDirection: LayoutDirection):
            AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform = {
        val sign = if (layoutDirection == LayoutDirection.Rtl) -1 else 1
        ContentTransform(
            enterSlide({ -it * sign }, PredictivePopDuration, PredictivePopEasing),
            exitSlide({ -it * sign }, PredictivePopDuration, PredictivePopEasing),
            targetContentZIndex = PopZIndex,
        )
    }
}
