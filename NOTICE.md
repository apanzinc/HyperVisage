# 第三方组件与素材

本项目为 GPL-3.0-only，但包含以下第三方成果，它们各自保留原许可证。

## hyperx-compose（已修改）

- 位置：`hyperx-compose/`
- 来源：https://github.com/HowieHChen/hyperx-compose
- 许可：Apache License 2.0
- 说明：HyperOS 风格的 Compose 组件库（布局、偏好项、导航）。以源码形式引入，**已作如下修改**：

  | 文件 | 改动 |
  |---|---|
  | `build.gradle.kts` | miuix 的 ui / preference / icons / blur 依赖 0.9.0 → 0.9.4；保留 `miuix-navigation3-ui:0.9.0`（0.9.4 起不再发布该 artifact），移除官方 `androidx.navigation3:navigation3-ui`，否则两套类会重复打包 |
  | `ui/effect/GlassEffect.kt` | 新增：柔光玻璃边缘高光 + 随设备倾斜的光源 |
  | `ui/layout/HyperXScaffold.kt` | 顶栏/底栏接入可选高光；模糊改为 `progressiveTextureBlur` 线性渐变（顶部最强向下渐清晰），移除盖住模糊的纯色层 |
  | `ui/layout/LayoutLocals.kt` | 新增 `isGlassHighlightEnabled`（默认关，避免顶栏出现白色描边）；玻璃底色默认值 0.8/0.7 → 0.32/0.28 |
  | `ui/effect/BlurEffect.kt` | 适配 miuix 0.9.4 移除的 `isRenderEffectSupported` |
  | `ui/theme/HyperXTheme.kt` | 适配 miuix 0.9.4 移除的 `smoothRounding` |
  | `ui/theme/HyperXShapes.kt` | 新增：`hyperxShape` 替代 miuix 0.9.4 移除的 `miuixShape` |
  | `ui/component/AdaptiveIcon.kt`、`ui/component/Card.kt`、`ui/layout/TabRow.kt` | 改用 `hyperxShape` |
  | `ui/layout/HyperXNavDisplay.kt`、`ui/layout/HyperXAppLayout.kt` | 新增 `HyperXNavDisplay`：照 fork 的 `NavDisplay(entries = ...)` 拆开一层，改用 `NavDisplay(sceneState, navigationEventState)` 重载并加 `predictiveBackEnabled`。关闭预测返回时给页面一个恒 Idle 的手势 state，返回不跟手但仍走普通 pop 动画 |
  | `ui/animation/HyperXNavTransitions.kt`、`ui/layout/HyperXAppLayout.kt` | 恢复 `NavDisplayTransitionEffects`（原项目的圆角裁剪 / 变暗 / 拦截输入 / 跟手返回方向，继续用 miuix fork 的 `NavDisplay`）；删除手写弹簧 `NavTransitionEasing`（阻尼=1 时判别式为 0 会算出 NaN，改临界阻尼后曲线又在 90ms 内走完 89% 位移，观感是「进入没动画、返回拖着不动」），改用标准 `FastOutSlowInEasing`；时长改为进入 350ms / 返回 250ms / 侧滑收尾 180ms；返回场景设置 `targetContentZIndex = -1f` 让滑出的二级页保持在上层；新增 `splitPopTransitionSpec` 修正分屏返回方向 |

  版权与原许可声明保留于原作者。依据 Apache-2.0 第 4 条，此处明确标注文件已被修改。

## miuix-kotlin-multiplatform

- 版本：0.9.4
- 来源：https://github.com/miuix-kotlin-multiplatform/miuix
- 许可：Apache License 2.0
- 说明：通过 Maven 依赖引入，未修改。提供基础组件、主题与 blur / highlight 管线。

## androidx.navigation3

- 版本：runtime 1.1.3
- 来源：https://developer.android.com/guide/navigation/navigation-3
- 许可：Apache License 2.0
- 说明：只使用 runtime（NavKey / NavEntry / rememberNavBackStack），未修改。ui 层由下面 miuix 的 fork 提供，两者不能同时引入 `navigation3-ui`。

## miuix-navigation3-ui

- 版本：0.9.0
- 坐标：`top.yukonga.miuix.kmp:miuix-navigation3-ui:0.9.0`
- 许可：Apache License 2.0
- 说明：官方 navigation3-ui 的 fork，额外提供 `NavDisplayTransitionEffects`（页面切换圆角裁剪、背后变暗、切换期间拦截输入、跟手返回方向）。通过 Maven 依赖引入，未修改。0.9.4 起 miuix 不再发布这个 artifact，但 0.9.0 与 runtime 1.1.3 可共存，本工程升级前就是这个组合。它只额外依赖 `miuix-shapes:0.9.0`。

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
