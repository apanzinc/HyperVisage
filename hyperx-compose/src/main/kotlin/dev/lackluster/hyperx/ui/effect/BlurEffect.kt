package dev.lackluster.hyperx.ui.effect

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.lackluster.hyperx.ui.layout.LocalHyperXLayoutConfig
import dev.lackluster.hyperx.ui.layout.LocalPageTransitionRunning
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberBlurBackdrop(
    containerColor: Color = MiuixTheme.colorScheme.surface
): LayerBackdrop? {
    val config = LocalHyperXLayoutConfig.current
    val transitionState = LocalPageTransitionRunning.current
    // miuix 0.9.4 起模糊管线统一走 RuntimeShader，isRenderEffectSupported 已移除
    if (!config.isBlurEnabled || !isRuntimeShaderSupported()) {
        return null
    }
    return rememberLayerBackdrop {
        drawRect(containerColor)
        // 转场时顶栏的 drawBackdrop 已经 enabled=false，这份离屏纹理没人采样。
        // 两个场景同时合成时，这里少重画一遍整页内容是最直接的一笔节省。
        // 屏幕上的内容由 LayerBackdropNode.draw() 的 drawContent() 正常绘制，不受影响。
        if (!transitionState.value) {
            drawContent()
        }
    }
}
