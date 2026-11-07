// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.data

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 全应用的导航目的地。
 * navigation3 用 @Serializable 的 NavKey 做类型安全路由，
 * 新增页面：加一个 data object，再到 MainActivity 的 customEntryProvider 里挂上。
 */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Main : Route

    @Serializable
    data object ModuleSettings : Route

    @Serializable
    data object Example : Route

    @Serializable
    data object About : Route
}
