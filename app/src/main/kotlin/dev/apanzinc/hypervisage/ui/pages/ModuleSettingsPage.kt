// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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

    HyperXPage(title = title) {
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
        }
    }
}
