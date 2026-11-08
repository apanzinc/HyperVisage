// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
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
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
 * 固定贴底的柔光玻璃分离式底栏：胶囊导航 + 独立圆形搜索按钮。
 *
 * 整条底栏不画底色，就是页面背景色；胶囊和圆钮也没有灰色实底或硬描边，
 * 只叠一层极淡的玻璃渐变和多层柔光边（BloomStroke 的轻量纯绘制近似）。
 * 选中项是同一套玻璃、浓度更高的一块，不是拿灰色块当边界。
 * 不引入 backdrop / RuntimeShader，页面转场时不会多跑一遍玻璃合成。
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
private val IndicatorInsetVertical = 3.dp
private val IndicatorInsetHorizontal = 8.dp
private val IndicatorOverhang = 5.dp

/** 按下时图标和文字的灰度，按压反馈不做 ripple。 */
private const val PressedAlpha = 0.42f
/** 选择器按下时高度压缩、宽度外扩的比例。 */
private const val SquishHeight = 0.22f
private const val SquishWidth = 0.10f

@Immutable
data class HyperVisageBottomBarItem(
    /** 选中与未选中都用同一个面性图标，不做线性 / 面性切换 */
    val icon: ImageVector,
    val label: String,
)

@Composable
fun HyperVisageBottomBar(
    items: List<HyperVisageBottomBarItem>,
    selectedIndex: Int,
    /** Pager 的连续进度（currentPage + offsetFraction），高光在 draw 阶段直接读 */
    selectedProgress: State<Float>,
    onSelect: (Int) -> Unit,
    /** 手指在胶囊上横滑，按「页」为单位喂给 Pager，页面和高光同一份进度 */
    onDragPageDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragActiveChange: (Boolean) -> Unit,
    actionIcon: ImageVector,
    actionContentDescription: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val index = selectedIndex.coerceIn(0, items.lastIndex)
    val colors = MiuixTheme.colorScheme
    val isDark = colors.background.luminance() < 0.5f
    val contentColor = colors.onBackground
    // 深色用白光、浅色用暗影，两边都只取很低的不透明度
    val glassTint = if (isDark) Color.White else Color.Black
    val capsuleShape = RoundedCornerShape(percent = 50)

    val navBarBottom = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()

    // 整条底栏没有自己的底色，直接是页面背景色；只留系统导航栏 inset
    Box(
        modifier = modifier
            .fillMaxWidth()
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
                onDragPageDelta = onDragPageDelta,
                onDragEnd = onDragEnd,
                onDragActiveChange = onDragActiveChange,
                glassTint = glassTint,
                contentColor = contentColor,
                shape = capsuleShape,
            )
            RoundAction(
                icon = actionIcon,
                contentDescription = actionContentDescription,
                glassTint = glassTint,
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
    onDragPageDelta: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragActiveChange: (Boolean) -> Unit,
    glassTint: Color,
    contentColor: Color,
    shape: RoundedCornerShape,
) {
    val density = LocalDensity.current
    val overhangPx = with(density) { IndicatorOverhang.toPx() }
    val insetPx = with(density) { IndicatorInsetVertical.toPx() }
    val contentInsetPx = with(density) { IndicatorInsetHorizontal.toPx() }
    val slotPx = with(density) { (width - IndicatorInsetHorizontal * 2).toPx() / items.size }

    val interactionSources = remember(items.size) {
        List(items.size) { MutableInteractionSource() }
    }
    val pressedStates = remember(items.size) {
        List(items.size) { mutableStateOf(false) }
    }
    LaunchedEffect(interactionSources) {
        interactionSources.forEachIndexed { position, source ->
            launch {
                source.interactions.collect { interaction ->
                    pressedStates[position].value = when (interaction) {
                        is PressInteraction.Press -> true
                        is PressInteraction.Release, is PressInteraction.Cancel -> false
                        else -> pressedStates[position].value
                    }
                }
            }
        }
    }
    val anyPressed by remember(pressedStates) {
        derivedStateOf { pressedStates.any { it.value } }
    }
    // 返回的是 State<Float>，在 draw 阶段读，不用解包到组合里
    val squish = animateFloatAsState(
        targetValue = if (anyPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 700f),
        label = "indicatorSquish",
    )

    Box(
        modifier = Modifier
            .width(width)
            .height(BarHeight)
            .clip(shape)
            .drawBehind {
                // 容器：极淡玻璃，几乎就是背景色
                drawSoftGlass(0f, 0f, size.width, size.height, glassTint, 1f)

                // 选择器：同套玻璃、浓度更高，读 Pager 连续进度；按压时压扁并轻微拉宽
                val slot = (size.width - contentInsetPx * 2f) / items.size
                val p = progress.value.coerceIn(0f, items.lastIndex.toFloat())
                val s = squish.value
                val w = (slot + overhangPx * 2f) * (1f + SquishWidth * s)
                val h = (size.height - insetPx * 2f) * (1f - SquishHeight * s)
                if (w <= 0f || h <= 0f) return@drawBehind
                val centerX = contentInsetPx + (p + 0.5f) * slot
                drawSoftGlass(
                    centerX - w / 2f,
                    (size.height - h) / 2f,
                    w,
                    h,
                    glassTint,
                    2.2f,
                )
            }
            .pointerInput(slotPx, onDragPageDelta, onDragEnd, onDragActiveChange) {
                detectHorizontalDragGestures(
                    onDragStart = { onDragActiveChange(true) },
                    onDragEnd = {
                        onDragActiveChange(false)
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragActiveChange(false)
                        onDragEnd()
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        // 一个 slot 宽度对应一整页，方向按真机手感取正
                        onDragPageDelta(dragAmount / slotPx)
                    },
                )
            },
    ) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = IndicatorInsetHorizontal)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { position, item ->
                val pressed by pressedStates[position]
                val tint = if (pressed) contentColor.copy(alpha = PressedAlpha) else contentColor
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = position == index,
                            onClick = { onSelect(position) },
                            role = Role.Tab,
                            interactionSource = interactionSources[position],
                            indication = null,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize),
                            tint = tint,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            color = tint,
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
    glassTint: Color,
    contentColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = rememberPressed(interactionSource)
    val tint = if (pressed.value) contentColor.copy(alpha = PressedAlpha) else contentColor
    Box(
        modifier = Modifier
            .size(ActionSize)
            .clip(CircleShape)
            .drawBehind {
                drawSoftGlass(0f, 0f, size.width, size.height, glassTint, 1f)
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(ActionIconSize),
            tint = tint,
        )
    }
}

