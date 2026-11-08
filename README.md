# HyperVisage

一个适用于 HyperOS 4 的美化 LSPosed 模块。

## 环境要求

- HyperOS 4（Android 17 / API 37）
- LSPosed，支持 libxposed API 101
- 构建：JDK 21 + Android SDK 37

## 构建

```bash
# 本地需要 local.properties 指向 Android SDK
./gradlew :app:assembleDebug
```
产物在 `app/build/outputs/apk/debug/`。

## 架构

```
app/src/main/kotlin/dev/apanzinc/hypervisage/
├─ HookEntry.kt            XposedModule 入口，按包名分发给对应的 scope
├─ hook/
│  ├─ base/                Hooker 树：BaseHooker / StaticHooker / DynamicHooker
│  ├─ scopes/              每个目标应用一个入口（SystemUI、AndroidFramework…）
│  ├─ rules/               具体功能，挂在 scope 下面
│  └─ utils/               跨进程设置、日志、Context 持有
├─ data/                   包名常量、路由、PreferenceKey 定义
├─ ui/                     Compose 界面（miuix / hyperx-compose）
└─ di/                     Koin 模块
```

### 三条主链路

**设置下发**
设置页写入 remote preferences → 目标进程 `RemotePreferences.get()` 读取 → `observe` 监听变化即时生效。

**Hooker 树**
scope 级 Hooker `attach` 若干 rule 级 Hooker，父节点关闭时子节点全部卸载（`DynamicHooker` 可运行时卸载，`StaticHooker` 不行）。

**目标分发**
`HookEntry.hookerOf(pkg)` 一个 when 决定该进程挂哪棵树。新增目标 = 加常量 + 建 scope + 加一个分支。

### 加一个新功能

1. `data/preference/Preferences.kt` 加一个 `PreferenceKey`
2. `ui/pages/` 加设置页（或塞进已有的页）
3. `hook/rules/` 加一个 Rule，`onInit` 里 `observe` 那个 key
4. 在对应 scope 的 `onReady()` 里 `attach` 这个 Rule

## 界面设置

- 模块设置页的「预测返回」默认关闭。开启后侧滑返回跟手，关闭后不跟手、松手直接走普通返回动画。
- 该开关只影响本 App 界面，存本地 SharedPreferences（`AppSettings`），不依赖 LSPosed 服务是否连上。
- fork 的 `NavDisplay(backStack = ...)` 内部会无条件注册 `NavigationBackHandler`，外部没法关掉跟手效果。`HyperXNavDisplay` 照它的 `NavDisplay(entries = ...)` 重载拆开一层，改用 `NavDisplay(sceneState, navigationEventState)` 重载：系统返回手势始终由应用消费，但关闭时交给页面的是另一个永远停在 Idle 的手势 state。
- 模块设置页的「界面模糊」默认开启，走本地存储，控制顶栏渐进模糊；底栏底层只在深色模式下启用，浅色模式整层隐藏（不建 `LayerBackdrop`、不跑 RuntimeShader、也不补任何底色，按钮直接浮在页面背景上）。开关关掉后两层模糊都完全跳过，可用来直接判断转场掉帧是不是模糊造成的。
- 主页 / 设置 / 关于是**并行的三个顶层标签页**，由 `app/src/main/kotlin/dev/apanzinc/hypervisage/ui/pages/MainTabsScreen.kt` 的 `HorizontalPager` 承载，不进导航栈；`navigator.push` 只留给示例功能、搜索这类二级页。点底栏走 `animateScrollToPage`，手指横滑也能直接切页，三页各自的滚动位置和输入状态都保留。没有二级页时返回键先滑回主页标签。
- 底栏固定在屏幕底部。底层要采到内容纹理，所以 `MainTabsScreen` 用 `Box` 让 Pager 铺满全屏、底栏作为覆盖层压在底部（不是带留白的悬浮胶囊）；三个标签页列表末尾留 112dp，最后一项仍能滚出遮挡。底栏底层只在深色 + 模糊开启时铺 `progressiveTextureBlur`，翻转 180° 用 `ProgressiveBlur.Bottom`：底部最模糊、越往上越清晰；`BlurColors` 叠一层背景色，`drawBehind` 只补从透明到 0.35 alpha 的淡竖向过渡。浅色模式、模糊关闭、转场 / 拖动时整层隐藏，不画任何底色、不拆节点（浅色下早先补的纯背景色会连成一条白，已去掉）。切页时底栏不重建；选中高光（选择器）直接读 `currentPage + currentPageOffsetFraction` 这个连续进度，在 draw 阶段算位置，手指拖到哪高光到哪，不再跑一套自己的弹簧。胶囊本身也支持横向拖动：`pointerInput` 把手指位移换算成页偏移喂给 `PagerState.dispatchRawDelta(pageDelta * layoutInfo.pageSize)`，页面和高光同一份进度，松手用弹簧吸到最近页。方向按真机手感取正（手指左滑进下一页），不按源码推导的符号硬套。底栏拖动同样并入 `LocalPageTransitionRunning`，横滑时两页不会各自跑一遍模糊 shader，见「转场性能」。搜索结果用一个 `MutableSharedFlow<Int>` 通知 Pager 滑到对应标签。
- 模块设置里的「标准底栏」默认关闭。打开后不画分离式胶囊，改用 miuix 固定贴底通栏的 `NavigationBar`（不是浮起来的 `FloatingNavigationBar`），图标和搜索入口都保留；开关存本地 SharedPreferences，不依赖 LSPosed。
- 底栏按钮（`app/src/main/kotlin/dev/apanzinc/hypervisage/ui/component/HyperVisageBottomBar.kt`）：胶囊放主页 / 设置 / 关于三项，右侧独立圆钮是搜索入口。两套主题分开处理：浅色是白色实底 + 白色 0.75dp 描边 + 黑色图标文字（之前直接用 `onBackground`，图标糊在白底里看不见），选中项是同色系稍深的一层；深色是柔光玻璃（极淡渐变 + 三层柔光边，`BloomStroke` 的纯绘制近似）+ 白色内容。两种模式都只在胶囊和圆钮上加 3dp 轻微扩散阴影，模糊材质背后不加阴影。按下时该项图标和文字变灰，选择器压扁并轻微拉宽，松手弹簧回弹。结构和尺寸参照 miuix 上游 main 分支尚未发布的 `miuix-glass/GlassNavigationBar`。

