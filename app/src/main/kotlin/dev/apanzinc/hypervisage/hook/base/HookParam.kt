// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.hook.base

import android.content.pm.ApplicationInfo

/**
 * 每次 Hook 所需的上下文信息，跟随 Hooker 树往下传。
 */
data class HookParam(
    val processName: String,
    val packageName: String,
    val isSystemServer: Boolean = false,
    val isFirstPackage: Boolean = false,
    val isPackageReady: Boolean = false,
    val appInfo: ApplicationInfo? = null,
) {
    val isMainProcess: Boolean
        get() = processName == packageName
}
