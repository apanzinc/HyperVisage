// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.hook.scopes

import dev.apanzinc.hypervisage.hook.base.StaticHooker
import dev.apanzinc.hypervisage.hook.utils.MLog

/**
 * system_server 进程的入口，用来改 framework 层（ActivityTaskManager、WindowManager 之类）。
 * 这里的 Hook 影响全局，改坏了容易卡开机，调试时务必先准备救砖手段。
 */
object AndroidFramework : StaticHooker() {

    override fun onHook() {
        MLog.i { "system_server hook attached" }
        // TODO: 挂 framework 层美化相关的方法
    }
}
