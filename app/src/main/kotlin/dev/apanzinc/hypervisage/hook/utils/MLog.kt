// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.hook.utils

import android.util.Log
import dev.apanzinc.hypervisage.BuildConfig
import dev.apanzinc.hypervisage.data.Constants
import io.github.libxposed.api.XposedModule

/**
 * 日志同时走 logcat 和 LSPosed 日志面板，方便在真机上排查。
 * 详细日志受「调试日志」开关控制。
 */
object MLog {
    const val TAG = Constants.TAG

    @Volatile
    private var module: XposedModule? = null

    @Volatile
    var isDebugEnabled: Boolean = BuildConfig.DEBUG
        set(value) {
            if (field == value) return
            field = value
            if (value) d { "debug log enabled" }
        }

    fun init(module: XposedModule) {
        this.module = module
    }

    inline fun d(msg: () -> String) {
        if (isDebugEnabled) print(Log.DEBUG, TAG, msg(), null)
    }

    inline fun i(msg: () -> String) {
        print(Log.INFO, TAG, msg(), null)
    }

    inline fun w(msg: () -> String) {
        print(Log.WARN, TAG, msg(), null)
    }

    fun e(message: String, t: Throwable? = null) {
        print(Log.ERROR, TAG, message, t)
    }

    fun e(tag: String, message: String, t: Throwable? = null) {
        print(Log.ERROR, tag, message, t)
    }

    @PublishedApi
    internal fun print(priority: Int, tag: String, msg: String, t: Throwable?) {
        when (priority) {
            Log.DEBUG -> Log.d(tag, msg, t)
            Log.INFO -> Log.i(tag, msg, t)
            Log.WARN -> Log.w(tag, msg, t)
            Log.ERROR -> Log.e(tag, msg, t)
            else -> Log.v(tag, msg, t)
        }
        runCatching {
            if (t != null) module?.log(priority, tag, msg, t) else module?.log(priority, tag, msg)
        }
    }
}
