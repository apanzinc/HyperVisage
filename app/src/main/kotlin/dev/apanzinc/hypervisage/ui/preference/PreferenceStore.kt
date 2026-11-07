// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.preference

import android.content.SharedPreferences
import androidx.core.content.edit
import dev.apanzinc.hypervisage.data.preference.Preferences
import dev.apanzinc.hypervisage.manager.XposedServiceManager
import dev.lackluster.hyperx.ui.preference.core.PreferenceKey
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 设置读写。
 *
 * 正常情况下走 XposedService 提供的 remote preferences（跨进程，目标应用进程能读到）。
 * LSPosed 没连上时退回内存/本地值，保证 UI 不崩，只是改了不生效。
 */
class PreferenceStore(
    private val serviceManager: XposedServiceManager,
) {
    private val _updates = MutableSharedFlow<PreferenceKey<*>>(
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val updates = _updates.asSharedFlow()

    val isServiceAvailable: Boolean
        get() = serviceManager.currentService != null

    private val prefs: SharedPreferences?
        get() = runCatching {
            serviceManager.currentService?.getRemotePreferences(Preferences.NAME)
        }.getOrNull()

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(key: PreferenceKey<T>): T {
        val sp = prefs ?: return key.default
        return when (val def = key.default) {
            is Boolean -> sp.getBoolean(key.name, def) as T
            is Int -> sp.getInt(key.name, def) as T
            is Long -> sp.getLong(key.name, def) as T
            is Float -> sp.getFloat(key.name, def) as T
            is String -> (sp.getString(key.name, def) ?: def) as T
            is Set<*> -> {
                val fallback = (def as? Set<String>) ?: emptySet()
                (sp.getStringSet(key.name, fallback) ?: fallback) as T
            }
            else -> def
        }
    }

    fun <T : Any> put(key: PreferenceKey<T>, value: T) {
        prefs?.edit {
            when (value) {
                is Boolean -> putBoolean(key.name, value)
                is Int -> putInt(key.name, value)
                is Long -> putLong(key.name, value)
                is Float -> putFloat(key.name, value)
                is String -> putString(key.name, value)
                is Set<*> -> putStringSet(key.name, value.filterIsInstance<String>().toSet())
            }
        }
        _updates.tryEmit(key)
    }

    /** LSPosed 连上之后重新广播一次，让所有页面重读 */
    fun notifyAllReloaded() {
        _updates.tryEmit(Preferences.Module.ENABLED)
    }
}
