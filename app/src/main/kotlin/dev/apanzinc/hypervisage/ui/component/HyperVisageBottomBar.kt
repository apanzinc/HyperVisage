// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import kotlin.math.abs
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 固定贴底的分离式底栏：胶囊导航 + 独立圆形搜索按钮。
 *
 * 底层材质由窗口级 [HyperVisageBottomBarOverlay] 的翻转渐进模糊提供，
 * 按钮自己再叠一层更强的背景模糊（textureBlur）。开着「界面模糊」时
 * 表面只有模糊：不叠 blend 色、不画实底、不画柔光渐变，
 * 深浅主题走同一条路径，深色下就是纯黑色调的模糊。
 * 纯模糊没有轮廓，所以深色模式下胶囊和圆钮各描一圈很细很淡的中性灰边；
 * 浅色模式不描边。底层材质层在任何主题下都不加边，边界仍然只靠模糊渐变。
 *
 * 关掉模糊时没有纹理可采，按钮才退回不透明实底，深色灰边照常保留。
 * 阴影不用 `Modifier.shadow`（它固定带一个向下的 spot 偏移），改成从按钮轮廓
 * 四周同心外扩的多层淡描边：扩散 18dp、峰值只有 3% 黑、向外二次衰减，
 * 范围大但不深，材质层背后不加阴影。
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
/** 从按钮轮廓向四周均匀散开的柔光阴影：范围大、浓度低、没有向下偏移。 */
private val GlowRadius = 18.dp
private const val GlowSteps = 18
private const val GlowPeakAlpha = 0.030f
/** 只在深色模式画：细而淡的中性灰，浅色模式完全不描；底层材质层不加边 */
private val BarBorderWidth = 0.5.dp
private val BarBorderColor = Color(0xFF949494).copy(alpha = 0.40f)

