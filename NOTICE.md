# 第三方组件与素材

本项目为 GPL-3.0-only，但包含以下第三方成果，它们各自保留原许可证。

## hyperx-compose（已修改）

- 位置：`hyperx-compose/`
- 来源：https://github.com/HowieHChen/hyperx-compose
- 许可：Apache License 2.0
- 说明：HyperOS 风格的 Compose 组件库（布局、偏好项、导航）。以源码形式引入，**已作如下修改**：

  | 文件 | 改动 |
  |---|---|
  | `build.gradle.kts` | miuix 依赖 0.9.0 → 0.9.4；移除已停更的 `miuix-navigation3-ui`，改用官方 `androidx.navigation3:navigation3-ui` |
  | `ui/effect/GlassEffect.kt` | 新增：柔光玻璃边缘高光 + 随设备倾斜的光源 |
  | `ui/layout/HyperXScaffold.kt` | 顶栏/底栏模糊层接入高光 |
  | `ui/layout/LayoutLocals.kt` | 新增 `isGlassHighlightEnabled` 开关 |
  | `ui/effect/BlurEffect.kt` | 适配 miuix 0.9.4 移除的 `isRenderEffectSupported` |
  | `ui/theme/HyperXTheme.kt` | 适配 miuix 0.9.4 移除的 `smoothRounding` |
  | `ui/theme/HyperXShapes.kt` | 新增：`hyperxShape` 替代 miuix 0.9.4 移除的 `miuixShape` |
  | `ui/component/AdaptiveIcon.kt`、`ui/component/Card.kt`、`ui/layout/TabRow.kt` | 改用 `hyperxShape` |
  | `ui/animation/HyperXNavTransitions.kt`、`ui/layout/HyperXAppLayout.kt` | 移除 `NavDisplayTransitionEffects`（该 API 来自 miuix 的 navigation3 fork，0.9.4 起不再发布） |

  版权与原许可声明保留于原作者。依据 Apache-2.0 第 4 条，此处明确标注文件已被修改。

## miuix-kotlin-multiplatform

- 版本：0.9.4
- 来源：https://github.com/miuix-kotlin-multiplatform/miuix
- 许可：Apache License 2.0
- 说明：通过 Maven 依赖引入，未修改。提供基础组件、主题与 blur / highlight 管线。

## androidx.navigation3

- 版本：1.1.3
- 来源：https://developer.android.com/guide/navigation/navigation-3
- 许可：Apache License 2.0
- 说明：替换 miuix 的 navigation3 fork，未修改。

## MingCute Icons

- 位置：`art/icon.svg`、`app/src/main/res/drawable/ic_launcher_*`
- 来源：https://www.mingcute.com/
- 许可：Apache License 2.0
- 说明：应用图标基于 MingCute 图标修改而来（调整了配色、构图与尺寸，并转换为 Android VectorDrawable）。

## libxposed API / service

- 来源：https://github.com/libxposed/api、https://github.com/libxposed/service
- 许可：Apache License 2.0

## KavaRef

- 来源：https://github.com/HighCapable/KavaRef
- 许可：Apache License 2.0

## HiddenApiBypass

- 来源：https://github.com/LSPosed/AndroidHiddenApiBypass
- 许可：Apache License 2.0

## 架构参考

- XiaomiHelper：https://github.com/HowieHChen/XiaomiHelper （GPL-3.0）
  本项目的 Hooker 树、跨进程设置同步等设计参考了该项目。