/**
 * 柔光玻璃的纯绘制近似：一层上下略有明暗的极淡玻璃渐变，加三层由外到内渐亮的
 * 圆角描边，形成柔光晕而不是一条硬白线，也不垫灰色实底。
 */
private fun DrawScope.drawSoftGlass(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    tint: Color,
    intensity: Float,
) {
    if (width <= 0f || height <= 0f) return
    val corner = CornerRadius(height / 2f)
    val fill = Brush.verticalGradient(
        colors = listOf(
            tint.copy(alpha = 0.034f * intensity * 1.6f),
            tint.copy(alpha = 0.034f * intensity * 0.65f),
            tint.copy(alpha = 0.034f * intensity * 1.15f),
        ),
        startY = top,
        endY = top + height,
    )
    drawRoundRect(
        brush = fill,
        topLeft = Offset(left, top),
        size = Size(width, height),
        cornerRadius = corner,
    )
    listOf(3.5f to 0.030f, 2f to 0.055f, 1f to 0.090f).forEach { (strokeWidth, alpha) ->
        val inset = strokeWidth / 2f + 0.5f
        val w = width - inset * 2f
        val h = height - inset * 2f
        if (w <= 0f || h <= 0f) return@forEach
        drawRoundRect(
            color = tint.copy(alpha = alpha * intensity),
            topLeft = Offset(left + inset, top + inset),
            size = Size(w, h),
            cornerRadius = CornerRadius(h / 2f),
            style = Stroke(width = strokeWidth),
        )
    }
}

@Composable
private fun rememberPressed(interactionSource: MutableInteractionSource): State<Boolean> {
    val pressed = remember { mutableStateOf(false) }
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            pressed.value = when (interaction) {
                is PressInteraction.Press -> true
                is PressInteraction.Release, is PressInteraction.Cancel -> false
                else -> pressed.value
            }
        }
    }
    return pressed
}
