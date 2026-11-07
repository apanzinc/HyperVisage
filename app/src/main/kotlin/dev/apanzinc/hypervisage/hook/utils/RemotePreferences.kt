// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.hook.utils

import android.content.SharedPreferences
import dev.apanzinc.hypervisage.data.preference.Preferences
import dev.lackluster.hyperx.ui.preference.core.PreferenceKey
import io.github.libxposed.api.XposedModule
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Hook 进程侧读取设置。
 *
 * module.getRemotePreferences() 让目标应用进程能读到模块自己写入的 SharedPreferences，
 * 所以设置改完不用重启也能立刻生效（具体由各 Hooker 决定要不要立即重挂）。
 */
object RemotePreferences {
    private var prefs: SharedPreferences? = null

    private val observers = ConcurrentHashMap<String, CopyOnWriteArraySet<() -> Unit>>()
    private var listenerRegistered = false

    private val globalListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == null) return@OnSharedPreferenceChangeListener
            observers[key]?.forEach { runCatching { it.invoke() } }
        }

    fun init(module: XposedModule) {
        prefs = module.getRemotePreferences(Preferences.NAME)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> read(key: PreferenceKey<T>): T {
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

    fun <T : Any> PreferenceKey<T>.get(): T = read(this)

    fun <T : Any> PreferenceKey<T>.lazyGet(): Lazy<T> = lazy { read(this) }

    /**
     * 监听某个 key 的变化。
     * fireImmediately = true 时会先用当前值回调一次，省掉手动初始化。
     */
    fun <T : Any> PreferenceKey<T>.observe(
        fireImmediately: Boolean = true,
        action: (T) -> Unit,
    ): () -> Unit {
        val sp = prefs ?: return {}

        if (!listenerRegistered) {
            listenerRegistered = true
            sp.registerOnSharedPreferenceChangeListener(globalListener)
        }

        val slot = observers.getOrPut(this.name) { CopyOnWriteArraySet() }
        val wrapped: () -> Unit = { action(read(this)) }
        slot.add(wrapped)

        if (fireImmediately) action(read(this))

        return {
            slot.remove(wrapped)
            if (slot.isEmpty()) observers.remove(this.name)
        }
    }
}
