// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.apanzinc.hypervisage.BuildConfig
import dev.apanzinc.hypervisage.R
import dev.lackluster.hyperx.ui.layout.HyperXPage
import dev.lackluster.hyperx.ui.preference.ItemPosition
import dev.lackluster.hyperx.ui.preference.TextPreference
import dev.lackluster.hyperx.ui.preference.itemPreferenceGroup

private const val MINGCUTE_URL = "https://www.mingcute.com/"
private const val APACHE_URL = "https://www.apache.org/licenses/LICENSE-2.0"

@Composable
fun AboutPage() {
    val uriHandler = LocalUriHandler.current

    val title = stringResource(R.string.page_about)
    val appName = stringResource(R.string.app_name)
    val appDesc = stringResource(R.string.xposed_desc)
    val buildTimeTitle = stringResource(R.string.about_build_time)
    val creditsTitle = stringResource(R.string.about_group_credits)
    val iconTitle = stringResource(R.string.about_icon_title)
    val iconSummary = stringResource(R.string.about_icon_summary)
    val licenseTitle = stringResource(R.string.about_license_title)
    val licenseSummary = stringResource(R.string.about_license_summary)
    val projectLicense = stringResource(R.string.about_project_license)

    HyperXPage(title = title, navigationIcon = {}) {
        itemPreferenceGroup(position = ItemPosition.Single) {
            TextPreference(
                title = appName,
                summary = appDesc,
                value = "v${BuildConfig.VERSION_NAME}",
                enabled = false,
            )
            TextPreference(
                title = buildTimeTitle,
                value = BuildConfig.BUILD_TIME,
                enabled = false,
            )
        }

        itemPreferenceGroup(
            title = creditsTitle,
            position = ItemPosition.Middle,
        ) {
            TextPreference(
                title = iconTitle,
                summary = iconSummary,
                onClick = { runCatching { uriHandler.openUri(MINGCUTE_URL) } },
            )
            TextPreference(
                title = licenseTitle,
                summary = licenseSummary,
                value = "Apache-2.0",
                onClick = { runCatching { uriHandler.openUri(APACHE_URL) } },
            )
        }

        itemPreferenceGroup(position = ItemPosition.Last) {
            TextPreference(
                title = projectLicense,
                value = "GPL-3.0",
                enabled = false,
            )
        }

        // 底栏是压在内容上的模糊覆盖层，留出让最后一项滚出遮挡的空间
        item { Spacer(modifier = Modifier.height(112.dp)) }
    }
}
