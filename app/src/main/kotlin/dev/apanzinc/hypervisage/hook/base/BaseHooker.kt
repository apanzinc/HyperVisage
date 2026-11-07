// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.hook.base

import com.highcapable.kavaref.extension.toClass
import com.highcapable.kavaref.extension.toClassOrNull
import com.highcapable.kavaref.resolver.ConstructorResolver
import com.highcapable.kavaref.resolver.MethodResolver
import dev.apanzinc.hypervisage.hook.utils.MLog
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Executable
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Hooker 树的一个节点。
 *
 * 生效条件 = 自己开启 && 所有父节点开启。
 * 状态变化时：
 *  - 变开启 -> onHook()，子节点跟着递归
 *  - 变关闭 -> DynamicHooker 逐个 unhook；StaticHooker 无法卸载，只记日志
 */
sealed class BaseHooker {
    lateinit var module: XposedModule
    lateinit var classLoader: ClassLoader
    lateinit var hookParam: HookParam

    private val handles = CopyOnWriteArraySet<XposedInterface.HookHandle>()
    private val children = CopyOnWriteArraySet<BaseHooker>()

    private var selfEnabled = true
    private var parentEnabled = false

    val isHooked: Boolean get() = handles.isNotEmpty()

    /** 子类可覆盖，声明自己需要的元信息 */
    open val hookerName: String get() = this::class.java.simpleName

    val isMainProcess: Boolean get() = hookParam.isMainProcess

    private val effectiveEnabled: Boolean get() = selfEnabled && parentEnabled

    @Synchronized
    fun setSelfEnabled(enabled: Boolean) {
        val before = effectiveEnabled
        selfEnabled = enabled
        applyState(before)
    }

    @Synchronized
    fun setParentEnabled(enabled: Boolean) {
        val before = effectiveEnabled
        parentEnabled = enabled
        applyState(before)
    }

    private fun applyState(before: Boolean) {
        val after = effectiveEnabled
        if (before == after) return

        if (after) {
            if (!isHooked) {
                MLog.d { "hook on: $hookerName" }
                onHook()
            }
        } else {
            when (this) {
                is DynamicHooker -> {
                    MLog.d { "unhook: $hookerName" }
                    handles.forEach { runCatching { it.unhook() } }
                    handles.clear()
                }
                is StaticHooker -> {
                    MLog.d { "static hooker can not unhook, skipped: $hookerName" }
                }
            }
        }

        children.forEach { it.setParentEnabled(after) }
    }

    /** 在 Hook 之前调用，用来做一些不依赖 Hook 的准备工作 */
    open fun onInit() {}

    /** 真正挂 Hook 的地方 */
    open fun onHook() {}

    internal fun performInit() {
        runCatching { onInit() }
            .onFailure { MLog.e("onInit failed in $hookerName", it) }
    }

    /** 把子 Hooker 挂到这棵树上 */
    fun attach(
        hooker: BaseHooker,
        customClassLoader: ClassLoader? = null,
        param: HookParam? = null,
    ) {
        if (children.contains(hooker)) return

        hooker.module = module
        hooker.classLoader = customClassLoader ?: this.classLoader
        hooker.hookParam = param ?: this.hookParam

        children.add(hooker)
        hooker.performInit()
        hooker.setParentEnabled(effectiveEnabled)
    }

    /** 挂一个 Hook，并把 handle 记下来以便后续卸载 */
    fun Executable.hook(
        managed: Boolean = true,
        callback: HookScope.() -> HookResult,
    ): XposedInterface.HookHandle {
        val raw = module.hook(this).intercept { chain ->
            HookScopeImpl(chain).callback().value
        }

        if (!managed) return raw

        val tracked = object : XposedInterface.HookHandle {
            override fun getExecutable(): Executable = raw.executable

            override fun unhook() {
                raw.unhook()
                handles.remove(this)
            }
        }
        handles.add(tracked)
        return tracked
    }

    fun <T : Any> MethodResolver<T>.hook(
        managed: Boolean = true,
        callback: HookScope.() -> HookResult,
    ) = self.hook(managed, callback)

    fun <T : Any> ConstructorResolver<T>.hook(
        managed: Boolean = true,
        callback: HookScope.() -> HookResult,
    ) = self.hook(managed, callback)

    @JvmName("hookAllMethods")
    fun <T : Any> Iterable<MethodResolver<T>?>.hookAll(
        managed: Boolean = true,
        callback: HookScope.() -> HookResult,
    ) = mapNotNull { it?.hook(managed, callback) }

    @JvmName("hookAllExecutables")
    fun Iterable<Executable?>.hookAll(
        managed: Boolean = true,
        callback: HookScope.() -> HookResult,
    ) = mapNotNull { it?.hook(managed, callback) }

    fun String.toClass(initialize: Boolean = false): Class<Any> =
        this.toClass(loader = classLoader, initialize = initialize)

    fun String.toClassOrNull(initialize: Boolean = false): Class<Any>? =
        this.toClassOrNull(loader = classLoader, initialize = initialize)

    fun String.lazyClass(initialize: Boolean = false): Lazy<Class<Any>> =
        lazy { toClass(initialize) }

    fun String.lazyClassOrNull(initialize: Boolean = false): Lazy<Class<Any>?> =
        lazy { toClassOrNull(initialize) }
}

private class HookScopeImpl(
    chain: XposedInterface.Chain,
) : HookScope, XposedInterface.Chain by chain

/** 挂上就不能卸载，适合 hook 早期启动流程 */
abstract class StaticHooker : BaseHooker()

/** 支持运行时卸载，适合跟随设置开关切换 */
abstract class DynamicHooker : BaseHooker()
