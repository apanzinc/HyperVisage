// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage

import dev.apanzinc.hypervisage.data.Scope
import dev.apanzinc.hypervisage.data.preference.Preferences
import dev.apanzinc.hypervisage.hook.base.BaseHooker
import dev.apanzinc.hypervisage.hook.base.HookParam
import dev.apanzinc.hypervisage.hook.scopes.AndroidFramework
import dev.apanzinc.hypervisage.hook.scopes.SystemUI
import dev.apanzinc.hypervisage.hook.utils.MLog
import dev.apanzinc.hypervisage.hook.utils.RemotePreferences
import dev.apanzinc.hypervisage.hook.utils.RemotePreferences.get
import dev.apanzinc.hypervisage.hook.utils.RemotePreferences.observe
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface
import java.util.concurrent.ConcurrentHashMap

/**
 * 模块唯一入口，类名必须与 META-INF/xposed/java_init.list 里一致。
 *
 * 流程：框架加载模块 -> onModuleLoaded 初始化日志/设置 -> 目标进程被拉起时
 * onPackageLoaded / onSystemServerStarting 分发到对应的 scope Hooker。
 */
class HookEntry : XposedModule() {

    private var processName: String = ""
    private var isSystemServer: Boolean = false
    private var moduleEnabled: Boolean = true

    /** 同一个进程可能被回调多次，用这个集合去重 */
    private val injectedPackages = ConcurrentHashMap.newKeySet<String>()

    override fun onModuleLoaded(param: XposedModuleInterface.ModuleLoadedParam) {
        processName = param.processName
        isSystemServer = param.isSystemServer

        MLog.init(this)
        RemotePreferences.init(this)

        Preferences.Module.DEBUG.observe { enabled -> MLog.isDebugEnabled = enabled }
        moduleEnabled = Preferences.Module.ENABLED.get()

        MLog.d { "module loaded in $processName (systemServer=$isSystemServer, enabled=$moduleEnabled)" }
    }

    override fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        if (!moduleEnabled) return

        val hookParam = HookParam(
            processName = processName,
            packageName = Scope.SYSTEM,
            isSystemServer = true,
        )
        attachHooker(AndroidFramework, param.classLoader, hookParam)
    }

    override fun onPackageLoaded(param: XposedModuleInterface.PackageLoadedParam) {
        if (!moduleEnabled) return
        if (!injectedPackages.add(param.packageName)) return

        val hooker = hookerOf(param.packageName) ?: return

        val hookParam = HookParam(
            processName = processName,
            packageName = param.packageName,
            isSystemServer = isSystemServer,
            isFirstPackage = param.isFirstPackage,
            appInfo = param.applicationInfo,
        )
        attachHooker(hooker, param.defaultClassLoader, hookParam)
    }

    override fun onPackageReady(param: XposedModuleInterface.PackageReadyParam) {
        MLog.d { "package ready: ${param.packageName}" }
    }

    /**
     * 包名 -> Hooker 的映射表。
     * 加新目标：data/Scope 加常量，hook/scopes 建 Hooker，这里加一个分支。
     */
    private fun hookerOf(pkg: String): BaseHooker? = when (pkg) {
        Scope.SYSTEM_UI -> SystemUI
        else -> null
    }

    private fun attachHooker(
        hooker: BaseHooker,
        classLoader: ClassLoader,
        param: HookParam,
    ) {
        hooker.module = this
        hooker.classLoader = classLoader
        hooker.hookParam = param

        hooker.performInit()
        hooker.setParentEnabled(true)
    }
}
