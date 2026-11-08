// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.component

import androidx.annotation.DrawableRes
import dev.apanzinc.hypervisage.R

/**
 * 底栏图标：按参考截图里小米官方分离式底栏的造型自绘，全部 24dp viewport。
 * 面性（Face）是当前唯一在用的一套，选中与未选中不换图标。
 * 线性（Line）四个保留备用，不参与绘制，也没有字重 / 线宽混搭。
 */
object NavIcons {
    @DrawableRes val HomeFace = R.drawable.nav_home_fill
    @DrawableRes val SettingsFace = R.drawable.nav_settings_fill
    @DrawableRes val AboutFace = R.drawable.nav_about_fill
    @DrawableRes val SearchFace = R.drawable.nav_search_fill

    @DrawableRes val HomeLine = R.drawable.nav_home_line
    @DrawableRes val SettingsLine = R.drawable.nav_settings_line
    @DrawableRes val AboutLine = R.drawable.nav_about_line
    @DrawableRes val SearchLine = R.drawable.nav_search_line
}
