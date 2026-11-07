// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.apanzinc.hypervisage.R
import dev.apanzinc.hypervisage.data.Route
import dev.apanzinc.hypervisage.ui.component.HyperVisageBottomBar
import dev.apanzinc.hypervisage.ui.component.HyperVisageBottomBarItem
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.TextPreference
import dev.lackluster.hyperx.ui.preference.itemPreferenceGroup
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Settings

/**
 * 主界面。作为主入口页，把返回按钮去掉，底部叠一个悬浮底栏。
 */
@Composable
fun HomePage() {
    val navigator = LocalNavigator.current
    var selectedTab by remember { mutableIntStateOf(0) }
    // 二级页面是覆盖在 HomePage 之上的，返回后底栏要回到「主页」这一项
    val stackSize = navigator.backStack.size
    LaunchedEffect(stackSize) {
        if (stackSize <= 1) selectedTab = 0
    }

    // HyperXPage 的 content 是 LazyListScope，不是 @Composable 上下文，
    // 所以字符串要在这里先取好。
    val title = stringResource(R.string.page_home)
    val scopesTitle = stringResource(R.string.home_group_scopes)
    val othersTitle = stringResource(R.string.home_group_other)
    val exampleTitle = stringResource(R.string.page_example)
    val moduleTitle = stringResource(R.string.page_module_settings)
    val aboutTitle = stringResource(R.string.page_about)
    val navHome = stringResource(R.string.home_nav_home)
    val navSettings = stringResource(R.string.home_nav_settings)
    val navAbout = stringResource(R.string.home_nav_about)

    Box(modifier = Modifier.fillMaxSize()) {
        HyperXPage(
            title = title,
            navigationIcon = {},
        ) {
            itemPreferenceGroup(
                key = "scopes",
                title = scopesTitle,
                position = ItemPosition.Middle,
            ) {
                TextPreference(
                    title = "SystemUI",
                    summary = "状态栏、通知、控制中心相关的美化",
                    onClick = { navigator.push(Route.Example) },
                )
            }

            itemPreferenceGroup(
                key = "others",
                title = othersTitle,
                position = ItemPosition.Middle,
            ) {
                TextPreference(
                    title = exampleTitle,
                    summary = "演示设置项如何驱动 Hook",
                    onClick = { navigator.push(Route.Example) },
                )
            }

            itemPreferenceGroup(
                key = "module",
                position = ItemPosition.Last,
            ) {
                TextPreference(
                    title = moduleTitle,
                    onClick = { navigator.push(Route.ModuleSettings) },
                )
                TextPreference(
                    title = aboutTitle,
                    onClick = { navigator.push(Route.About) },
                )
            }

            // 悬浮底栏压在列表上方，留出让最后一项能滚出遮挡的空间
            item { Spacer(modifier = Modifier.height(112.dp)) }
        }

        HyperVisageBottomBar(
            items = listOf(
                HyperVisageBottomBarItem(MiuixIcons.Home, navHome),
                HyperVisageBottomBarItem(MiuixIcons.Settings, navSettings),
            ),
            selectedIndex = selectedTab,
            onSelect = { index ->
                selectedTab = index
                if (index == 1) navigator.push(Route.ModuleSettings)
            },
            actionIcon = MiuixIcons.Info,
            actionContentDescription = navAbout,
            onActionClick = { navigator.push(Route.About) },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
