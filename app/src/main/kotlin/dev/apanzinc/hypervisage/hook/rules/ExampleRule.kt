// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.hook.rules

import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.highcapable.kavaref.KavaRef.Companion.resolve
import dev.apanzinc.hypervisage.data.preference.Preferences
import dev.apanzinc.hypervisage.hook.base.DynamicHooker
import dev.apanzinc.hypervisage.hook.utils.ContextHolder
import dev.apanzinc.hypervisage.hook.utils.MLog
import dev.apanzinc.hypervisage.hook.utils.RemotePreferences.get
import dev.apanzinc.hypervisage.hook.utils.RemotePreferences.observe

/**
 * 示例 Rule，演示完整的"设置 <-> Hook"链路：
 *
 * 1. onInit 里订阅开关键，设置页一改，这边自动挂/卸 Hook
 * 2. onHook 里挂真实方法，回调中读另一个开关决定行为
 *
 * 写自己的功能时复制这个文件，改 onHook 就行。
 */
object ExampleRule : DynamicHooker() {

    private val hookTarget by lazy {
        "android.app.Application".toClass().resolve().firstMethod { name = "onCreate" }
    }

    override fun onInit() {
        Preferences.Example.ENABLED.observe { enabled ->
            MLog.d { "ExampleRule enabled -> $enabled" }
            setSelfEnabled(enabled)
        }
    }

    override fun onHook() {
        hookTarget.hook {
            // 先让原方法跑完，再动自己的逻辑
            val ret = proceed()

            if (Preferences.Example.SHOW_TOAST.get()) {
                runCatching {
                    ContextHolder.context?.let { ctx ->
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(ctx, "HyperVisage 已注入 ${hookParam.packageName}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.onFailure { MLog.e(hookerName, "toast failed", it) }
            }

            result(ret)
        }
    }
}
