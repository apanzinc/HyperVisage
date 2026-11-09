// SPDX-License-Identifier: GPL-3.0-only
// Copyright (C) 2026 apanzinc

package dev.apanzinc.hypervisage.ui.component

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 固定贴底的分离式底栏：胶囊导航 + 独立圆形搜索按钮。
 *
 * 底层材质由窗口级 [HyperVisageBottomBarOverlay] 的翻转渐进模糊提供，
 * 按钮自己再叠一层更强的背景模糊（textureBlur）。开着「界面模糊」时
 * 表面只有模糊：不叠 blend 色、不画实底、不画描边、不画柔光渐变，
 * 深浅主题走同一条路径，深色下就是纯黑色调的模糊。
 *
 * 关掉模糊时没有纹理可采，按钮才退回不透明实底。阴影只落在按钮上。
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
/** 按钮的轻微扩散阴影，材质层不加 */
private val ButtonShadow = 3.dp

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
    // 返回的是 State<Float>，在 draw 阶段读，不用解包到组合里
    val squish = animateFloatAsState(
        targetValue = if (anyPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 700f),
        label = "indicatorSquish",
    )
    // 关掉界面模糊时没有窗口级纹理，按钮退回不透明实底，避免半透明表面悬空
    val fallbackSurface = if (isLight) Color.White else MiuixTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .width(width)
            .height(BarHeight)
            .shadow(
                elevation = ButtonShadow,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.10f),
            )
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
                // 表面只有模糊，这里不画底 / 描边 / 渐变，只留必要的选中指示：
                // 一层极淡的内容色，跟 Pager 连续进度走，按压时压扁并轻微拉宽
                val slot = (size.width - contentInsetPx * 2f) / items.size
                val p = progress.value.coerceIn(0f, items.lastIndex.toFloat())
                val s = squish.value
                val w = (slot + overhangPx * 2f) * (1f + SquishWidth * s)
                val h = (size.height - insetPx * 2f) * (1f - SquishHeight * s)
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
            .shadow(
                elevation = ButtonShadow,
                shape = CircleShape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.10f),
            )
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
