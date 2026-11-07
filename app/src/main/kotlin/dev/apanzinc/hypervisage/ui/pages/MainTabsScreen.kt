// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.apanzinc.hypervisage.R
import dev.apanzinc.hypervisage.data.Route
import dev.apanzinc.hypervisage.ui.component.HyperVisageBottomBar
import dev.apanzinc.hypervisage.ui.component.HyperVisageBottomBarItem
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.layout.LocalPageTransitionRunning
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Settings

const val TabHome = 0
const val TabSettings = 1
const val TabAbout = 2

private const val TabCount = 3

/** 点击底栏时的横向滑动，稍软一点，和页面转场的弹簧节奏接近。 */
private val TabSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 320f)

/**
 * 顶层三个页面完全并行：主页 / 设置 / 关于都是同一个 HorizontalPager 的页，
 * 没有 push / pop，点底栏是 animateScrollToPage，手指横滑也能直接切页，
 * 各自的滚动位置与输入框状态由 Pager 保留。
 *
 * 底栏在 Pager 之外，切页时它不动，只有选中椭圆跟着滑。
 */
@Composable
fun MainTabsScreen(
    tabRequests: MutableSharedFlow<Int>,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { TabCount }
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val navTransition = LocalPageTransitionRunning.current

    // 横滑期间两个页面会同时存在，各自跑一遍渐进模糊就会掉帧。
    // 复用导航层那套降级开关：合并「导航栈转场」和「Pager 滑动」两个来源，
    // 期间只画静态底色，停下后恢复 shader。
    val tabTransition: State<Boolean> = remember(navTransition, pagerState) {
        derivedStateOf { navTransition.value || pagerState.isScrollInProgress }
    }

    LaunchedEffect(tabRequests, pagerState) {
        tabRequests.collect { page ->
            if (page in 0 until TabCount && pagerState.currentPage != page) {
                pagerState.animateScrollToPage(page, animationSpec = TabSpring)
            }
        }
    }

    TabBackHandler(pagerState)

    Box(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalPageTransitionRunning provides tabTransition) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    TabHome -> HomePage(onOpenTab = { tabRequests.tryEmit(it) })
                    TabSettings -> ModuleSettingsPage()
                    TabAbout -> AboutPage()
                }
            }
        }

        MainBottomBar(
            pagerState = pagerState,
            onSearch = { navigator.push(Route.Search) },
            modifier = Modifier.align(Alignment.BottomCenter),
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

@Composable
private fun MainBottomBar(
    pagerState: PagerState,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val selectedIndex by remember(pagerState) { derivedStateOf { pagerState.currentPage } }
    val navHome = stringResource(R.string.home_nav_home)
    val navSettings = stringResource(R.string.home_nav_settings)
    val navAbout = stringResource(R.string.home_nav_about)
    val navSearch = stringResource(R.string.home_nav_search)

    HyperVisageBottomBar(
        items = listOf(
            HyperVisageBottomBarItem(MiuixIcons.Home, navHome),
            HyperVisageBottomBarItem(MiuixIcons.Settings, navSettings),
            HyperVisageBottomBarItem(MiuixIcons.Info, navAbout),
        ),
        selectedIndex = selectedIndex,
        onSelect = { index ->
            scope.launch { pagerState.animateScrollToPage(index, animationSpec = TabSpring) }
        },
        actionIcon = MiuixIcons.Search,
        actionContentDescription = navSearch,
        onActionClick = onSearch,
        modifier = modifier,
    )
}
