// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.debug

import android.content.Context
import android.os.Build
import android.os.Process
import java.io.File
import kotlin.system.exitProcess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 不能在设备上跑 ADB 时用的崩溃记录器。
 *
 * 未捕获异常会先写进应用私有目录，再交给系统默认处理器；下次启动由
 * [dev.apanzinc.hypervisage.ui.CrashReportScreen] 显示出来，方便直接复制堆栈。
 */
object CrashRecorder {
    private const val FILE_NAME = "last_crash.txt"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { write(app, thread, error) }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    fun read(context: Context): String? = runCatching {
        val file = File(context.filesDir, FILE_NAME)
        if (file.exists()) file.readText().takeIf { it.isNotBlank() } else null
    }.getOrNull()

    fun clear(context: Context) {
        runCatching { File(context.filesDir, FILE_NAME).delete() }
    }

    private fun write(context: Context, thread: Thread, error: Throwable) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val text = buildString {
            append("time: ").append(stamp).appendLine()
            append("thread: ").append(thread.name).appendLine()
            append("device: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
                .append(" / Android ").append(Build.VERSION.RELEASE)
                .append(" (SDK ").append(Build.VERSION.SDK_INT).append(')').appendLine()
            appendLine()
            append(error.stackTraceToString())
            var cause = error.cause
            while (cause != null) {
                appendLine()
                append("Caused by: ").append(cause.stackTraceToString())
                cause = cause.cause
            }
        }
        File(context.filesDir, FILE_NAME).writeText(text)
    }
}
