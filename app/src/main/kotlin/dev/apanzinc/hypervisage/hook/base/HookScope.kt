// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.hook.base

import io.github.libxposed.api.XposedInterface

/**
 * 包在 libxposed 的 Chain 外面，让 Hook 回调能直接 return 一个值当作方法结果。
 *
 * 用法：
 * ```
 * someMethod.hook {
 *     if (!Preferences.Example.ENABLED.get()) return@hook proceed()
 *     val arg = getArg(0)
 *     result(handle(arg))
 * }
 * ```
 */
interface HookScope : XposedInterface.Chain {
    fun result(value: Any?): HookResult = HookResult(value)
}

@JvmInline
value class HookResult internal constructor(val value: Any?)
