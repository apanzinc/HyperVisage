// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.preference

import dev.lackluster.hyperx.ui.preference.core.PreferenceActions
import dev.lackluster.hyperx.ui.preference.core.PreferenceKey
import kotlinx.coroutines.flow.Flow

/**
 * 把 PreferenceStore 接到 hyperx 的偏好组件上。
 * 提供了它，SwitchPreference / SeekBarPreference 之类只要传 PreferenceKey 就自动读写。
 */
class AppPreferenceActions(
    private val store: PreferenceStore,
) : PreferenceActions {

    override val preferenceUpdates: Flow<PreferenceKey<*>>
        get() = store.updates

    override fun <T : Any> get(key: PreferenceKey<T>): T = store.get(key)

    override fun <T : Any> update(key: PreferenceKey<T>, value: T) = store.put(key, value)
}
