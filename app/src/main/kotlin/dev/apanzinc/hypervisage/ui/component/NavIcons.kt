// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home as OutlineHome
import androidx.compose.material.icons.outlined.Info as OutlineInfo
import androidx.compose.material.icons.outlined.Search as OutlineSearch
import androidx.compose.material.icons.outlined.Settings as OutlineSettings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 底栏图标的线性 / 面性配对。选中用面性，未选中用线性，和小米官方底栏一致。
 * 两套都来自 Material Icons（Apache-2.0），见 NOTICE.md。
 */
object NavIcons {
    val HomeLine: ImageVector get() = Icons.Outlined.OutlineHome
    val HomeFill: ImageVector get() = Icons.Filled.Home

    val SettingsLine: ImageVector get() = Icons.Outlined.OutlineSettings
    val SettingsFill: ImageVector get() = Icons.Filled.Settings

    val AboutLine: ImageVector get() = Icons.Outlined.OutlineInfo
    val AboutFill: ImageVector get() = Icons.Filled.Info

    val SearchLine: ImageVector get() = Icons.Outlined.OutlineSearch
    val SearchFill: ImageVector get() = Icons.Filled.Search
}
