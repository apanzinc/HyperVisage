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
- 模块设置页新增「主题模式」下拉：跟随系统 / 浅色模式 / 深色模式，默认跟随系统。存本地 SharedPreferences（`Preferences.App.THEME_MODE`，0/1/2），经 `AppSettings.themeMode` 的 StateFlow 在 `MainActivity.AppContent` 映射成 miuix 的 `ColorSchemeMode.System/Light/Dark`，传给 `HyperXAppLayout` 新增的 `colorSchemeMode` 参数（内部 `HyperXTheme` 据此重建 `ThemeController`）。崩溃报告页用同一套配色。切换后整棵 Compose 树重组，不需要重建 Activity。
- 模块设置页的「界面模糊」默认开启，走本地存储。页面内顶栏的渐进模糊仍会在页面转场时降级；底栏模糊是高优先级、常驻效果，浅色深色都完整渲染、转场不降级，底栏材质层只保留模糊本身，不叠任何颜色或底色；胶囊和圆钮各描一圈中性灰边，深浅主题同色。开关关掉后所有模糊完全跳过，可用来直接判断掉帧是不是模糊造成的。
- 主页 / 设置 / 关于是**并行的三个顶层标签页**，由 `app/src/main/kotlin/dev/apanzinc/hypervisage/ui/pages/MainTabsScreen.kt` 的 `HorizontalPager` 承载，不进导航栈；`navigator.push` 只留给示例功能、搜索这类二级页。点底栏走 `animateScrollToPage`，手指横滑也能直接切页，三页各自的滚动位置和输入状态都保留。没有二级页时返回键先滑回主页标签。
- 底栏**不属于任何页面**。`HyperXAppLayout` 新增 `overlayContent: @Composable BoxScope.(LayerBackdrop?) -> Unit`，窗口级 `Box` 把 `AnimatedContent`（全部页面与布局）和覆盖层并列，覆盖层在所有页面之上绘制；`rememberLayerBackdrop` 录的是整棵内容树，不是某一个 Pager。因此底栏不会被 NavDisplay 的场景转场裁剪、缩放、变暗，也不会跟着页面一起被降级。`MainTabsScreen` 现在只剩 `HorizontalPager`，`LocalPageTransitionRunning` 的降级只作用于页面内顶栏。
- 底栏底层（`app/ui/component/HyperVisageBottomBarOverlay.kt`）固定贴底、高 148dp，铺 `progressiveTextureBlur` + `ProgressiveBlur.Bottom`（翻转 180°：底部最模糊、越往上越清晰），半径 22dp。**半径单位是 dp**，miuix-blur 的 effect 内部会自己乘 density，再 `toPx()` 会把半径放大 density 倍。模糊层上不叠任何东西：不传 `BlurColors`（没有 blend 色，也不调 `brightness` / `contrast` / `saturation`）、不 `drawBehind` 补渐变、不画描边或玻璃底色，深浅主题走完全相同的路径。按钮的灰边画在 `HyperVisageBottomBar.kt` 里，不落在这一层。深色下就是纯黑色调的模糊，不会再出现白底或灰白雾面；浅色下也不靠冷灰染色来「看出模糊」。只有关掉「界面模糊」或设备不支持 RuntimeShader 时才整层不绘制，同样不补底色。
- 选中高光（选择器）直接读 `currentPage + currentPageOffsetFraction` 这个连续进度，在 draw 阶段算位置，手指拖到哪高光到哪，不再跑一套自己的弹簧。胶囊本身也支持横向拖动：`pointerInput` 把手指位移换算成页偏移喂给 `PagerState.dispatchRawDelta(pageDelta * layoutInfo.pageSize)`，页面和高光同一份进度，松手用弹簧吸到最近页。方向按真机手感取正（手指左滑进下一页），不按源码推导的符号硬套。Pager 状态与底栏拖动状态都由 `MainActivity` 提升，页面内容和窗口级底栏共用同一份进度。三个标签页和两个二级页列表末尾都留 112dp，让内容能滚到底栏后面，模糊才有东西可采。搜索结果用一个 `MutableSharedFlow<Int>` 通知 Pager 滑到对应标签。
- 模块设置里的「标准底栏」默认关闭。打开后不画分离式胶囊，改用 miuix 固定贴底通栏的 `NavigationBar`（不是浮起来的 `FloatingNavigationBar`），图标和搜索入口都保留；开关存本地 SharedPreferences，不依赖 LSPosed。
- 底栏按钮（`app/src/main/kotlin/dev/apanzinc/hypervisage/ui/component/HyperVisageBottomBar.kt`）：胶囊放主页 / 设置 / 关于三项，右侧独立圆钮是搜索入口。按钮自己再叠一层 `textureBlur`，半径 32dp，强于底层的 22dp；同样不传 `BlurColors`、不画表面，白实底、半透明白、白色描边、柔光渐变、三层柔光边都已删除，开着模糊时表面只有模糊本身。胶囊和圆钮的外轮廓各加一条 1dp 中性灰边（`Color(0xFF949494)`，0.70 alpha），深浅主题用同一个颜色、关掉模糊也照常保留，用来给没有轮廓的纯模糊勾出边界；底层材质层仍不加边，边界只靠模糊渐变。选中指示只留一层 `contentColor` 0.13 alpha 的极淡圆角块，仍跟 Pager 连续进度走、按压时压扁，本身不画底也不描边。关闭模糊时没有纹理可采，按钮才退回不透明实底。图标文字浅色用黑色、深色用主题前景；两种模式都只在胶囊和圆钮上加 3dp 轻微扩散阴影，模糊材质背后不加阴影。按下时该项图标和文字变灰，松手弹簧回弹。结构和尺寸参照 miuix 上游 main 分支尚未发布的 `miuix-glass/GlassNavigationBar`。

