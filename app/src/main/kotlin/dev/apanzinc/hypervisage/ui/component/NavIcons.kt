// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.component

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Settings

/**
 * 底栏图标改用 miuix-icons 0.9.4 内置的小米官方 misymbol 图标，见 NOTICE.md。
 *
 * 这套图标是从小米 `misymbol_vf.ttf` 图标字体里按字重提取的，只有
 * Light / Normal / Regular / Medium / Demibold 五档，没有独立的实心「面性」版本。
 * 因此未选中用最细的 Light（线性感），选中用最粗的 Demibold（接近面性），
 * 配合选中椭圆一起做线性 / 面性的观感区分。
 */
object NavIcons {
    val HomeLine: ImageVector get() = MiuixIcons.Light.Home
    val HomeActive: ImageVector get() = MiuixIcons.Demibold.Home

    val SettingsLine: ImageVector get() = MiuixIcons.Light.Settings
    val SettingsActive: ImageVector get() = MiuixIcons.Demibold.Settings

    val AboutLine: ImageVector get() = MiuixIcons.Light.Info
    val AboutActive: ImageVector get() = MiuixIcons.Demibold.Info

    val SearchLine: ImageVector get() = MiuixIcons.Regular.Search
    val SearchActive: ImageVector get() = MiuixIcons.Demibold.Search
}
