// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.data

import dev.apanzinc.hypervisage.BuildConfig

object Constants {
    const val TAG = "HyperVisage"

    /** 模块自己的包名，用于判断进程归属 */
    val MODULE_PACKAGE: String = BuildConfig.APPLICATION_ID

    /** 共享偏好的文件名，UI 与 Hook 进程都读这一份 */
    const val PREF_NAME = "config"
}

/**
 * 目标应用包名常量。
 * 新增一个 Hook 目标时，在这里加一行，再去 hook/scopes 建对应的 Hooker。
 */
object Scope {
    const val SYSTEM = "system"
    const val SYSTEM_UI = "com.android.systemui"
    const val SYSTEM_UI_PLUGIN = "miui.systemui.plugin"
    const val MIUI_HOME = "com.miui.home"
    const val SETTINGS = "com.android.settings"
    const val MI_SETTINGS = "com.xiaomi.misettings"
    const val SECURITY_CENTER = "com.miui.securitycenter"
    const val THEMES = "com.android.thememanager"
    const val NOTIFICATION = "com.android.notification"

    /** 全部可选范围，UI 上用来生成列表 */
    val ALL: List<String> = listOf(
        SYSTEM_UI,
        SYSTEM_UI_PLUGIN,
        MIUI_HOME,
        SETTINGS,
        MI_SETTINGS,
        SECURITY_CENTER,
        THEMES,
    )
}
