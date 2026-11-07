// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.di

import dev.apanzinc.hypervisage.manager.XposedServiceManager
import dev.apanzinc.hypervisage.ui.preference.AppPreferenceActions
import dev.apanzinc.hypervisage.ui.preference.AppSettings
import dev.apanzinc.hypervisage.ui.preference.PreferenceStore
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    // 跟着进程启动就注册监听，所以要 createdAtStart
    singleOf(::XposedServiceManager) { createdAtStart() }
    singleOf(::PreferenceStore)
    // androidContext() 已注册 Context，get() 会解析到 Application
    single { AppSettings(get()) }
    singleOf(::AppPreferenceActions)
}
