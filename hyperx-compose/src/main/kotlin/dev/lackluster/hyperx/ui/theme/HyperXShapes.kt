package dev.lackluster.hyperx.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 统一圆角形状。
 *
 * miuix 0.9.4 移除了 theme 包下的 `miuixShape`，圆角改由 miuix-squircle 的 Modifier 扩展
 * （`squircleClip` / `squircleSurface` / `squircleBorder`）提供，不再返回 Shape。
 * 这里先用 [RoundedCornerShape] 顶上：小圆角下与 squircle 几乎无差别，只有接近半圆时
 * 连续性略差。若要换成真正的 squircle，只改这一个函数即可。
 */
fun hyperxShape(radius: Dp = 16.dp): Shape = RoundedCornerShape(radius)
