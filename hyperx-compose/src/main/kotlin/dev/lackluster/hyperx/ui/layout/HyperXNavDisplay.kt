// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.lackluster.hyperx.ui.layout

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.runtime.Composable
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
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState

/**
 * 在 miuix fork 的 NavDisplay 之上加一个预测返回开关。
 *
 * fork 的 NavDisplay(backStack = ...) 重载内部会无条件注册 NavigationBackHandler，
 * 并把同一个手势 state 交给页面做跟手动画，外部没法关掉。这里照它的
 * NavDisplay(entries = ...) 重载拆开一层，改用第三个重载（sceneState + navigationEventState），
 * 关闭开关时给页面一个永远停在 Idle 的 state：手势照常被应用消费，
 * 但页面不跟手，松手后走普通 popTransitionSpec 返回。
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
