// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.apanzinc.hypervisage.R
import dev.apanzinc.hypervisage.data.Route
import dev.lackluster.hyperx.navigation.LocalNavigator
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.TextPreference
import dev.lackluster.hyperx.ui.preference.itemPreferenceGroup
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** target 是顶层标签页下标；-1 表示跳到示例功能这个二级页。 */
private data class SearchEntry(val title: String, val target: Int)

private const val TargetExample = -1

/**
 * 底栏右侧独立圆钮打开的搜索页。它不属于三个并行标签页，所以保留返回箭头。
 * 结果定位到标签页时先 pop 自己，再让 MainTabsScreen 滑过去。
 */
@Composable
fun SearchPage(onOpenTab: (Int) -> Unit) {
    val navigator = LocalNavigator.current
    var query by remember { mutableStateOf("") }

    val title = stringResource(R.string.page_search)
    val hint = stringResource(R.string.search_hint)
    val empty = stringResource(R.string.search_empty)
    val groupTitle = stringResource(R.string.search_group)
    val exampleTitle = stringResource(R.string.page_example)
    val moduleTitle = stringResource(R.string.page_module_settings)
    val aboutTitle = stringResource(R.string.page_about)
    val enabledTitle = stringResource(R.string.module_enabled_title)
    val debugTitle = stringResource(R.string.module_debug_title)
    val predictiveBackTitle = stringResource(R.string.module_predictive_back_title)
    val glassBlurTitle = stringResource(R.string.module_glass_blur_title)
    val standardBottomBarTitle = stringResource(R.string.module_standard_bottom_bar_title)
    val themeTitle = stringResource(R.string.module_theme_title)
    val iconTitle = stringResource(R.string.about_icon_title)
    val licenseTitle = stringResource(R.string.about_license_title)

    val entries = remember(
        exampleTitle, moduleTitle, aboutTitle, enabledTitle, debugTitle,
        predictiveBackTitle, glassBlurTitle, standardBottomBarTitle, themeTitle,
        iconTitle, licenseTitle,
    ) {
        listOf(
            SearchEntry("SystemUI", TargetExample),
            SearchEntry(exampleTitle, TargetExample),
            SearchEntry(moduleTitle, TabSettings),
            SearchEntry(enabledTitle, TabSettings),
            SearchEntry(debugTitle, TabSettings),
            SearchEntry(predictiveBackTitle, TabSettings),
            SearchEntry(glassBlurTitle, TabSettings),
            SearchEntry(standardBottomBarTitle, TabSettings),
            SearchEntry(themeTitle, TabSettings),
            SearchEntry(aboutTitle, TabAbout),
            SearchEntry(iconTitle, TabAbout),
            SearchEntry(licenseTitle, TabAbout),
        )
    }
    val results = remember(query, entries) {
        val keyword = query.trim()
        if (keyword.isEmpty()) entries
        else entries.filter { it.title.contains(keyword, ignoreCase = true) }
    }

    HyperXPage(title = title) {
        item {
            SearchInput(
                value = query,
                onValueChange = { query = it },
                hint = hint,
            )
        }

        itemPreferenceGroup(title = groupTitle, position = ItemPosition.Single) {
            if (results.isEmpty()) {
                TextPreference(title = empty, enabled = false)
            } else {
                results.forEach { entry ->
                    TextPreference(
                        title = entry.title,
                        onClick = {
                            if (entry.target == TargetExample) {
                                navigator.push(Route.Example)
                            } else {
                                onOpenTab(entry.target)
                            }
                        },
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun SearchInput(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(color = colors.onBackground, fontSize = 16.sp),
            singleLine = true,
            cursorBrush = SolidColor(colors.onBackground),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        color = colors.onBackground.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                    )
                }
                innerTextField()
            },
        )
    }
}
