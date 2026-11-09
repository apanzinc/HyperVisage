// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.apanzinc.hypervisage.R
import dev.apanzinc.hypervisage.data.Route
import dev.apanzinc.hypervisage.ui.pages.TabCount
import dev.apanzinc.hypervisage.ui.pages.TabSpring
import dev.lackluster.hyperx.navigation.LocalNavigator
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 材质层比按钮本体高，翻转渐进模糊才有足够距离从「下面最糊」过渡到「上面清晰」。 */
private val BarMaterialHeight = 148.dp
/** 底栏底层背景模糊：比顶栏略强，浅色深色都生效。 */
private val BackgroundBlurRadius = 22.dp
/** 按钮自身的背景模糊，明显强于底层，深色下尤其突出。 */
private val ButtonBlurRadius = 32.dp

/**
 * 窗口级底栏覆盖层。
 *
 * 它挂在 HyperXAppLayout 的 overlayContent 上，在整棵内容树之上绘制：
 * 不属于任何一个页面，不参与 NavDisplay 的场景转场，不会被裁剪、缩放、变暗，
 * 也不会被页面转场当成可降级效果关掉。背景纹理同样是窗口级的
 * `LayerBackdrop`，录的是全部页面内容而不是某一个 Pager。
 *
 * 模糊是高优先级：只要「界面模糊」开着且设备支持 RuntimeShader，浅色和深色
 * 都完整渲染；只有关掉开关才整层隐藏（不补底色）。
 */
