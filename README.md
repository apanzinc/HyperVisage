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
- 主界面底部叠加 miuix 0.9.4 的 `FloatingNavigationBar`，三个入口分别是主页、设置、关于；列表末尾留了 112dp，让最后一项能滚出遮挡。

## 柔光玻璃

组件库原版停在 2026 年 6 月，依赖 miuix 0.9.0，早于 HyperOS 4 的柔光玻璃设计语言，因此本项目把它升到了 miuix 0.9.4。

- `hyperx-compose/ui/effect/GlassEffect.kt`：`BloomStroke` 边缘光晕 + `rememberTiltLight` 随设备倾斜移动的主光源，深浅主题分别取预设
- `hyperx-compose/ui/layout/HyperXScaffold.kt`：改用 `progressiveTextureBlur` + `ProgressiveBlur.Top` 做线性渐变模糊（顶部最强、向下渐清晰，也就是从下到上逐渐模糊），并移除盖住模糊的纯色层
- `HyperXLayoutConfig.isGlassHighlightEnabled`：高光总开关，**默认关**。顶栏/底栏是贴边矩形，`BloomStroke` 的描边会显示成一条白色亮线；只有卡片类圆角玻璃面才建议打开
- `HyperXLayoutConfig.lightBlurAlpha / darkBlurAlpha`：玻璃底色浓度，默认 0.32 / 0.28；调太高会糊成一块纯色，把渐变遮住

系统级的柔光玻璃由小米自己渲染，第三方应用拿不到，这里是用 RuntimeShader 模拟，效果接近而非逐像素一致。`minSdk 35` 保证 `isRuntimeShaderSupported()` 恒为 true。

原项目那套页面切换特效来自 miuix 的 navigation3 fork，即 `miuix-navigation3-ui:0.9.0`，提供圆角裁剪、背后变暗、切换期间拦截触摸、跟手返回方向处理。0.9.4 起 miuix 不再发布这个 artifact，但 0.9.0 仍可用，只额外依赖 `miuix-shapes:0.9.0`，和 runtime 1.1.3 能共存，本工程升级前就是这个组合，所以现在保留它；升级到 0.9.4 的只有 ui / preference / icons / blur 四个模块。官方 `androidx.navigation3:navigation3-ui` 不能同时引入，两边会打包同一批 `androidx.navigation3.ui` / `scene` 类。详见 [NOTICE.md](NOTICE.md)。

转场节奏集中在 `hyperx-compose/ui/animation/HyperXNavTransitions.kt`：进入 350ms、返回 250ms、侧滑返回松手后收尾 180ms，统一 `FastOutSlowInEasing`，预测式返回的跟手阶段保持线性。返回场景设了 `targetContentZIndex = -1f`，让正在滑出的二级页留在上层，否则主页会直接盖住动画。别再换回手写弹簧曲线：之前那版 300ms 里前 90ms 就走完 89% 位移，剩下 210ms 几乎静止，看起来就是「进入没动画、返回拖着不动」。

## 崩溃日志

设备不方便接 ADB 时，未捕获异常会写到应用私有目录的 `last_crash.txt`；下次打开 App 会先显示一个日志页，可直接复制堆栈再关闭（关闭即删除日志）。

## 图标

应用图标基于 [MingCute](https://www.mingcute.com/) 的图标修改而来（调整了配色、构图与尺寸），依 Apache-2.0 授权使用。
原始 SVG 在 `art/icon.svg`，Android 侧的 VectorDrawable 由它生成。第三方许可明细见 [NOTICE.md](NOTICE.md)。

## 许可

GPL-3.0-only，见 [LICENSE](LICENSE)。

本项目的架构设计参考了 [XiaomiHelper](https://github.com/HowieHChen/XiaomiHelper)（GPL-3.0）。
`hyperx-compose/` 目录来自 [hyperx-compose](https://github.com/HowieHChen/hyperx-compose)（Apache-2.0），已作适配性修改，版权归原作者；改动清单见 [NOTICE.md](NOTICE.md)。
