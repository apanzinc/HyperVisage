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
import dev.apanzinc.hypervisage.data.preference.Preferences
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.SwitchPreference
import dev.lackluster.hyperx.ui.preference.itemPreferenceGroup

/**
 * 示例功能的设置页。写新功能时照这个改：
 * 1. Preferences 里加 key
 * 2. 这里加 SwitchPreference
 * 3. hook/rules 里加一个 Rule 订阅这个 key
 */
@Composable
fun ExamplePage() {
    val title = stringResource(R.string.page_example)
    val switchTitle = stringResource(R.string.example_switch_title)
    val switchSummary = stringResource(R.string.example_switch_summary)
    val toastTitle = stringResource(R.string.example_toast_title)
    val toastSummary = stringResource(R.string.example_toast_summary)

    HyperXPage(title = title) {
        itemPreferenceGroup(position = ItemPosition.Single) {
            SwitchPreference(
                key = Preferences.Example.ENABLED,
                title = switchTitle,
                summary = switchSummary,
            )
            SwitchPreference(
                key = Preferences.Example.SHOW_TOAST,
                title = toastTitle,
                summary = toastSummary,
            )
        }
        // 底栏是窗口级常驻覆盖层，二级页同样要留出空间让最后一项滚出遮挡
        item { Spacer(modifier = Modifier.height(112.dp)) }
    }
}
