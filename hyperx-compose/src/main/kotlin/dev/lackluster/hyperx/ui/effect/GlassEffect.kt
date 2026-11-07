package dev.lackluster.hyperx.ui.effect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.highlight.BloomStroke
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.highlight.LightPosition
import top.yukonga.miuix.kmp.blur.highlight.LightSource
import top.yukonga.miuix.kmp.blur.highlight.rememberTiltLight
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 柔光玻璃（Soft Light Glass）的高光层。
 *
 * HyperOS 4 的柔光玻璃由三部分构成：背景模糊、边缘光晕描边、随设备姿态移动的高光。
 * 系统那一层由小米自己渲染，第三方应用拿不到，这里用 miuix 0.9.4 的 highlight 管线模拟：
 *
 * - 描边用 [BloomStroke] 预设（深浅色主题分开），是圆角矩形 SDF + 半球法线光照；
 * - 主光源用 [rememberTiltLight] 接入重力传感器，倾斜设备时高光会沿边缘移动；
 * - 副光源固定，给对侧留一点轮廓光，避免只有一侧发亮显得单薄。
 *
 * @param enabled 关闭时返回 null，调用方按无高光绘制。
 * @param width 描边带宽，越宽光晕越柔。
 * @param sensitivity 每弧度倾斜对应的高光位移比例。
 */
@Composable
fun rememberGlassHighlight(
    enabled: Boolean = true,
    width: Dp = 0.8.dp,
    sensitivity: Float = 0.08f,
): Highlight? {
    if (!enabled || !isRuntimeShaderSupported()) return null

    val isDark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    val preset = if (isDark) BloomStroke.GlassStrokeMiddleDark else BloomStroke.GlassStrokeMiddleLight

    // 主光跟随设备倾斜，这是柔光玻璃最有辨识度的一层
    val tiltPrimary = rememberTiltLight(
        basePosition = preset.primaryLight.position,
        color = preset.primaryLight.color,
        intensity = preset.primaryLight.intensity,
        sensitivity = sensitivity,
    )

    // 副光固定在下侧，提供基础轮廓
    val secondary = remember(preset) {
        LightSource(
            position = LightPosition(0.5f, 0.8f, -0.5f),
            intensity = preset.secondaryLight.intensity,
        )
    }

    val style = remember(preset, tiltPrimary, secondary) {
        preset.copy(primaryLight = tiltPrimary, secondaryLight = secondary)
    }

    return remember(style, width) { Highlight(width = width, style = style) }
}
