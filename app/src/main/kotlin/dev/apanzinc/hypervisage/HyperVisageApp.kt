// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import dev.apanzinc.hypervisage.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class HyperVisageApp : Application() {

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        startKoin {
            androidLogger()
            androidContext(this@HyperVisageApp)
            modules(appModule)
        }
    }
}
