// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.apanzinc.hypervisage.R
import dev.apanzinc.hypervisage.data.preference.Preferences
import dev.apanzinc.hypervisage.ui.preference.AppSettings
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.DropDownEntry
import dev.lackluster.hyperx.ui.preference.DropDownPreference
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.SwitchPreference
import dev.lackluster.hyperx.ui.preference.itemPreferenceGroup
import org.koin.compose.koinInject

@Composable
fun ModuleSettingsPage() {
    val appSettings: AppSettings = koinInject()
    val themeMode by appSettings.themeMode.collectAsState()
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
    val themeTitle = stringResource(R.string.module_theme_title)
    val themeSummary = stringResource(R.string.module_theme_summary)
    val themeSystem = stringResource(R.string.module_theme_system)
    val themeLight = stringResource(R.string.module_theme_light)
    val themeDark = stringResource(R.string.module_theme_dark)

    // 顶层标签页没有返回箭头，底栏由 MainTabsScreen 统一叠放
    HyperXPage(title = title, navigationIcon = {}) {
        itemPreferenceGroup(position = ItemPosition.Single) {
            DropDownPreference(
                title = themeTitle,
                summary = themeSummary,
                value = themeMode,
                entries = listOf(
                    DropDownEntry(Preferences.App.THEME_SYSTEM, themeSystem),
                    DropDownEntry(Preferences.App.THEME_LIGHT, themeLight),
                    DropDownEntry(Preferences.App.THEME_DARK, themeDark),
                ),
                onValueChange = appSettings::setThemeMode,
            )
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

        // 底栏是压在内容上的模糊覆盖层，留出让最后一项滚出遮挡的空间
        item { Spacer(modifier = Modifier.height(112.dp)) }
    }
}
