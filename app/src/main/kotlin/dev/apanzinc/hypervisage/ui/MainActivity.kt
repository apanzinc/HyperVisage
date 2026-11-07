// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.NavEntry
import dev.apanzinc.hypervisage.data.Route
import dev.apanzinc.hypervisage.ui.pages.AboutPage
import dev.apanzinc.hypervisage.ui.pages.ExamplePage
import dev.apanzinc.hypervisage.ui.pages.HomePage
import dev.apanzinc.hypervisage.ui.pages.ModuleSettingsPage
import dev.apanzinc.hypervisage.ui.preference.AppPreferenceActions
import dev.lackluster.hyperx.core.HyperXActivity
import dev.lackluster.hyperx.ui.layout.HyperXAppLayout
import dev.lackluster.hyperx.ui.layout.HyperXLayoutConfig
import dev.lackluster.hyperx.ui.preference.core.LocalPreferenceActions
import org.koin.compose.koinInject

class MainActivity : HyperXActivity() {

    @Composable
    override fun AppContent() {
        val preferenceActions: AppPreferenceActions = koinInject()

        // 以后可以接上"是否分屏""模糊强度"这类模块自己的外观设置
        val layoutConfig = HyperXLayoutConfig(
            isSplitScreenEnabled = true,
            isBlurEnabled = true,
        )

        CompositionLocalProvider(LocalPreferenceActions provides preferenceActions) {
            HyperXAppLayout(
                config = layoutConfig,
                primaryContent = { HomePage() },
                customEntryProvider = { key ->
                    NavEntry(key) {
                        when (key) {
                            is Route.ModuleSettings -> ModuleSettingsPage()
                            is Route.Example -> ExamplePage()
                            is Route.About -> AboutPage()
                            else -> {}
                        }
                    }
                },
            )
        }
    }
}