## 转场性能

`AnimatedContent` 转场时会同时合成两个场景。若每个场景都各自 `layerBackdrop` 录制内容纹理、各自跑一遍 `progressiveTextureBlur` 的 RuntimeShader，每帧开销翻倍。miuix 0.9.4 的 API 文档也注明渐进模糊比普通模糊多一趟全分辨率叠加 pass，所以转场期间要把它停掉。

- `HyperXNavDisplay` 维护转场状态机：导航栈变化覆盖 push / pop，手势 `InProgress` 覆盖跟手阶段；跟手的 `progress` 每帧都在变，用 `distinctUntilChanged` 只关心是否处于 `InProgress`，避免反复取消重启收尾协程
- `LocalPageTransitionRunning` 下发的是 `State<Boolean>` 引用而不是 Boolean 值，读值的只有顶栏/底栏那一小块组合。直接下发 Boolean 会让整棵 NavDisplay 在动画开始和结束时各重组一次，连带重建 `SceneInfo` 和手势 state，正好卡在那两帧上
- `HyperXScaffold` 不拆装绘制节点，改用 miuix 0.9.4 自带的 `enabled` 开关：`drawBackdrop(enabled = false)` 时 `draw()` 直接 `drawContent()` 返回、`measure()` 也不 `placeWithLayer`，跳过整个 shader 而不重建节点树。底色在 `drawBehind` 里读 state 切换，只触发重绘
- `BlurEffect` 在转场期间跳过离屏纹理里的 `drawContent()`，两场景合成时少重画一遍整页内容；屏幕上的内容由 `LayerBackdropNode` 正常绘制，不受影响
- 静止状态和转场结束后仍是完整的线性渐进模糊，只有切换过程降级；降级/恢复各一次轻量的节点属性更新，不再有节点重建

## 柔光玻璃

组件库原版停在 2026 年 6 月，依赖 miuix 0.9.0，早于 HyperOS 4 的柔光玻璃设计语言，因此本项目把它升到了 miuix 0.9.4。

