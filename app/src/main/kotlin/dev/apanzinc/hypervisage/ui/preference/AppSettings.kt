// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.preference

import android.content.Context
import androidx.core.content.edit
import dev.apanzinc.hypervisage.data.preference.Preferences
import dev.lackluster.hyperx.ui.preference.core.PreferenceKey
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 只影响本 App 界面的设置，存本地 SharedPreferences。
 * 不依赖 LSPosed 服务，模块没激活时开关也能记住并生效。
 */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("hypervisage_app_settings", Context.MODE_PRIVATE)

    private val _updates = MutableSharedFlow<PreferenceKey<*>>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val updates: Flow<PreferenceKey<*>> = _updates.asSharedFlow()

    private val _predictiveBack = MutableStateFlow(
        prefs.getBoolean(Preferences.App.PREDICTIVE_BACK.name, false),
    )
    val predictiveBack = _predictiveBack.asStateFlow()

    fun setPredictiveBack(enabled: Boolean) {
        prefs.edit { putBoolean(Preferences.App.PREDICTIVE_BACK.name, enabled) }
        _predictiveBack.value = enabled
        _updates.tryEmit(Preferences.App.PREDICTIVE_BACK)
    }
}