@Composable
fun HyperVisageBottomBarOverlay(
    backdrop: LayerBackdrop?,
    glassBlur: Boolean,
    standardBottomBar: Boolean,
    pagerState: PagerState,
    bottomBarDragging: MutableState<Boolean>,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    // 用背景与前景的相对亮度判断深浅，不单卡 0.5 阈值：
    // 深色主题下 background 万一偏亮，误走浅色分支就会叠出一整块白底
    val isLight = colors.background.luminance() > colors.onBackground.luminance()
    // 只受开关与设备能力控制，不读 LocalPageTransitionRunning —— 转场时不降级。
    // null 表示关了模糊或设备不支持 RuntimeShader，此时整层不画底色、按钮退回实底。
    val activeBackdrop = if (glassBlur) backdrop else null

    // miuix-blur 的 blurRadius 单位是 dp：effect 内部会自己乘 density，
    // 这里再 toPx() 会把半径放大 density 倍，浅色下就成了洗白一片。
    val backgroundBlurRadius = BackgroundBlurRadius.value
    val buttonBlurRadius = ButtonBlurRadius.value

    // 浅色页面背景接近纯白，模糊白底几乎看不出来，所以叠一层很淡的冷灰玻璃色
    // 并抬高对比度，让「被糊开的内容」和纯背景分开，而不是靠一条灰/白实底当边界。
    val backgroundBlurColors = remember(isLight, colors.background) {
        if (isLight) {
            BlurColors(
                blendColors = listOf(
                    BlendColorEntry(Color(0xFFC6CEDA).copy(alpha = 0.20f)),
                ),
                brightness = -0.012f,
                contrast = 1.09f,
                saturation = 1.02f,
            )
        } else {
            // 深色只压暗、不提亮：不叠任何白色，也不加 brightness / contrast，
            // 模糊结果保持黑色调。白雾面和提亮会把整块刷成灰白，看着像一层白底。
            BlurColors(
                blendColors = listOf(
                    BlendColorEntry(colors.background.copy(alpha = 0.46f)),
                ),
                brightness = 0f,
                contrast = 1f,
                saturation = 1f,
            )
        }
    }

    // 按钮模糊比底层更强靠的是 32dp 半径和更浅的压暗，不是靠白色雾面提亮。
    // 深色下按钮比底层少压一层，自然浮出来，整块仍是黑色调的纯模糊。
    val buttonBlurColors = remember(isLight, colors.background) {
        if (isLight) {
            BlurColors(
                blendColors = listOf(
                    BlendColorEntry(Color.White.copy(alpha = 0.52f)),
                ),
                contrast = 1.07f,
            )
        } else {
            BlurColors(
                blendColors = listOf(
                    BlendColorEntry(colors.background.copy(alpha = 0.22f)),
                ),
                brightness = 0f,
                contrast = 1f,
                saturation = 1f,
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(BarMaterialHeight)
            .then(
                activeBackdrop?.let {
                    Modifier.progressiveTextureBlur(
                        backdrop = it,
                        shape = RectangleShape,
                        blurRadius = backgroundBlurRadius,
                        // 翻转 180°：底部最模糊，越往上越清晰
                        gradient = ProgressiveBlur.Bottom,
                        noiseCoefficient = 0f,
                        colors = backgroundBlurColors,
                        enabled = true,
                    )
                } ?: Modifier
            )
            .drawBehind {
                if (activeBackdrop == null) return@drawBehind
                // 只补一点过渡，上端完全透明，不画灰/白实底当边界
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = if (isLight) {
                            listOf(Color.Transparent, Color(0xFFE4E9F0).copy(alpha = 0.26f))
                        } else {
                            listOf(Color.Transparent, colors.background.copy(alpha = 0.28f))
                        }
                    )
                )
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        ) {
            BottomBarHost(
                backdrop = activeBackdrop,
                buttonBlurRadius = buttonBlurRadius,
                buttonBlurColors = buttonBlurColors,
                pagerState = pagerState,
                standardBottomBar = standardBottomBar,
                onDragActiveChange = { bottomBarDragging.value = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 把 Pager 的离散选中页和连续进度都收在这一小块组合里，重组不会波及页面内容。 */
@Composable
private fun BottomBarHost(
    backdrop: LayerBackdrop?,
    buttonBlurRadius: Float,
    buttonBlurColors: BlurColors,
    pagerState: PagerState,
    standardBottomBar: Boolean,
    onDragActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val selectedIndex by remember(pagerState) { derivedStateOf { pagerState.currentPage } }
    val selectedProgress: State<Float> = remember(pagerState) {
        derivedStateOf { pagerState.currentPage + pagerState.currentPageOffsetFraction }
    }
    val navHome = stringResource(R.string.home_nav_home)
    val navSettings = stringResource(R.string.home_nav_settings)
    val navAbout = stringResource(R.string.home_nav_about)
    val navSearch = stringResource(R.string.home_nav_search)

    // 图标固定一套面性，选中与未选中不换图标
    val items = listOf(
        HyperVisageBottomBarItem(
            icon = ImageVector.vectorResource(NavIcons.HomeFace),
            label = navHome,
        ),
        HyperVisageBottomBarItem(
            icon = ImageVector.vectorResource(NavIcons.SettingsFace),
            label = navSettings,
        ),
        HyperVisageBottomBarItem(
            icon = ImageVector.vectorResource(NavIcons.AboutFace),
            label = navAbout,
        ),
    )
    val searchIcon = ImageVector.vectorResource(NavIcons.SearchFace)

    // 底栏现在常驻在二级页之上，切标签时先把二级页退掉，否则点了看不到切换
    val onSelect: (Int) -> Unit = { index ->
        while (navigator.backStackSize() > 1) navigator.pop()
        scope.launch { pagerState.animateScrollToPage(index, animationSpec = TabSpring) }
    }

    if (standardBottomBar) {
        // 组件库固定贴底的标准 NavigationBar，不是浮起来的 FloatingNavigationBar
        NavigationBar(modifier = modifier) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = item.icon,
                    label = item.label,
                )
            }
            NavigationBarItem(
                selected = false,
                onClick = { navigator.push(Route.Search) },
                icon = searchIcon,
                label = navSearch,
            )
        }
    } else {
        HyperVisageBottomBar(
            items = items,
            selectedIndex = selectedIndex,
            selectedProgress = selectedProgress,
            onSelect = onSelect,
            onDragPageDelta = { pageDelta ->
                val pageSize = pagerState.layoutInfo.pageSize
                if (pageSize > 0) {
                    pagerState.dispatchRawDelta(pageDelta * pageSize)
                }
            },
            onDragEnd = {
                val target = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    .roundToInt().coerceIn(0, TabCount - 1)
                scope.launch { pagerState.animateScrollToPage(target, animationSpec = TabSpring) }
            },
            onDragActiveChange = onDragActiveChange,
            actionIcon = searchIcon,
            actionContentDescription = navSearch,
            onActionClick = { navigator.push(Route.Search) },
            backdrop = backdrop,
            buttonBlurRadius = buttonBlurRadius,
            buttonBlurColors = buttonBlurColors,
            modifier = modifier,
        )
    }
}