- `hyperx-compose/ui/effect/GlassEffect.kt`：`BloomStroke` 边缘光晕 + `rememberTiltLight` 随设备倾斜移动的主光源，深浅主题分别取预设
- `hyperx-compose/ui/layout/HyperXScaffold.kt`：改用 `progressiveTextureBlur` + `ProgressiveBlur.Top` 做线性渐变模糊（顶部最强、向下渐清晰，也就是从下到上逐渐模糊），并移除盖住模糊的纯色层；转场期间按 `LocalPageTransitionRunning` 用 `enabled` 关掉 shader，见「转场性能」
- `app/ui/pages/MainTabsScreen.kt` 的 `BottomBarBlurLayer`：仅深色 + 界面模糊开启时创建 `rememberBlurBackdrop`，渐变换成 `ProgressiveBlur.Bottom`（翻转 180°，下面最模糊、上面越来越清晰），半径 18dp 比顶栏小；`layerBackdrop` 挂在 Pager 上，和页面内 `HyperXScaffold` 的顶栏 backdrop 是两层，静止时才跑 shader；浅色模式不创建 backdrop、整层不绘制
- `HyperXLayoutConfig.isGlassHighlightEnabled`：高光总开关，**默认关**。顶栏/底栏是贴边矩形，`BloomStroke` 的描边会显示成一条白色亮线；只有卡片类圆角玻璃面才建议打开
- `HyperXLayoutConfig.lightBlurAlpha / darkBlurAlpha`：玻璃底色浓度，默认 0.32 / 0.28；调太高会糊成一块纯色，把渐变遮住

系统级的柔光玻璃由小米自己渲染，第三方应用拿不到，这里是用 RuntimeShader 模拟，效果接近而非逐像素一致。`minSdk 35` 保证 `isRuntimeShaderSupported()` 恒为 true。

原项目那套页面切换特效来自 miuix 的 navigation3 fork，即 `miuix-navigation3-ui:0.9.0`，提供圆角裁剪、背后变暗、切换期间拦截触摸、跟手返回方向处理。0.9.4 起 miuix 不再发布这个 artifact，但 0.9.0 仍可用，只额外依赖 `miuix-shapes:0.9.0`，和 runtime 1.1.3 能共存，本工程升级前就是这个组合，所以现在保留它；升级到 0.9.4 的只有 ui / preference / icons / blur 四个模块。官方 `androidx.navigation3:navigation3-ui` 不能同时引入，两边会打包同一批 `androidx.navigation3.ui` / `scene` 类。详见 [NOTICE.md](NOTICE.md)。

转场节奏集中在 `hyperx-compose/ui/animation/HyperXNavTransitions.kt`：进入 500ms、返回 500ms、侧滑返回跟手阶段 550ms，与参考项目及 miuix fork 的 `defaultTransitionSpec` / `defaultPopTransitionSpec` 完全一致。进入和返回用欠阻尼弹簧 `NavTransitionEasing(0.8, 0.95)`，跟手阶段保持线性。曾缩短到 350/250/180ms 并改用 `FastOutSlowInEasing`，可见滑动被压进前 100ms，观感是「闪一下」，已回退。弹簧参数别调到临界阻尼 `(0.5, 1.0)`：判别式为 0 时 `w=0`、`c2=-Inf` 会算出 NaN 导致切换崩溃，类里已对判别式非正的情况留了临界阻尼分支兜底。返回时的层级由 fork 的 `NavDisplay` 自己算 `targetZIndex`，自定义 spec 里设 `targetContentZIndex` 会被覆盖，不必再设。

## 崩溃日志

设备不方便接 ADB 时，未捕获异常会写到应用私有目录的 `last_crash.txt`；下次打开 App 会先显示一个日志页，可直接复制堆栈再关闭（关闭即删除日志）。

## 图标

应用图标基于 [MingCute](https://www.mingcute.com/) 的图标修改而来（调整了配色、构图与尺寸），依 Apache-2.0 授权使用。
原始 SVG 在 `art/icon.svg`，Android 侧的 VectorDrawable 由它生成。

底栏图标按参考截图里分离式底栏的造型自绘（`res/drawable/nav_*.xml`）：主页 / 设置 / 关于 / 搜索四个图标统一 24dp viewport，当前只用面性那一套，选中与未选中不换图标、不做交叉淡入。线性的四个 drawable 保留备用但不参与绘制。不用 Material Icons，也不用 miuix misymbol 的字重差（那套只有粗细五档、没有实心版，混用会粗细与线面都不统一）。第三方许可明细见 [NOTICE.md](NOTICE.md)。

## 许可

GPL-3.0-only，见 [LICENSE](LICENSE)。

本项目的架构设计参考了 [XiaomiHelper](https://github.com/HowieHChen/XiaomiHelper)（GPL-3.0）。
`hyperx-compose/` 目录来自 [hyperx-compose](https://github.com/HowieHChen/hyperx-compose)（Apache-2.0），已作适配性修改，版权归原作者；改动清单见 [NOTICE.md](NOTICE.md)。
