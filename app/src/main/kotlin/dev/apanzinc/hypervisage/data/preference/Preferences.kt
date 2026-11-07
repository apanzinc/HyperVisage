// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.data.preference

import dev.lackluster.hyperx.ui.preference.core.PreferenceKey
import dev.apanzinc.hypervisage.data.Constants

/**
 * 所有设置项的唯一定义处。
 * UI 侧用 rememberPreferenceState 读写，Hook 侧用 RemotePreferences.get() 读取，
 * 两边靠同一个 name 字符串对齐，所以 key 名不要随便改。
 */
object Preferences {
    const val NAME = Constants.PREF_NAME

    object Module {
        val ENABLED = PreferenceKey("module_enabled", true)
        val DEBUG = PreferenceKey("module_debug", false)
    }

    object Example {
        val ENABLED = PreferenceKey("example_enabled", false)
        val SHOW_TOAST = PreferenceKey("example_show_toast", false)
    }
}
