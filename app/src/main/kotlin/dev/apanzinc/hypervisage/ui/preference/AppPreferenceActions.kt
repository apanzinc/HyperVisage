// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.preference

import dev.apanzinc.hypervisage.data.preference.Preferences
import dev.lackluster.hyperx.ui.preference.core.PreferenceActions
import dev.lackluster.hyperx.ui.preference.core.PreferenceKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

/**
 * 把 PreferenceStore 接到 hyperx 的偏好组件上。
 * 提供了它，SwitchPreference / SeekBarPreference 之类只要传 PreferenceKey 就自动读写。
 *
 * App.* 这类只影响本 App 界面的设置走 AppSettings（本地存储），
 * 其余仍走跨进程的 remote preferences。
 */
class AppPreferenceActions(
    private val store: PreferenceStore,
    private val appSettings: AppSettings,
) : PreferenceActions {

    override val preferenceUpdates: Flow<PreferenceKey<*>> =
        merge(store.updates, appSettings.updates)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: PreferenceKey<T>): T = when (key) {
        Preferences.App.PREDICTIVE_BACK -> appSettings.predictiveBack.value as T
        Preferences.App.GLASS_BLUR -> appSettings.glassBlur.value as T
        Preferences.App.STANDARD_BOTTOM_BAR -> appSettings.standardBottomBar.value as T
        else -> store.get(key)
    }

    override fun <T : Any> update(key: PreferenceKey<T>, value: T) {
        when (key) {
            Preferences.App.PREDICTIVE_BACK -> appSettings.setPredictiveBack(value as Boolean)
            Preferences.App.GLASS_BLUR -> appSettings.setGlassBlur(value as Boolean)
            Preferences.App.STANDARD_BOTTOM_BAR -> appSettings.setStandardBottomBar(value as Boolean)
            else -> store.put(key, value)
        }
    }
}
