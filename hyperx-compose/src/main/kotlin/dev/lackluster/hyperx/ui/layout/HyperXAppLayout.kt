package dev.lackluster.hyperx.ui.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import dev.lackluster.hyperx.R
import dev.lackluster.hyperx.navigation.HyperXRoute
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.navigation.Navigator
import dev.lackluster.hyperx.ui.animation.HyperXNavTransitions
import dev.lackluster.hyperx.ui.component.AdaptiveIcon
import dev.lackluster.hyperx.ui.component.IconSize
import dev.lackluster.hyperx.ui.component.ImageIcon
import dev.lackluster.hyperx.ui.component.ImageSource
import dev.lackluster.hyperx.ui.theme.HyperXTheme
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils

@Composable
fun HyperXAppLayout(
    config: HyperXLayoutConfig = HyperXLayoutConfig(),
    predictiveBackEnabled: Boolean = false,
    /** 由 App 侧的界面配色设置传入，默认仍跟随系统 */
    colorSchemeMode: ColorSchemeMode = ColorSchemeMode.System,
    customEntryProvider: ((key: NavKey) -> NavEntry<NavKey>)? = null,
    emptyContent: @Composable () -> Unit = { DefaultEmptyPage() },
    /**
     * 窗口级覆盖层：在所有页面与布局之上绘制，不属于任何页面，也不参与
     * NavDisplay 的场景转场（不会被裁剪、缩放、变暗或跟着页面滑动）。
     * 参数是挂在整个内容层上的 [LayerBackdrop]，覆盖层可以用它做背景模糊；
     * 关闭模糊或设备不支持 RuntimeShader 时为 null。
     */
    overlayContent: @Composable BoxScope.(LayerBackdrop?) -> Unit = {},
    primaryContent: @Composable () -> Unit
) {
    HyperXTheme(colorSchemeMode = colorSchemeMode) {
        val density = LocalDensity.current
        val containerSize = LocalWindowInfo.current.containerSize
        val windowWidth = with(density) { containerSize.width.toDp() }
        val windowHeight = with(density) { containerSize.height.toDp() }

        val isLandscape = windowWidth > windowHeight
        val largeScreen = windowHeight >= 480.dp && windowWidth >= 840.dp

        val backStack = rememberNavBackStack(HyperXRoute.Main)
        val navigator = remember(backStack) { Navigator(backStack) }

        val appRootLayout = when {
            config.isSplitScreenEnabled && largeScreen && isLandscape -> AppRootLayout.Split12
            config.isSplitScreenEnabled && (largeScreen || isLandscape) -> AppRootLayout.Split11
            largeScreen -> AppRootLayout.LargeScreen
            else -> AppRootLayout.Normal
        }

        CompositionLocalProvider(
            LocalNavigator provides navigator,
            LocalHyperXLayoutConfig provides config
        ) {
            // 窗口级背景纹理：录制整棵内容树（包含所有页面、以及页面转场时同时存在的两个场景），
            // 供顶层覆盖层采样。这里不读 LocalPageTransitionRunning —— 覆盖层的模糊是高优先级
            // 效果，不能因为页面转场就被降级成纯色。
            val overlayContainerColor = MiuixTheme.colorScheme.background
            val overlayBackdrop = if (config.isBlurEnabled && isRuntimeShaderSupported()) {
                rememberLayerBackdrop {
                    drawRect(overlayContainerColor)
                    drawContent()
                }
            } else {
                null
            }

            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (overlayBackdrop != null) {
                                Modifier.layerBackdrop(overlayBackdrop)
                            } else {
                                Modifier
                            }
                        )
                ) {
                    AnimatedContent(
                        targetState = appRootLayout,
                        label = "HyperXLayoutSwitch",
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                        }
                    ) { targetLayout ->
                        when (targetLayout) {
                            AppRootLayout.Split11, AppRootLayout.Split12 -> {
                                val rightWeight = if (targetLayout == AppRootLayout.Split12) 2.0f else 1.0f
                                UnifiedSplitLayout(
                                    backStack = backStack,
                                    rightWeight = rightWeight,
                                    predictiveBackEnabled = predictiveBackEnabled,
                                    primaryContent = primaryContent,
                                    emptyContent = emptyContent,
                                    customEntryProvider = customEntryProvider
                                )
                            }
                            else -> {
                                val extraPadding = if (targetLayout == AppRootLayout.LargeScreen) {
                                    PaddingValues(horizontal = windowWidth * 0.1f)
                                } else {
                                    PaddingValues(0.dp)
                                }
                                UnifiedNormalLayout(
                                    backStack = backStack,
                                    extraPadding = extraPadding,
                                    predictiveBackEnabled = predictiveBackEnabled,
                                    primaryContent = primaryContent,
                                    customEntryProvider = customEntryProvider
                                )
                            }
                        }
                    }
                }
                // 窗口级覆盖层：绘制在所有页面之上，不属于任何页面，也不参与场景转场。
                overlayContent(overlayBackdrop)
            }
        }
        MiuixPopupUtils.MiuixPopupHost()
    }
}

