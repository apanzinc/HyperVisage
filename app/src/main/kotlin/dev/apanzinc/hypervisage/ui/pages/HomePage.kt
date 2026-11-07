// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.apanzinc.hypervisage.R
import dev.apanzinc.hypervisage.data.Route
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.TextPreference
import dev.lackluster.hyperx.ui.preference.itemPreferenceGroup

/**
 * 主界面。作为主入口页，把返回按钮去掉。
 */
@Composable
fun HomePage() {
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
                onClick = { navigator.push(Route.ModuleSettings) },
            )
            TextPreference(
                title = aboutTitle,
                onClick = { navigator.push(Route.About) },
            )
        }
    }
}
