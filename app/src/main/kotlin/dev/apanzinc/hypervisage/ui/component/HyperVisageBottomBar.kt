// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.component

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
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * HyperOS 4 风格的固定贴底分离式底栏：一个胶囊导航 + 一个独立圆形搜索按钮。
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
    /** 未选中：线性图标 */
    val icon: ImageVector,
    /** 选中：面性图标 */
    val selectedIcon: ImageVector,
    val label: String,
)

@Composable
fun HyperVisageBottomBar(
    items: List<HyperVisageBottomBarItem>,
    selectedIndex: Int,
    /** Pager 的连续进度（currentPage + offsetFraction），高光在 draw 阶段直接读，和页面严格同步 */
    selectedProgress: State<Float>,
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

    // 底栏是固定贴底的一块，不是浮在页面上的浮层：给不透明底色，
    // 只留系统导航栏 inset 和很小的上下呼吸，不再留 32dp 悬浮留白。
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(top = 12.dp, bottom = navBarBottom + 12.dp),
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
                progress = selectedProgress,
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
    progress: State<Float>,
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

    Box(
        modifier = Modifier
            .width(width)
            .height(BarHeight)
            .background(containerColor, shape)
            .border(StrokeWidth, strokeColor, shape)
            .clip(shape),
    ) {
        // 高光不再跑自己的弹簧：直接用 Pager 的连续进度算位置，
        // 手指拖到哪高光就到哪，点击时 Pager 自己的弹簧动画也会 1:1 带过来。
        // 只在 draw 阶段读 State.value，滑动时零重组。
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val slot = (size.width - contentInsetPx * 2f) / items.size
                    val p = progress.value.coerceIn(0f, items.lastIndex.toFloat())
                    val l = contentInsetPx + p * slot - overhangPx
                    val r = l + slot + overhangPx * 2f
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
                        // 线性 / 面性两套图标按 Pager 进度交叉淡入，读值和算位置都在
                        // graphicsLayer 的延迟 lambda 里，滑动时不会触发重组，
                        // 手指拖到哪图标就渐变到哪，和高光严格同步。
                        Box(modifier = Modifier.size(IconSize)) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        alpha = 1f - selectionWeight(progress.value, position)
                                    },
                                tint = contentColor,
                            )
                            Icon(
                                imageVector = item.selectedIcon,
                                contentDescription = null,
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        alpha = selectionWeight(progress.value, position)
                                    },
                                tint = contentColor,
                            )
                        }
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

/** 进度正好落在某一项上时权重为 1，滑到相邻页归 0，用于图标交叉淡入。 */
private fun selectionWeight(progress: Float, position: Int): Float =
    (1f - kotlin.math.abs(progress - position.toFloat())).coerceIn(0f, 1f)
