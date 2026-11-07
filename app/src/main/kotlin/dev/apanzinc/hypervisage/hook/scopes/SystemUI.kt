// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.hook.scopes

import dev.apanzinc.hypervisage.data.Scope
import dev.apanzinc.hypervisage.hook.base.ContextAwareHooker
import dev.apanzinc.hypervisage.hook.base.ContextScope
import dev.apanzinc.hypervisage.hook.rules.ExampleRule

/**
 * SystemUI 这个目标的入口。
 * 想加新功能：在 onReady 里 attach 新的 Rule 即可。
 */
object SystemUI : ContextAwareHooker() {

    override val targetPackage: String = Scope.SYSTEM_UI

    override fun ContextScope.onReady() {
        attach(ExampleRule)
    }
}
