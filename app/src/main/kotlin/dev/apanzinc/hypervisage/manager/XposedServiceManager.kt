// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.manager

import dev.apanzinc.hypervisage.hook.utils.MLog
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 与 LSPosed 的桥接。只有绑定成功，模块才能读写 remote preferences、主动结束进程等。
 */
class XposedServiceManager : XposedServiceHelper.OnServiceListener {

    private val _service = MutableStateFlow<XposedService?>(null)
    val serviceFlow = _service.asStateFlow()

    val currentService: XposedService?
        get() = _service.value

    init {
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        MLog.d { "XposedService bound" }
        _service.value = service
    }

    override fun onServiceDied(service: XposedService) {
        MLog.d { "XposedService died" }
        _service.value = null
    }
}
