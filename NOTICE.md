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
  | `ui/effect/BlurEffect.kt` | 适配 miuix 0.9.4 移除的 `isRenderEffectSupported`；转场期间跳过离屏纹理里的 `drawContent()`，避免两场景合成时把整页内容重画两遍 |
  | `ui/theme/HyperXTheme.kt` | 适配 miuix 0.9.4 移除的 `smoothRounding` |
  | `ui/theme/HyperXShapes.kt` | 新增：`hyperxShape` 替代 miuix 0.9.4 移除的 `miuixShape` |
  | `ui/component/AdaptiveIcon.kt`、`ui/component/Card.kt`、`ui/layout/TabRow.kt` | 改用 `hyperxShape` |
  | `ui/layout/HyperXNavDisplay.kt`、`ui/layout/HyperXAppLayout.kt` | 新增 `HyperXNavDisplay`：按 fork 的 `NavDisplay(entries = ...)` 重载 1:1 组装，改用 `NavDisplay(sceneState, navigationEventState)` 重载并加 `predictiveBackEnabled`。关闭预测返回时给页面一个恒 Idle 的手势 state，返回不跟手但仍走普通 pop 动画；另加转场状态机（导航栈变化 + 手势 `InProgress`），通过 `LocalPageTransitionRunning` 下发 |
  | `ui/layout/HyperXScaffold.kt`、`ui/layout/LayoutLocals.kt`、`ui/layout/HyperXNavDisplay.kt` | 新增 `LocalPageTransitionRunning`（下发 `State<Boolean>` 引用，避免转场开始/结束时整棵导航树重组）；顶栏/底栏恒定挂载绘制节点，转场期间用库自带的 `progressiveTextureBlur(enabled = false)` 跳过 RuntimeShader 而非拆装节点，底色在 draw 阶段随 state 切换；跟手状态流加 `distinctUntilChanged` |
  | `ui/animation/HyperXNavTransitions.kt`、`ui/layout/HyperXAppLayout.kt` | 恢复 `NavDisplayTransitionEffects`（原项目的圆角裁剪 / 变暗 / 拦截输入 / 跟手返回方向，继续用 miuix fork 的 `NavDisplay`）；恢复参考项目的欠阻尼弹簧 `NavTransitionEasing(0.8, 0.95)` 与 500/500/550ms 时长，替换掉此前 350/250/180ms + `FastOutSlowInEasing` 造成的「闪一下」；对判别式非正的情况保留临界阻尼分支，避免参数调到 `(0.5, 1.0)` 时 `w=0`、`c2=-Inf` 算出 NaN 崩溃；移除无效的 `targetContentZIndex`（fork 的 `NavDisplay` 会用自己的 `targetZIndex` 覆盖）；新增 `splitPopTransitionSpec` 修正分屏返回方向 |

  版权与原许可声明保留于原作者。依据 Apache-2.0 第 4 条，此处明确标注文件已被修改。

## miuix-kotlin-multiplatform

- 版本：0.9.4
- 来源：https://github.com/miuix-kotlin-multiplatform/miuix
- 许可：Apache License 2.0
- 说明：通过 Maven 依赖引入，未修改。提供基础组件、主题与 blur / highlight 管线。
- 另：主界面底栏 `app/src/main/kotlin/dev/apanzinc/hypervisage/ui/component/HyperVisageBottomBar.kt` 的结构与尺寸参照了该仓库 main 分支尚未发布的 `miuix-glass` 模块中的 `GlassNavigationBar` / `GlassNavigationBarLayout`。未复制其源码，也未引入该模块；实现只用 0.9.4 的公开主题颜色与 Compose 基础绘制 API。

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

## 小米 misymbol 图标（随 miuix-icons 0.9.4 引入）

- 来源：https://github.com/compose-miuix-ui/miuix ，图标字形提取自小米官方 `misymbol_vf.ttf` 图标字体
- 许可：miuix 项目为 Apache License 2.0，图标随该依赖引入，未修改；字形本身的权属归小米
- 说明：底栏主页 / 设置 / 关于 / 搜索改用这套小米官方图标，见 `app/src/main/kotlin/dev/apanzinc/hypervisage/ui/component/NavIcons.kt`。该图标集按字体字重提供 Light / Normal / Regular / Medium / Demibold 五档笔画粗细，没有独立的实心「面性」版本，因此选中用 Demibold、未选中用 Light 做粗细对比。此前使用的 AndroidX Material Icons 依赖已移除。应用图标仍来自 MingCute，两者不混用。

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
