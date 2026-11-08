// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import dev.apanzinc.hypervisage.R
import dev.apanzinc.hypervisage.data.Route
import dev.apanzinc.hypervisage.ui.component.HyperVisageBottomBar
import dev.apanzinc.hypervisage.ui.component.HyperVisageBottomBarItem
import dev.apanzinc.hypervisage.ui.component.NavIcons
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.layout.LocalPageTransitionRunning
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem

const val TabHome = 0
const val TabSettings = 1
const val TabAbout = 2

private const val TabCount = 3

/** 点击底栏或拖动松手时的横向滑动，稍软一点，和页面转场的弹簧节奏接近。 */
private val TabSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 320f)

/**
 * 顶层三个页面完全并行：主页 / 设置 / 关于都是同一个 HorizontalPager 的页，
 * 没有 push / pop，点底栏是 animateScrollToPage，手指横滑也能直接切页，
 * 各自的滚动位置与输入框状态由 Pager 保留。
 *
 * 底栏是固定在屏幕底部的一块，不浮在内容上：用 Column 把 Pager 压到
 * weight(1f)，底栏占下面独立一行，页面内容不会滚到底栏背后。
 */
@Composable
fun MainTabsScreen(
    tabRequests: MutableSharedFlow<Int>,
    standardBottomBar: Boolean,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { TabCount }
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val navTransition = LocalPageTransitionRunning.current
    // 底栏拖动不走 Pager 自己的 scrollable，得单独告诉模糊层「正在转场」
    val bottomBarDragging = remember { mutableStateOf(false) }

    // 横滑期间两个页面会同时存在，各自跑一遍渐进模糊就会掉帧。
    // 复用导航层那套降级开关：合并「导航栈转场」「Pager 滑动」「底栏拖动」三个来源。
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

    Column(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalPageTransitionRunning provides tabTransition) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) { page ->
                when (page) {
                    TabHome -> HomePage(onOpenTab = { tabRequests.tryEmit(it) })
                    TabSettings -> ModuleSettingsPage()
                    TabAbout -> AboutPage()
                }
            }
        }

        BottomBarHost(
            pagerState = pagerState,
            standardBottomBar = standardBottomBar,
            onDragActiveChange = { bottomBarDragging.value = it },
            onSelect = { index ->
                scope.launch { pagerState.animateScrollToPage(index, animationSpec = TabSpring) }
            },
            onSearch = { navigator.push(Route.Search) },
            modifier = Modifier.fillMaxWidth(),
        )
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
