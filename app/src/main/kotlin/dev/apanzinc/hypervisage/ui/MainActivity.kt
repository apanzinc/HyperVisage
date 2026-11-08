// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import dev.apanzinc.hypervisage.data.Route
import dev.apanzinc.hypervisage.data.preference.Preferences
import dev.apanzinc.hypervisage.debug.CrashRecorder
import dev.apanzinc.hypervisage.ui.component.HyperVisageBottomBarOverlay
import dev.apanzinc.hypervisage.ui.pages.ExamplePage
import dev.apanzinc.hypervisage.ui.pages.MainTabsScreen
import dev.apanzinc.hypervisage.ui.pages.SearchPage
import dev.apanzinc.hypervisage.ui.pages.TabCount
import dev.apanzinc.hypervisage.ui.preference.AppPreferenceActions
import dev.apanzinc.hypervisage.ui.preference.AppSettings
import dev.lackluster.hyperx.core.HyperXActivity
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.layout.HyperXAppLayout
import dev.lackluster.hyperx.ui.layout.HyperXLayoutConfig
import dev.lackluster.hyperx.ui.preference.core.LocalPreferenceActions
import dev.lackluster.hyperx.ui.theme.HyperXTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.theme.ColorSchemeMode

class MainActivity : HyperXActivity() {

    @Composable
    override fun AppContent() {
        val appSettings: AppSettings = koinInject()
        val themeMode by appSettings.themeMode.collectAsState()
        val colorSchemeMode = when (themeMode) {
            Preferences.App.THEME_LIGHT -> ColorSchemeMode.Light
            Preferences.App.THEME_DARK -> ColorSchemeMode.Dark
            else -> ColorSchemeMode.System
        }
        // 上次崩溃的日志优先展示，方便不接 ADB 时直接复制堆栈
        val crashReport = remember { mutableStateOf(CrashRecorder.read(this)) }
        val report = crashReport.value
        if (report != null) {
            HyperXTheme(colorSchemeMode = colorSchemeMode) {
                CrashReportScreen(
                    report = report,
                    onClose = {
                        CrashRecorder.clear(this)
                        crashReport.value = null
                    },
                )
            }
        } else {
            MainContent(colorSchemeMode = colorSchemeMode)
        }
    }

    @Composable
    private fun MainContent(colorSchemeMode: ColorSchemeMode) {
        val preferenceActions: AppPreferenceActions = koinInject()
        val appSettings: AppSettings = koinInject()
        val predictiveBack by appSettings.predictiveBack.collectAsState()
        val glassBlur by appSettings.glassBlur.collectAsState()
        val standardBottomBar by appSettings.standardBottomBar.collectAsState()
        // 二级页（搜索结果）要能定位回某个顶层标签页，用一个一次性事件流通知 Pager
        val tabRequests = remember { MutableSharedFlow<Int>(extraBufferCapacity = 1) }
        // Pager 状态与底栏拖动状态都提到窗口层：页面内容和窗口级底栏共用同一份
        // 连续进度，底栏才不会在页面转场或切页时跟内容错位。
        val pagerState = rememberPagerState { TabCount }
        val bottomBarDragging = remember { mutableStateOf(false) }

        // 以后可以接上"是否分屏""模糊强度"这类模块自己的外观设置
        val layoutConfig = HyperXLayoutConfig(
            isSplitScreenEnabled = true,
            isBlurEnabled = glassBlur,
            // 顶栏是贴边矩形，BloomStroke 的玻璃描边会显示成一条白色亮线，关掉
            isGlassHighlightEnabled = false,
        )

        CompositionLocalProvider(LocalPreferenceActions provides preferenceActions) {
            HyperXAppLayout(
                config = layoutConfig,
                predictiveBackEnabled = predictiveBack,
                colorSchemeMode = colorSchemeMode,
                primaryContent = {
                    MainTabsScreen(
                        tabRequests = tabRequests,
                        pagerState = pagerState,
                        bottomBarDragging = bottomBarDragging,
                    )
                },
                // 底栏挂在窗口层而不是页面里：模糊是高优先级、常驻效果，
                // 不能跟着 NavDisplay 的场景转场被裁剪、缩放、变暗或降级。
                overlayContent = { backdrop ->
                    HyperVisageBottomBarOverlay(
                        backdrop = backdrop,
                        glassBlur = glassBlur,
                        standardBottomBar = standardBottomBar,
                        pagerState = pagerState,
                        bottomBarDragging = bottomBarDragging,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                },
                customEntryProvider = { key ->
                    NavEntry(key) {
                        when (key) {
                            is Route.Example -> ExamplePage()
                            is Route.Search -> {
                                val navigator = LocalNavigator.current
                                SearchPage(
                                    onOpenTab = { index ->
                                        navigator.pop()
                                        tabRequests.tryEmit(index)
                                    },
                                )
                            }
                            else -> {}
                        }
                    }
                },
            )
        }
    }
}
