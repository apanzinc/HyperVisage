// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.lackluster.hyperx.ui.layout

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
 *    layerBackdrop + progressiveTextureBlur，每帧要录两份内容纹理、跑两遍 RuntimeShader，
 *    动画全程掉帧。这里把「转场进行中」下发到页面，让顶栏/底栏在转场期间降级成静态底色。
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

    var navBusy by remember { mutableStateOf(false) }
    var gestureBusy by remember { mutableStateOf(false) }
    val isTransitioning = navBusy || gestureBusy

    LaunchedEffect(Unit) {
        snapshotFlow { backStack.size to backStack.lastOrNull() }
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
        snapshotFlow { gestureState.transitionState }
            .collectLatest { state ->
                if (state is InProgress) {
                    gestureBusy = true
                } else {
                    // 松手后的 pop 动画同样要降级
                    delay(TransitionSettleDelayMs)
                    gestureBusy = false
                }
            }
    }

    CompositionLocalProvider(LocalPageTransitionRunning provides isTransitioning) {
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
