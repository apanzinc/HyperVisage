// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.hook.utils

import android.content.Context

/**
 * 目标进程的 Application Context。
 * ContextAwareHooker 在 attachBaseContext 阶段拿到后塞进来，
 * 之后各个 Rule 就不用再自己去找 Context 了。
 */
object ContextHolder {
    @Volatile
    private var appContext: Context? = null

    fun set(context: Context) {
        if (appContext == null) appContext = context.applicationContext ?: context
    }

    val context: Context?
        get() = appContext
}
