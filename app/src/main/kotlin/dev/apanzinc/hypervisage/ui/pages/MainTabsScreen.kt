// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.layout.LocalPageTransitionRunning
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

const val TabHome = 0
const val TabSettings = 1
const val TabAbout = 2

const val TabCount = 3

/** 点击底栏或拖动松手时的横向滑动，稍软一点，和页面转场的弹簧节奏接近。 */
val TabSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 320f)

/**
 * 顶层三个并行标签页的内容：主页 / 设置 / 关于都只是同一个 HorizontalPager 的页，
 * 没有 push / pop，各自的滚动位置与输入框状态由 Pager 保留。
 *
 * 底栏不在这里 —— 它和它的模糊是窗口级覆盖层（[HyperVisageBottomBarOverlay]），
 * 由 MainActivity 通过 HyperXAppLayout 的 overlayContent 挂在所有页面之上，
 * 因此不会被页面转场缩放、裁剪、变暗，也不会跟着页面一起被降级。
 *
 * 三个标签页列表末尾留 112dp，让内容能滚到底栏后面（模糊才有东西可采），
 * 同时最后一项也能滚出底栏遮挡。
 */
@Composable
fun MainTabsScreen(
    tabRequests: MutableSharedFlow<Int>,
    pagerState: PagerState,
    bottomBarDragging: State<Boolean>,
    modifier: Modifier = Modifier,
) {
    val navTransition = LocalPageTransitionRunning.current

    // 页面内顶栏的模糊仍按「导航栈转场 / Pager 滑动 / 底栏拖动」降级；
    // 窗口级底栏模糊不读这个状态，它是高优先级、常驻的。
    val tabTransition: State<Boolean> = remember(navTransition, pagerState, bottomBarDragging) {
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

    CompositionLocalProvider(LocalPageTransitionRunning provides tabTransition) {
        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
        ) { page ->
            when (page) {
                TabHome -> HomePage(onOpenTab = { tabRequests.tryEmit(it) })
                TabSettings -> ModuleSettingsPage()
                TabAbout -> AboutPage()
            }
        }
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
