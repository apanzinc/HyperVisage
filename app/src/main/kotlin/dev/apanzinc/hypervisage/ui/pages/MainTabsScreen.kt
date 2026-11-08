// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import dev.apanzinc.hypervisage.R
import dev.apanzinc.hypervisage.data.Route
import dev.apanzinc.hypervisage.ui.component.HyperVisageBottomBar
import dev.apanzinc.hypervisage.ui.component.HyperVisageBottomBarItem
import dev.apanzinc.hypervisage.ui.component.NavIcons
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.effect.rememberBlurBackdrop
import dev.lackluster.hyperx.ui.layout.LocalPageTransitionRunning
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

const val TabHome = 0
const val TabSettings = 1
const val TabAbout = 2

private const val TabCount = 3

/** 点击底栏或拖动松手时的横向滑动，稍软一点，和页面转场的弹簧节奏接近。 */
private val TabSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 320f)

/** 底栏底层模糊半径，比顶栏小一些，少一层逐帧开销。 */
private val BottomBarBlurRadius = 18.dp

/**
 * 顶层三个页面完全并行：主页 / 设置 / 关于都是同一个 HorizontalPager 的页，
 * 没有 push / pop，点底栏是 animateScrollToPage，手指横滑也能直接切页，
 * 各自的滚动位置与输入框状态由 Pager 保留。
 *
 * 底栏固定贴在屏幕底部，但页面内容会延伸到它后面：底层模糊要采到内容，
 * 就必须让内容铺满整个屏幕，底栏作为覆盖层压在底部。三个标签页列表末尾
 * 留 112dp，最后一项仍能滚出遮挡。
 */
@Composable
fun MainTabsScreen(
    tabRequests: MutableSharedFlow<Int>,
    standardBottomBar: Boolean,
    glassBlur: Boolean,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { TabCount }
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val navTransition = LocalPageTransitionRunning.current
    // 底栏拖动不走 Pager 自己的 scrollable，得单独告诉模糊层「正在转场」
    val bottomBarDragging = remember { mutableStateOf(false) }

    // 横滑期间两个页面会同时存在，各自跑一遍渐进模糊就会掉帧。
    // 合并「导航栈转场」「Pager 滑动」「底栏拖动」三个来源，一起降级。
    val tabTransition: State<Boolean> = remember(navTransition, pagerState) {
        derivedStateOf {
            navTransition.value || pagerState.isScrollInProgress || bottomBarDragging.value
        }
    }

    LaunchedEffect(tabRequests, pagerState) {
        tabRequests.collect { page ->
            if (page in 0 until TabCount && pagerState.currentPage != page) {
                pagerState.animateScrollToPage(page, animationSpec = TabSpring)
            }
        }
    }

    TabBackHandler(pagerState)

    // 让 backdrop 也读到合并后的转场状态：横滑时同样跳过离屏录制
    CompositionLocalProvider(LocalPageTransitionRunning provides tabTransition) {
        val colors = MiuixTheme.colorScheme
        val backdrop = if (glassBlur) {
            rememberBlurBackdrop(containerColor = colors.background)
        } else {
            null
        }

        Box(modifier = modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (backdrop != null) {
                            Modifier.layerBackdrop(backdrop)
                        } else {
                            Modifier
                        }
                    ),
            ) { page ->
                when (page) {
                    TabHome -> HomePage(onOpenTab = { tabRequests.tryEmit(it) })
                    TabSettings -> ModuleSettingsPage()
                    TabAbout -> AboutPage()
                }
            }

            BottomBarBlurLayer(
                backdrop = backdrop,
                glassBlur = glassBlur,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
            ) {
                BottomBarHost(
                    pagerState = pagerState,
                    standardBottomBar = standardBottomBar,
                    onDragActiveChange = { bottomBarDragging.value = it },
                    onSelect = { index ->
                        scope.launch {
                            pagerState.animateScrollToPage(index, animationSpec = TabSpring)
                        }
                    },
                    onSearch = { navigator.push(Route.Search) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * 底栏的底层材质：和顶栏同一套渐进模糊，但翻转 180° 用 `ProgressiveBlur.Bottom`，
 * 也就是底部最模糊、越往上越清晰。材质本身不加阴影，阴影只落在按钮上。
 *
 * 模糊关闭、设备不支持或转场期间，直接在 draw 阶段补一层不透明背景色，
 * 不拆装节点、不跑 shader。
 */
@Composable
private fun BottomBarBlurLayer(
    backdrop: LayerBackdrop?,
    glassBlur: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    val transition = LocalPageTransitionRunning.current
    val blurEnabled = glassBlur && backdrop != null && !transition.value
    val blurRadius = with(LocalDensity.current) { BottomBarBlurRadius.toPx() }
    val blurColors = remember(colors.background) {
        BlurColors(
            blendColors = listOf(
                BlendColorEntry(colors.background.copy(alpha = if (isDark) 0.52f else 0.58f))
            )
        )
    }

    Box(
        modifier = modifier
            .then(
                if (backdrop != null) {
                    Modifier.progressiveTextureBlur(
                        backdrop = backdrop,
                        shape = RectangleShape,
                        blurRadius = blurRadius,
                        gradient = ProgressiveBlur.Bottom,
                        noiseCoefficient = 0f,
                        colors = blurColors,
                        enabled = blurEnabled,
                    )
                } else {
                    Modifier
                }
            )
            .drawBehind {
                if (!blurEnabled) {
                    drawRect(color = colors.background)
                } else {
                    // 底部压一层背景色，向上渐透明，让「越往上越清晰」有实体过渡
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                colors.background.copy(alpha = 0.45f),
                                colors.background,
                            ),
                        )
                    )
                }
            },
    ) {
        content()
    }
}

/** 只在没有二级页面压着的时候，返回键先回主页标签。 */
@Composable
private fun TabBackHandler(pagerState: PagerState) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val notHome by remember(pagerState) { derivedStateOf { pagerState.currentPage != TabHome } }
    // backStack 是 SnapshotStateList，在 derivedStateOf 里读 size 才能订阅到二级页的进出
    val hasSecondary by remember(navigator) { derivedStateOf { navigator.backStackSize() > 1 } }
    BackHandler(enabled = notHome && !hasSecondary) {
        scope.launch { pagerState.animateScrollToPage(TabHome, animationSpec = TabSpring) }
    }
}

/** 把 Pager 的离散选中页和连续进度都收在这一小块组合里，重组不会波及页面内容。 */
@Composable
private fun BottomBarHost(
    pagerState: PagerState,
    standardBottomBar: Boolean,
    onDragActiveChange: (Boolean) -> Unit,
    onSelect: (Int) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
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
                onClick = onSearch,
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
            onActionClick = onSearch,
            modifier = modifier,
        )
    }
}
