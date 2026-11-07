package dev.lackluster.hyperx.ui.effect

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.lackluster.hyperx.ui.layout.LocalHyperXLayoutConfig
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun rememberBlurBackdrop(
    containerColor: Color = MiuixTheme.colorScheme.surface
): LayerBackdrop? {
    val config = LocalHyperXLayoutConfig.current
    // miuix 0.9.4 起模糊管线统一走 RuntimeShader，isRenderEffectSupported 已移除
    if (!config.isBlurEnabled || !isRuntimeShaderSupported()) {
        return null
    }
    return rememberLayerBackdrop {
        drawRect(containerColor)
        drawContent()
    }
}