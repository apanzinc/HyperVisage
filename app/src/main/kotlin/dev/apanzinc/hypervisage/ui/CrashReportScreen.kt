// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 上次崩溃的堆栈。只在检测到日志文件时出现，复制完即可关闭并删除日志。
 */
@Composable
fun CrashReportScreen(
    report: String,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val onSurface = MiuixTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        BasicText(
            text = "上次崩溃日志",
            style = TextStyle(color = onSurface, fontSize = 22.sp),
        )
        Spacer(Modifier.height(8.dp))
        BasicText(
            text = "复制发给开发者后即可关闭，关闭会删除这份日志",
            style = TextStyle(color = onSurface.copy(alpha = 0.7f), fontSize = 13.sp),
        )
        Spacer(Modifier.height(16.dp))
        BasicText(
            text = report,
            style = TextStyle(
                color = onSurface,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 15.sp,
            ),
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                modifier = Modifier.weight(1f),
                text = "复制日志",
                minHeight = 50.dp,
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as? ClipboardManager
                    clipboard?.setPrimaryClip(
                        ClipData.newPlainText("HyperVisage crash", report)
                    )
                },
            )
            TextButton(
                modifier = Modifier.weight(1f),
                text = "关闭",
                minHeight = 50.dp,
                onClick = onClose,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