/** 按下时图标和文字的灰度，按压反馈不做 ripple。 */
private const val PressedAlpha = 0.42f
/** 按下时选择器整体等比缩小，拖动全程保持，松手才回弹。 */
private const val PressScale = 0.88f
/** 拖动速度带来的轻微拉长：满速时宽度最多多出 12%，高度相应收一点。 */
private const val StretchMax = 0.12f
private const val StretchHeightLoss = 0.25f
private const val VelocityFullStretch = 1.8f // px per ms
private const val VelocitySmoothing = 0.35f
/** 甩出去时水平轻微压扁，再弹回原形。 */
private const val FlingVelocityThreshold = 0.55f // px per ms
private const val FlingWidthLoss = 0.10f
private const val FlingHeightGain = 0.08f

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
    /** 窗口级背景纹理；非 null 时按钮自己也做背景模糊 */
    backdrop: LayerBackdrop?,
    buttonBlurRadius: Float,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val index = selectedIndex.coerceIn(0, items.lastIndex)
    val colors = MiuixTheme.colorScheme
    // 和窗口级覆盖层用同一套相对亮度判断，避免两处深浅判定不一致
    val isLight = colors.background.luminance() > colors.onBackground.luminance()
    // 浅色模式内容固定黑色，避免图标糊在白底里；深色沿用主题的浅色前景
    val contentColor = if (isLight) Color.Black else colors.onBackground
    val capsuleShape = RoundedCornerShape(percent = 50)

    val navBarBottom = WindowInsets.navigationBars
        .only(WindowInsetsSides.Bottom)
        .asPaddingValues()
        .calculateBottomPadding()

    // 整条底栏自身不画底色，底层是窗口级覆盖层的翻转渐进模糊材质
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
                contentColor = contentColor,
                isLight = isLight,
                shape = capsuleShape,
                backdrop = backdrop,
                blurRadius = buttonBlurRadius,
            )
            RoundAction(
                icon = actionIcon,
                contentDescription = actionContentDescription,
                contentColor = contentColor,
                isLight = isLight,
                onClick = onActionClick,
                backdrop = backdrop,
                blurRadius = buttonBlurRadius,
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
    contentColor: Color,
    isLight: Boolean,
    shape: RoundedCornerShape,
    backdrop: LayerBackdrop?,
    blurRadius: Float,
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
    // 拖动手势可能把 PressInteraction 提前 cancel，所以缩小状态同时由 dragActive 兜住：
    // 手指没离开就一直缩小，只有松手才回弹；位置仍单独读 Pager 进度，拖动照常跟手。
    val dragActive = remember { mutableStateOf(false) }
    val holdShrink = anyPressed || dragActive.value
    val settlePage = onDragEnd

    // 三个形变量都用 Animatable，只在 draw 阶段读 .value：
    // 拖动每帧只改动画值，不重组这棵底栏，也不影响模糊层。
    val pressScale = remember { Animatable(1f) }
    val stretch = remember { Animatable(0f) }
    val flingSquish = remember { Animatable(0f) }
    val animationScope = rememberCoroutineScope()
    LaunchedEffect(holdShrink) {
        pressScale.animateTo(
            targetValue = if (holdShrink) PressScale else 1f,
            animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
        )
    }
    // 关掉界面模糊时没有窗口级纹理，按钮退回不透明实底，避免半透明表面悬空
    val fallbackSurface = if (isLight) Color.White else MiuixTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .width(width)
            .height(BarHeight)
            // 必须在 clip 之前画，扩散才不会被裁掉
            .drawBehind { drawSoftGlow() }
            .clip(shape)
            .then(
                if (backdrop == null) {
                    Modifier.background(fallbackSurface, shape)
                } else {
                    // 按钮自己再叠一层比底层更强的背景模糊，两种主题都生效
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = shape,
                        blurRadius = blurRadius,
                        noiseCoefficient = 0f,
                    )
                }
            )
            .drawBehind {
                // 灰边只走深色模式，浅色直接跳过
                drawBarBorder(isLight)

                // 选中指示：位置只由 Pager 连续进度决定，形变一律以中心为锚，
                // 所以拖动照常跟手，缩小 / 拉长 / 甩动回弹都不会影响切页。
                val slot = (size.width - contentInsetPx * 2f) / items.size
                val p = progress.value.coerceIn(0f, items.lastIndex.toFloat())
                val scale = pressScale.value
                val lengthening = stretch.value
                val fling = flingSquish.value
                val w = (slot + overhangPx * 2f) *
                    scale * (1f + lengthening) * (1f - FlingWidthLoss * fling)
                val h = (size.height - insetPx * 2f) *
                    scale * (1f - StretchHeightLoss * lengthening) * (1f + FlingHeightGain * fling)
                if (w <= 0f || h <= 0f) return@drawBehind
                val centerX = contentInsetPx + (p + 0.5f) * slot
                drawRoundRect(
                    color = contentColor.copy(alpha = 0.13f),
                    topLeft = Offset(centerX - w / 2f, (size.height - h) / 2f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(h / 2f),
                )
            }
            .pointerInput(slotPx, onDragPageDelta, onDragEnd, onDragActiveChange) {
                // 速度只在手势内部累计，不写 Compose State，避免拖动每帧重组
                var lastUptime = -1L
                var smoothedVelocity = 0f
                fun finishDrag(allowFling: Boolean) {
                    val releasedVelocity = smoothedVelocity
                    smoothedVelocity = 0f
                    lastUptime = -1L
                    dragActive.value = false
                    onDragActiveChange(false)
                    animationScope.launch {
                        stretch.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = 420f),
                        )
                    }
                    // 甩出去：瞬间水平压扁一点，再用欠阻尼弹簧弹回原形
                    if (allowFling && abs(releasedVelocity) >= FlingVelocityThreshold) {
                        animationScope.launch {
                            flingSquish.snapTo(1f)
                            flingSquish.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(dampingRatio = 0.5f, stiffness = 260f),
                            )
                        }
                    }
                    settlePage()
                }
                detectHorizontalDragGestures(
                    onDragStart = {
                        dragActive.value = true
                        onDragActiveChange(true)
                        lastUptime = -1L
                        smoothedVelocity = 0f
                    },
                    onDragEnd = { finishDrag(true) },
                    onDragCancel = { finishDrag(false) },
                    onHorizontalDrag = { change, dragAmount ->
                        val now = change.uptimeMillis
                        val dt = if (lastUptime > 0L) {
                            (now - lastUptime).coerceAtLeast(1L).toFloat()
                        } else {
                            16f
                        }
                        val instant = dragAmount / dt
                        smoothedVelocity = smoothedVelocity * (1f - VelocitySmoothing) +
                            instant * VelocitySmoothing
                        lastUptime = now
                        // 速度越快拉得越长，慢速拖动几乎不形变
                        val target = (abs(smoothedVelocity) / VelocityFullStretch)
                            .coerceIn(0f, 1f) * StretchMax
                        animationScope.launch { stretch.snapTo(target) }
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
    contentColor: Color,
    isLight: Boolean,
    onClick: () -> Unit,
    backdrop: LayerBackdrop?,
    blurRadius: Float,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = rememberPressed(interactionSource)
    val tint = if (pressed.value) contentColor.copy(alpha = PressedAlpha) else contentColor
    val fallbackSurface = if (isLight) Color.White else MiuixTheme.colorScheme.surface
    Box(
        modifier = Modifier
            .size(ActionSize)
            .drawBehind { drawSoftGlow() }
            .clip(CircleShape)
            .then(
                if (backdrop == null) {
                    Modifier.background(fallbackSurface, CircleShape)
                } else {
                    Modifier.textureBlur(
                        backdrop = backdrop,
                        shape = CircleShape,
                        blurRadius = blurRadius,
                        noiseCoefficient = 0f,
                    )
                }
            )
            .drawBehind {
                // 圆钮与胶囊同一条灰边，浅色模式同样不描
                drawBarBorder(isLight)
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
 * 从轮廓四周同心外扩的多层淡描边。每层宽度正好衔接，浓度按 (1-t)² 向外衰减，
 * 紧贴按钮框的一圈最明显、越往外越淡；没有向下偏移，也不会压出一块深色。
 */
private fun DrawScope.drawSoftGlow() {
    val spread = GlowRadius.toPx()
    val step = spread / GlowSteps
    val baseCorner = size.height / 2f
    for (i in GlowSteps - 1 downTo 0) {
        val t = i.toFloat() / (GlowSteps - 1)
        val expand = step * i
        val falloff = 1f - t
        drawRoundRect(
            color = Color.Black.copy(alpha = GlowPeakAlpha * falloff * falloff),
            topLeft = Offset(-expand, -expand),
            size = Size(size.width + expand * 2f, size.height + expand * 2f),
            cornerRadius = CornerRadius(baseCorner + expand),
            style = Stroke(width = step * 1.08f),
        )
    }
}

/** 深色模式才画：0.5dp 的中性灰细边，勾出没有轮廓的纯模糊按钮。 */
private fun DrawScope.drawBarBorder(isLight: Boolean) {
    if (isLight) return
    val strokePx = BarBorderWidth.toPx()
    val inset = strokePx / 2f
    drawRoundRect(
        color = BarBorderColor,
        topLeft = Offset(inset, inset),
        size = Size(size.width - strokePx, size.height - strokePx),
        cornerRadius = CornerRadius(size.height / 2f - inset),
        style = Stroke(width = strokePx),
    )
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
