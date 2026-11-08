// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.apanzinc.hypervisage.R
import dev.apanzinc.hypervisage.data.Route
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.TextPreference
import dev.lackluster.hyperx.ui.preference.itemPreferenceGroup

/**
 * 三个顶层标签页里的「主页」。底栏由 [MainTabsScreen] 统一叠在 Pager 上方，
 * 这里只负责自己的内容，不再 push 设置 / 关于。
 */
@Composable
fun HomePage(onOpenTab: (Int) -> Unit) {
    val navigator = LocalNavigator.current

    // HyperXPage 的 content 是 LazyListScope，不是 @Composable 上下文，
    // 所以字符串要在这里先取好。
    val title = stringResource(R.string.page_home)
    val scopesTitle = stringResource(R.string.home_group_scopes)
    val othersTitle = stringResource(R.string.home_group_other)
    val exampleTitle = stringResource(R.string.page_example)
    val moduleTitle = stringResource(R.string.page_module_settings)
    val aboutTitle = stringResource(R.string.page_about)

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
                onClick = { onOpenTab(TabSettings) },
            )
            TextPreference(
                title = aboutTitle,
                onClick = { onOpenTab(TabAbout) },
            )
        }

        // 底栏是独立一行，列表末尾只留少量呼吸空间
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
