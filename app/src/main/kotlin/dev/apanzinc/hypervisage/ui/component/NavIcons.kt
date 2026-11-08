// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.component

import androidx.annotation.DrawableRes
import dev.apanzinc.hypervisage.R

/**
 * 底栏图标：按参考截图里小米官方分离式底栏的造型自绘的成套线性 / 面性两版。
 * 全部 24dp viewport、2dp 线宽，四个图标同一套几何语言，见 `res/drawable/nav_*.xml`。
 *
 * 不再用 miuix misymbol 的字重差：它只有粗细五档，没有真正的实心版，
 * 混着用就是「粗细不一样、线面也不一样」。也不再用 Material Icons。
 */
object NavIcons {
    @DrawableRes val HomeLine = R.drawable.nav_home_line
    @DrawableRes val HomeActive = R.drawable.nav_home_fill

    @DrawableRes val SettingsLine = R.drawable.nav_settings_line
    @DrawableRes val SettingsActive = R.drawable.nav_settings_fill

    @DrawableRes val AboutLine = R.drawable.nav_about_line
    @DrawableRes val AboutActive = R.drawable.nav_about_fill

    /** 搜索是独立圆钮上的动作，不参与选中，只要线性版。 */
    @DrawableRes val SearchLine = R.drawable.nav_search_line
}
