package dev.lackluster.hyperx.ui.layout

import androidx.compose.foundation.background
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
    // 这里的条件只依赖静态配置，转场期间不拆装 layerBackdrop 节点。
    // 拆装会重建整条绘制链，动画开始/结束各抖一下，反而更卡。
    val hasBlurBars = (blurTopBar || blurBottomBar) && backdrop != null

    Scaffold(
        modifier = modifier,
        topBar = @Composable {
            topBar?.let {
                if (blurTopBar) {
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
                if (blurBottomBar) {
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
            if (hasBlurBars) {
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
        // 模糊关闭 / 设备不支持 RuntimeShader 时顶栏是透明的，必须补底色。
        Box(modifier = Modifier.background(containerColor)) {
            content()
        }
        return
    }

    // 只订阅这一小块组合；转场状态变化不会波及 HyperXScaffold 和导航层。
    val transitionState = LocalPageTransitionRunning.current
    val blurEnabled = !transitionState.value

    val blurColors = remember(containerColor, blurTintAlpha) {
        BlurColors(
            blendColors = listOf(BlendColorEntry(containerColor.copy(alpha = blurTintAlpha)))
        )
    }

    // 柔光玻璃描边是可选的：顶栏/底栏用 RectangleShape，描边会变成一条白色亮线，
    // 因此默认不启用；只有卡片类的圆角玻璃面才需要它。
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
            // 0.9.4 的渐进模糊默认就是 0，显式写出防止以后默认值变动
            // 又多出一趟逐帧噪声 pass
            noiseCoefficient = 0f,
            colors = blurColors,
            highlight = highlight,
            // 官方开关：false 时 draw() 直接 drawContent() 返回，
            // measure 也不 placeWithLayer，完全跳过 shader 且不重建节点
            enabled = blurEnabled,
        ).drawBehind {
            // draw 阶段读 state，底色切换只触发重绘不触发重组。
            // 顶栏在 HyperXPage 里是透明的，降级期间必须给不透明底色。
            if (transitionState.value) {
                drawRect(color = containerColor)
            }
            // 只补一点底色保证标题对比度，绝不能用纯色把渐进模糊盖住。
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