## 转场性能

`AnimatedContent` 转场时会同时合成两个场景。若每个场景都各自 `layerBackdrop` 录制内容纹理、各自跑一遍 `progressiveTextureBlur` 的 RuntimeShader，每帧开销翻倍。miuix 0.9.4 的 API 文档也注明渐进模糊比普通模糊多一趟全分辨率叠加 pass，所以**页面内**的模糊在转场期间要停掉；窗口级底栏模糊是常驻的高优先级效果，不降级。

- `HyperXNavDisplay` 维护转场状态机：导航栈变化覆盖 push / pop，手势 `InProgress` 覆盖跟手阶段；跟手的 `progress` 每帧都在变，用 `distinctUntilChanged` 只关心是否处于 `InProgress`，避免反复取消重启收尾协程
- `LocalPageTransitionRunning` 下发的是 `State<Boolean>` 引用而不是 Boolean 值，读值的只有页面内顶栏那一小块组合。直接下发 Boolean 会让整棵 NavDisplay 在动画开始和结束时各重组一次，连带重建 `SceneInfo` 和手势 state，正好卡在那两帧上
- `HyperXScaffold` 不拆装绘制节点，改用 miuix 0.9.4 自带的 `enabled` 开关：`drawBackdrop(enabled = false)` 时 `draw()` 直接 `drawContent()` 返回、`measure()` 也不 `placeWithLayer`，跳过整个 shader 而不重建节点树。底色在 `drawBehind` 里读 state 切换，只触发重绘
- `BlurEffect` 在转场期间跳过离屏纹理里的 `drawContent()`，两场景合成时少重画一遍整页内容；屏幕上的内容由 `LayerBackdropNode` 正常绘制，不受影响
- 页面内的线性渐进模糊静止时和转场结束后仍完整渲染，只有切换过程降级；窗口级底栏模糊全程常驻。降级/恢复各一次轻量的节点属性更新，不再有节点重建

## 柔光玻璃

组件库原版停在 2026 年 6 月，依赖 miuix 0.9.0，早于 HyperOS 4 的柔光玻璃设计语言，因此本项目把它升到了 miuix 0.9.4。

- `hyperx-compose/ui/effect/GlassEffect.kt`：`BloomStroke` 边缘光晕 + `rememberTiltLight` 随设备倾斜移动的主光源，深浅主题分别取预设
- `hyperx-compose/ui/layout/HyperXScaffold.kt`：改用 `progressiveTextureBlur` + `ProgressiveBlur.Top` 做线性渐变模糊（顶部最强、向下渐清晰，也就是从下到上逐渐模糊），并移除盖住模糊的纯色层；转场期间按 `LocalPageTransitionRunning` 用 `enabled` 关掉 shader，见「转场性能」
- `app/ui/component/HyperVisageBottomBarOverlay.kt`：窗口级底栏材质层，`progressiveTextureBlur` + `ProgressiveBlur.Bottom`（翻转 180°，下面最模糊、上面越来越清晰），半径 22dp；采样的是 `HyperXAppLayout` 挂在整个内容层上的窗口级 `LayerBackdrop`，和页面内 `HyperXScaffold` 的顶栏 backdrop 是两层。不读 `LocalPageTransitionRunning`，转场时不降级；浅色深色都完整渲染。胶囊和圆钮另外各叠一层 32dp 的 `textureBlur`
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
