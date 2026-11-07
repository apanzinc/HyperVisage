// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.lackluster.hyperx.ui.layout

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneInfo
import androidx.navigation3.scene.SinglePaneSceneStrategy
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.NavDisplayTransitionEffects
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventTransitionState.InProgress
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/** 转场动画 500ms，多留 100ms 等彻底 settle 再恢复重型模糊。 */
private const val TransitionSettleDelayMs = 600L

/**
 * 导航层，按 miuix fork 的 `NavDisplay(entries = ...)` 重载 1:1 组装后，只做两件事：
 *
 * 1. 预测返回开关。fork 的第一/第二重载内部会把同一个 gestureState 无条件交给
 *    handler 和页面，外部关不掉。这里改用第三重载 `NavDisplay(sceneState, navigationEventState)`，
 *    关闭时给页面一个不注册 handler、恒 Idle 的 state：系统手势仍由应用消费，
 *    但页面不跟手，松手后走普通 popTransitionSpec。
 * 2. 转场状态机。AnimatedContent 转场时会同时合成两个场景，如果每个场景都保留
 *    layerBackdrop + progressiveTextureBlur，每帧要录两份内容纹理、跑两遍 RuntimeShader。
 *    这里把「转场进行中」以 [State] 引用下发给页面，页面只用库自带的 `enabled` 开关
 *    跳过 shader，不拆装绘制节点，也不会让整棵导航树跟着重组。
 *    导航栈变化覆盖 push / pop，手势 InProgress 覆盖跟手阶段。
 */
@Composable
fun <T : Any> HyperXNavDisplay(
    backStack: List<T>,
    onBack: () -> Unit,
    entryProvider: (key: T) -> NavEntry<T>,
    predictiveBackEnabled: Boolean,
    transitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform,
    popTransitionSpec: AnimatedContentTransitionScope<Scene<T>>.() -> ContentTransform,
    predictivePopTransitionSpec:
        AnimatedContentTransitionScope<Scene<T>>.(@NavigationEvent.SwipeEdge Int) -> ContentTransform,
    modifier: Modifier = Modifier,
    transitionEffects: NavDisplayTransitionEffects = NavDisplayTransitionEffects.Default,
) {
    val entries = rememberDecoratedNavEntries(
        backStack = backStack,
        entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
        entryProvider = entryProvider,
    )

    val sceneState = rememberSceneState(
        entries = entries,
        sceneStrategies = listOf(SinglePaneSceneStrategy()),
        onBack = onBack,
    )
    val scene = sceneState.currentScene
    val currentInfo = SceneInfo(scene)
    val previousSceneInfos = sceneState.previousScenes.map { SceneInfo(it) }

    // 系统返回手势始终由应用消费，否则系统会自己播一套窗口级返回动画。
    val gestureState = rememberNavigationEventState(
        currentInfo = currentInfo,
        backInfo = previousSceneInfos,
    )
    // 不注册给任何 handler，transitionState 会一直停在 Idle。
    val idleState = rememberNavigationEventState(currentInfo = currentInfo)

    NavigationBackHandler(
        state = gestureState,
        isBackEnabled = scene.previousEntries.isNotEmpty(),
        onBackCompleted = {
            repeat(entries.size - scene.previousEntries.size) { onBack() }
        },
    )

    // 这两个 flag 只在 LaunchedEffect 里读写，组合主体不订阅它们。
    // 否则 navBusy 一变就会重组整个 HyperXNavDisplay，连带重建 SceneInfo、
    // gestureState 和 NavDisplay 的参数，正好卡在动画开始/结束的帧上。
    var navBusy by remember { mutableStateOf(false) }
    var gestureBusy by remember { mutableStateOf(false) }
    val transitionRunning: State<Boolean> = remember {
        derivedStateOf { navBusy || gestureBusy }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { backStack.size to backStack.lastOrNull() }
            .distinctUntilChanged()
            .drop(1)
            .collectLatest {
                navBusy = true
                delay(TransitionSettleDelayMs)
                navBusy = false
            }
    }

    LaunchedEffect(predictiveBackEnabled) {
        if (!predictiveBackEnabled) {
            gestureBusy = false
            return@LaunchedEffect
        }
        // 跟手时 progress 每帧都在变，只关心是否处于 InProgress，
        // 否则 collectLatest 每帧都取消重启一次 delay。
        snapshotFlow { gestureState.transitionState }
            .distinctUntilChanged { old, new -> (old is InProgress) == (new is InProgress) }
            .collectLatest { state -> gestureBusy = state is InProgress }
    }

    CompositionLocalProvider(LocalPageTransitionRunning provides transitionRunning) {
        NavDisplay(
            sceneState = sceneState,
            navigationEventState = if (predictiveBackEnabled) gestureState else idleState,
            modifier = modifier,
            transitionSpec = transitionSpec,
            popTransitionSpec = popTransitionSpec,
            predictivePopTransitionSpec = predictivePopTransitionSpec,
            transitionEffects = transitionEffects,
        )
    }
}
