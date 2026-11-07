package dev.lackluster.hyperx.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import dev.lackluster.hyperx.ui.effect.rememberBlurBackdrop
import dev.lackluster.hyperx.ui.effect.rememberGlassHighlight
import top.yukonga.miuix.kmp.basic.FabPosition
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun HyperXScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable ((contentPadding: PaddingValues) -> Unit)? = null,
    bottomBar: @Composable ((contentPadding: PaddingValues) -> Unit)? = null,
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    snackbarHost: @Composable () -> Unit = {},
    containerColor: Color = MiuixTheme.colorScheme.surface,
    contentWindowInsets: WindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Vertical),
    blurTopBar: Boolean = false,
    topBarBlurFractionProvider: () -> Float = { 1f },
    blurBottomBar: Boolean = false,
    bottomBarBlurFractionProvider: () -> Float = { 1f },
    blurTintAlpha: Float = 0.32f,
    layoutPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (PaddingValues) -> Unit
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    val maxTopBlurRadius = with(density) { 25.dp.toPx() }
    val maxBottomBlurRadius = with(density) { 25.dp.toPx() }

    val backdrop = rememberBlurBackdrop(containerColor)

    Scaffold(
        modifier = modifier,
        topBar = @Composable {
            topBar?.let {
                if (blurTopBar && backdrop != null) {
                    DynamicBlurBox(
                        backdrop = backdrop,
                        containerColor = containerColor,
                        blurRadius = maxTopBlurRadius,
                        blurTintAlpha = blurTintAlpha,
                        fractionProvider = topBarBlurFractionProvider
                    ) {
                        it(layoutPadding)
                    }
                } else {
                    it(layoutPadding)
                }
            }
        },
        bottomBar = @Composable {
            bottomBar?.let {
                if (blurBottomBar && backdrop != null) {
                    DynamicBlurBox(
                        backdrop = backdrop,
                        containerColor = containerColor,
                        blurRadius = maxBottomBlurRadius,
                        blurTintAlpha = blurTintAlpha,
                        fractionProvider = bottomBarBlurFractionProvider
                    ) {
                        it(layoutPadding)
                    }
                } else {
                    it(layoutPadding)
                }
            }
        },
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        snackbarHost = snackbarHost,
        containerColor = containerColor,
        contentWindowInsets = contentWindowInsets,
    ) { contentPadding ->
        Box(
            if ((blurTopBar || blurBottomBar) && backdrop != null) {
                Modifier.layerBackdrop(backdrop)
            } else {
                Modifier
            }
        ) {
            content(
                PaddingValues(
                    start = contentPadding.calculateStartPadding(layoutDirection) +
                            layoutPadding.calculateStartPadding(layoutDirection),
                    top = contentPadding.calculateTopPadding() +
                            layoutPadding.calculateTopPadding(),
                    end = contentPadding.calculateEndPadding(layoutDirection) +
                            layoutPadding.calculateEndPadding(layoutDirection),
                    bottom = contentPadding.calculateBottomPadding() +
                            layoutPadding.calculateBottomPadding()
                )
            )
        }
    }
}

@Composable
private fun DynamicBlurBox(
    backdrop: LayerBackdrop?,
    containerColor: Color,
    blurRadius: Float,
    blurTintAlpha: Float,
    fractionProvider: () -> Float,
    content: @Composable () -> Unit
) {
    if (backdrop == null) {
        content()
        return
    }

    val blurColors = remember(containerColor, blurTintAlpha) {
        BlurColors(
            blendColors = listOf(BlendColorEntry(containerColor.copy(alpha = blurTintAlpha)))
        )
    }

    // 柔光玻璃：边缘光晕 + 随倾斜移动的高光
    val uiConfig = LocalHyperXLayoutConfig.current
    val highlight = rememberGlassHighlight(
        enabled = uiConfig.isBlurEnabled && uiConfig.isGlassHighlightEnabled,
    )

    Box(
        modifier = Modifier.progressiveTextureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = blurRadius,
            // 顶部最强、向下线性渐清晰，也就是「从下到上逐渐模糊」
            gradient = ProgressiveBlur.Top,
            colors = blurColors,
            highlight = highlight,
        ).drawBehind {
            // 只补一点底色保证标题对比度，绝不能用纯色把渐进模糊盖住。
            // 展开态 fraction=0 完全透明，折叠后才逐渐加深。
            val fraction = fractionProvider().coerceIn(0f, 1f)
            val tint = fraction * 0.18f
            if (tint > 0f) {
                drawRect(color = containerColor.copy(alpha = tint))
            }
        }
    ) {
        content()
    }
}