package dev.lackluster.hyperx.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

data class HyperXLayoutConfig(
    val isSplitScreenEnabled: Boolean = true,
    val isBlurEnabled: Boolean = true,
    /**
     * 柔光玻璃边缘高光（含随设备倾斜的光效）。isBlurEnabled 关闭时不生效。
     * 顶栏/底栏是贴边的矩形，描边会变成一条白色亮线，所以默认关闭；
     * 需要给卡片类组件加玻璃描边时再单独打开。
     */
    val isGlassHighlightEnabled: Boolean = false,
    /** 玻璃底色浓度。渐进模糊要能透出后面的内容，别调太高，0.8 会糊成一块纯色。 */
    val lightBlurAlpha: Float = 0.32f,
    val darkBlurAlpha: Float = 0.28f,
)

enum class PageLayoutMode {
    FULL_SCREEN,
    SPLIT_PRIMARY,
    SPLIT_SECONDARY
}

val LocalHyperXLayoutConfig = compositionLocalOf { HyperXLayoutConfig() }

val LocalPageMode = compositionLocalOf { PageLayoutMode.FULL_SCREEN }

val LocalLayoutPadding = compositionLocalOf { PaddingValues(0.dp) }

/**
 * 页面转场是否正在进行。
 *
 * 下发的是 [State] 引用而不是 Boolean 值：读值的组合只有真正用到它的那一小块
 * （顶栏/底栏的模糊盒子），转场开始/结束时不会让整棵 NavDisplay 跟着重组。
 * draw 阶段直接读 `value` 还能让底色切换只触发重绘，不触发重组。
 *
 * 转场时 AnimatedContent 会同时合成两个场景，如果每个场景都保留
 * `layerBackdrop` + `progressiveTextureBlur`，每帧就要录两份内容纹理、
 * 跑两遍 RuntimeShader。页面据此在转场期间跳过模糊，动画结束后恢复。
 */
val LocalPageTransitionRunning = staticCompositionLocalOf<State<Boolean>> { mutableStateOf(false) }