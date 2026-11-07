@file:Suppress("DiscouragedApi")

// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.hook.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import com.highcapable.kavaref.KavaRef.Companion.resolve
import dev.apanzinc.hypervisage.hook.utils.ContextHolder
import dev.apanzinc.hypervisage.hook.utils.MLog

/**
 * 目标进程刚起来时就需要拿到 Context 的 Hooker 继承它。
 * 内部 hook 的是 Application.attachBaseContext，拿到 Context 后再回调 onReady。
 */
abstract class ContextAwareHooker : StaticHooker() {
    abstract val targetPackage: String

    private var ready = false

    private val attachBaseContext by lazy {
        "android.app.Application".toClass().resolve().firstMethod {
            name = "attachBaseContext"
            parameters(Context::class)
            superclass()
        }
    }

    final override fun onHook() {
        attachBaseContext.hook {
            if (!ready) {
                val ctx = getArg(0) as? Context
                if (ctx != null) {
                    ready = true
                    ContextHolder.set(ctx)
                    runCatching { ContextScope(ctx, targetPackage).onReady() }
                        .onFailure { MLog.e(hookerName, "onReady failed", it) }
                }
            }
            result(proceed())
        }
    }

    abstract fun ContextScope.onReady()
}

/** 目标应用自己的资源与上下文封装，写 Hook 时基本离不开它 */
class ContextScope(
    val context: Context,
    val pkgName: String,
) {
    val res: Resources get() = context.resources

    private fun String.id(type: String): Int = res.getIdentifier(this, type, pkgName)

    fun String.toId(): Int = id("id")
    fun String.toLayoutId(): Int = id("layout")
    fun String.toDrawableId(): Int = id("drawable")
    fun String.toDimenId(): Int = id("dimen")
    fun String.toStringId(): Int = id("string")
    fun String.toArrayId(): Int = id("array")
    fun String.toBoolId(): Int = id("bool")
    fun String.toStyleId(): Int = id("style")

    fun String.getString(): String = res.getString(toStringId())
    fun String.getDimen(): Float = res.getDimension(toDimenId())
    fun Int.dp2px(): Int = (this * res.displayMetrics.density + 0.5f).toInt()
}
