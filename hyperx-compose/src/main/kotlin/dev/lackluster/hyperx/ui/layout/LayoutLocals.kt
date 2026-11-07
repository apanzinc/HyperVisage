package dev.lackluster.hyperx.ui.layout

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.compositionLocalOf
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