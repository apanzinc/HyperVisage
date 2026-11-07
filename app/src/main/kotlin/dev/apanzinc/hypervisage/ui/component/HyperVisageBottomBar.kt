// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * HyperOS 4 风格的分离式悬浮底栏：一个胶囊导航 + 一个独立圆形搜索按钮。
 * 胶囊里放三个并行标签页（主页 / 设置 / 关于），圆钮是打开搜索页的动作入口。
 *
 * 结构和尺寸参照 compose-miuix-ui/miuix main 分支的 `GlassNavigationBar`
 * （GlassNavigationBarDefaults / GlassNavigationBarLayout），但只用 0.9.4 已有的
 * 纯绘制 API 复刻，不引入 backdrop 与 RuntimeShader，避免页面转场时再跑一遍玻璃合成。
 */
private val BarHeight = 56.dp
private val CapsuleWidth = 208.dp
/** 三个标签时每项仍是 88dp，和两项的 104dp 视觉密度接近。 */
private val CapsuleWidthWide = 264.dp
private val ActionSize = 56.dp
private val ActionSpacing = 12.dp
private val IconSize = 26.dp
private val ActionIconSize = 26.dp
private val LabelSize = 11.sp
private val StrokeWidth = 0.75.dp
private val IndicatorInsetVertical = 3.dp
private val IndicatorInsetHorizontal = 8.dp
private val IndicatorOverhang = 5.dp

@Immutable
data class HyperVisageBottomBarItem(
    val icon: ImageVector,
    val label: String,
)

@Composable
fun HyperVisageBottomBar(
    items: List<HyperVisageBottomBarItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    actionIcon: ImageVector,
    actionContentDescription: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val index = selectedIndex.coerceIn(0, items.lastIndex)
    val colors = MiuixTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    val containerColor = colors.surfaceContainer
    val strokeColor = colors.dividerLine
    val contentColor = colors.onBackground
    // 与上游 GlassNavigationBarDefaults.indicatorColor() 一致
    val indicatorColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.06f)
    }
    val capsuleShape = RoundedCornerShape(percent = 50)

    val navBarBottom = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()
    val bottomPadding = if (navBarBottom == 0.dp) 32.dp else 16.dp + navBarBottom

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ActionSpacing),
        ) {
            NavigationCapsule(
                width = if (items.size >= 3) CapsuleWidthWide else CapsuleWidth,
                items = items,
                index = index,
                onSelect = onSelect,
                containerColor = containerColor,
                strokeColor = strokeColor,
                contentColor = contentColor,
                indicatorColor = indicatorColor,
                shape = capsuleShape,
            )
            RoundAction(
                icon = actionIcon,
                contentDescription = actionContentDescription,
                containerColor = containerColor,
                strokeColor = strokeColor,
                contentColor = contentColor,
                onClick = onActionClick,
            )
        }
    }
}

@Composable
private fun NavigationCapsule(
    width: Dp,
    items: List<HyperVisageBottomBarItem>,
    index: Int,
    onSelect: (Int) -> Unit,
    containerColor: Color,
    strokeColor: Color,
    contentColor: Color,
    indicatorColor: Color,
    shape: RoundedCornerShape,
) {
    val density = LocalDensity.current
    val overhangPx = with(density) { IndicatorOverhang.toPx() }
    val insetPx = with(density) { IndicatorInsetVertical.toPx() }
    val contentInsetPx = with(density) { IndicatorInsetHorizontal.toPx() }

    var barWidth by remember { mutableIntStateOf(0) }
    // 两条边各自用弹簧驱动，切换时高光会先被拉开再收拢，和上游的独立边缘动画一致
    val left = remember { Animatable(0f) }
    val right = remember { Animatable(0f) }

    LaunchedEffect(index, barWidth, items.size) {
        if (barWidth <= 0) return@LaunchedEffect
        val slot = (barWidth - contentInsetPx * 2f) / items.size
        val targetLeft = contentInsetPx + index * slot - overhangPx
        val targetRight = targetLeft + slot + overhangPx * 2f
        if (right.value <= left.value) {
            left.snapTo(targetLeft)
            right.snapTo(targetRight)
        } else {
            launch { left.animateTo(targetLeft, spring(dampingRatio = 0.8f, stiffness = 420f)) }
            launch { right.animateTo(targetRight, spring(dampingRatio = 0.8f, stiffness = 300f)) }
        }
    }

    Box(
        modifier = Modifier
            .width(width)
            .height(BarHeight)
            .background(containerColor, shape)
            .border(StrokeWidth, strokeColor, shape)
            .clip(shape)
            .onSizeChanged { barWidth = it.width },
    ) {
        // 选中态高光。读 Animatable.value 只发生在 draw 阶段，切换不会重组整棵底栏
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val l = left.value
                    val r = right.value
                    val h = size.height - insetPx * 2f
                    if (r <= l || h <= 0f) return@drawBehind
                    drawRoundRect(
                        color = indicatorColor,
                        topLeft = Offset(l, insetPx),
                        size = Size(r - l, h),
                        cornerRadius = CornerRadius(h / 2f),
                    )
                }
        )

        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = IndicatorInsetHorizontal)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { position, item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = position == index,
                            onClick = { onSelect(position) },
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize),
                            tint = contentColor,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            color = contentColor,
                            fontSize = LabelSize,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoundAction(
    icon: ImageVector,
    contentDescription: String,
    containerColor: Color,
    strokeColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(ActionSize)
            .background(containerColor, CircleShape)
            .border(StrokeWidth, strokeColor, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(ActionIconSize),
            tint = contentColor,
        )
    }
}