@Composable
private fun UnifiedNormalLayout(
    backStack: MutableList<NavKey>,
    extraPadding: PaddingValues,
    predictiveBackEnabled: Boolean,
    primaryContent: @Composable () -> Unit,
    customEntryProvider: ((key: NavKey) -> NavEntry<NavKey>)?
) {
    val layoutDirection = LocalLayoutDirection.current
    val systemBarInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal).asPaddingValues()

    val contentPadding = PaddingValues(
        start = systemBarInsets.calculateStartPadding(layoutDirection) + extraPadding.calculateStartPadding(layoutDirection),
        top = extraPadding.calculateTopPadding(),
        end = systemBarInsets.calculateEndPadding(layoutDirection) + extraPadding.calculateEndPadding(layoutDirection),
        bottom = extraPadding.calculateBottomPadding()
    )

    // 局部下发：全屏模式和当前页面的 Padding
    CompositionLocalProvider(
        LocalPageMode provides PageLayoutMode.FULL_SCREEN,
        LocalLayoutPadding provides contentPadding
    ) {
        HyperXNavDisplay(
            backStack = backStack,
            onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
            predictiveBackEnabled = predictiveBackEnabled,
            transitionSpec = HyperXNavTransitions.normalTransitionSpec(layoutDirection),
            popTransitionSpec = HyperXNavTransitions.normalPopTransitionSpec(layoutDirection),
            predictivePopTransitionSpec = HyperXNavTransitions.normalPredictivePopTransitionSpec(layoutDirection),
            transitionEffects = HyperXNavTransitions.NormalTransitionEffects,
            entryProvider = { key ->
                when (key) {
                    // 全屏下，遇到 Main 路由直接渲染主页
                    is HyperXRoute.Main -> NavEntry(key) { primaryContent() }
                    else -> customEntryProvider?.invoke(key) ?: NavEntry(key) {}
                }
            }
        )
    }
}

@Composable
private fun UnifiedSplitLayout(
    backStack: MutableList<NavKey>,
    rightWeight: Float,
    predictiveBackEnabled: Boolean,
    primaryContent: @Composable () -> Unit,
    emptyContent: @Composable () -> Unit,
    customEntryProvider: ((key: NavKey) -> NavEntry<NavKey>)?
) {
    val layoutDirection = LocalLayoutDirection.current
    val systemBarInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Horizontal).asPaddingValues()

    val primaryPadding = PaddingValues(
        start = systemBarInsets.calculateStartPadding(layoutDirection) + 12.dp,
        top = systemBarInsets.calculateTopPadding(),
        end = 12.dp,
        bottom = systemBarInsets.calculateBottomPadding()
    )

    val secondaryPadding = PaddingValues(
        start = 12.dp,
        top = systemBarInsets.calculateTopPadding(),
        end = systemBarInsets.calculateEndPadding(layoutDirection) + 12.dp,
        bottom = systemBarInsets.calculateBottomPadding()
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(
                LocalPageMode provides PageLayoutMode.SPLIT_PRIMARY,
                LocalLayoutPadding provides primaryPadding
            ) {
                primaryContent()
            }
        }

        VerticalDivider(thickness = 0.75.dp, color = MiuixTheme.colorScheme.dividerLine)

        // 右半边区域：根据路由栈渲染详情页
        CompositionLocalProvider(
            LocalPageMode provides PageLayoutMode.SPLIT_SECONDARY,
            LocalLayoutPadding provides secondaryPadding
        ) {
            HyperXNavDisplay(
                backStack = backStack,
                modifier = Modifier.weight(rightWeight),
                onBack = { if (backStack.size > 1) backStack.removeLastOrNull() },
                predictiveBackEnabled = predictiveBackEnabled,
                transitionSpec = HyperXNavTransitions.splitTransitionSpec(layoutDirection),
                popTransitionSpec = HyperXNavTransitions.splitPopTransitionSpec(layoutDirection),
                predictivePopTransitionSpec = HyperXNavTransitions.splitPredictivePopTransitionSpec(layoutDirection),
                transitionEffects = HyperXNavTransitions.SplitTransitionEffects,
                entryProvider = { key ->
                    when (key) {
                        is HyperXRoute.Main -> NavEntry(key) { emptyContent() }
                        else -> customEntryProvider?.invoke(key) ?: NavEntry(key) {}
                    }
                }
            )
        }
    }
}

@Composable
fun DefaultEmptyPage(
    imageIcon: ImageIcon = ImageIcon(
        source = ImageSource.Res(R.drawable.ic_miuix),
        size = IconSize.Unspecified,
        customSizeDp = 255.dp
    )
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        AdaptiveIcon(imageIcon)
    }
}

enum class AppRootLayout {
    Normal,
    LargeScreen,
    Split11,
    Split12
}

@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp,
    color: Color,
) =
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(thickness)
    ) {
        drawLine(
            color = color,
            strokeWidth = thickness.toPx(),
            start = Offset(thickness.toPx() / 2, 0f),
            end = Offset(thickness.toPx() / 2, size.height),
        )
    }