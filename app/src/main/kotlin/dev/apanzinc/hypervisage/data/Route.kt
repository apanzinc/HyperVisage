// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.data

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 二级页面路由。主页 / 设置 / 关于是并行的顶层标签页，不走导航栈，
 * 所以这里只放真正需要从标签页 push 出来的页面。
 */
@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Example : Route

    @Serializable
    data object Search : Route
}
