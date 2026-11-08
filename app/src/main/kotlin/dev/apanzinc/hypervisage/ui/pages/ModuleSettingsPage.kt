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

@Composable
fun ModuleSettingsPage() {
    val title = stringResource(R.string.page_module_settings)
    val enabledTitle = stringResource(R.string.module_enabled_title)
    val enabledSummary = stringResource(R.string.module_enabled_summary)
    val debugTitle = stringResource(R.string.module_debug_title)
    val debugSummary = stringResource(R.string.module_debug_summary)
    val predictiveBackTitle = stringResource(R.string.module_predictive_back_title)
    val predictiveBackSummary = stringResource(R.string.module_predictive_back_summary)
    val glassBlurTitle = stringResource(R.string.module_glass_blur_title)
    val glassBlurSummary = stringResource(R.string.module_glass_blur_summary)
    val standardBottomBarTitle = stringResource(R.string.module_standard_bottom_bar_title)
    val standardBottomBarSummary = stringResource(R.string.module_standard_bottom_bar_summary)

    // 顶层标签页没有返回箭头，底栏由 MainTabsScreen 统一叠放
    HyperXPage(title = title, navigationIcon = {}) {
        itemPreferenceGroup(position = ItemPosition.Single) {
            SwitchPreference(
                key = Preferences.Module.ENABLED,
                title = enabledTitle,
                summary = enabledSummary,
            )
            SwitchPreference(
                key = Preferences.Module.DEBUG,
                title = debugTitle,
                summary = debugSummary,
            )
            SwitchPreference(
                key = Preferences.App.PREDICTIVE_BACK,
                title = predictiveBackTitle,
                summary = predictiveBackSummary,
            )
            SwitchPreference(
                key = Preferences.App.GLASS_BLUR,
                title = glassBlurTitle,
                summary = glassBlurSummary,
            )
            SwitchPreference(
                key = Preferences.App.STANDARD_BOTTOM_BAR,
                title = standardBottomBarTitle,
                summary = standardBottomBarSummary,
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
