# UI-Kit 导航栏切换模式与易错点

> 适用：`easytier-miuix` 及后续复用 UI-Kit 的项目。
> 代码位置：`ui/kit/navigation/NavTabs.kt`（状态）、`ui/kit/components/BottomNav.kt`（普通形态底栏）、`ui/components/FloatingBottomBar.kt`（悬浮形态，依赖 miuix-blur）。

## 一、整体架构（单向数据流）

```
NavTabState（单一数据源，存 selectedId: String）
        │ 下发 selectedId / selectedIndex
        ▼
KitNavigationBar（普通形态，只读渲染）──┐
FloatingBottomBar（悬浮形态，只读渲染）─┤ 共用同一份状态
        │ 上报 onSelect(id) / onSelected(index)
        ▼
NavTabState.select(id) / selectByIndex(index)
```

两种形态（设置里的「悬浮底栏」开关切换）**共用同一份状态**，所以切换形态时选中页不丢；这也是「保留悬浮底栏开关」需求下的关键约束。

## 二、API 定义

### KitTab

```kotlin
data class KitTab(
    val id: String,          // 唯一标识（推荐用 enum.name），持久化与业务跳转都用它
    val label: String,
    val icon: ImageVector,
    val showBadge: Boolean = false,   // 小圆点徽标（如运行中 live 提示）
)
```

### NavTabState / rememberNavTabState

```kotlin
@Composable fun rememberNavTabState(tabs: List<KitTab>): NavTabState

class NavTabState internal constructor(ids: List<String>, initialId: String) {
    val selectedId: String          // 只读，经 select/open 修改
    val selectedIndex: Int          // 与底部条按下标交互的组件对接
    fun select(id: String)          // 未知 id 静默忽略
    fun selectByIndex(index: Int)   // 越界自动收敛
    fun open(id: String): Boolean   // 业务跳转；目标不存在返回 false
    companion object { fun saver(ids: List<String>): Saver<NavTabState, String> }
}
```

### KitNavigationBar（普通形态）

```kotlin
@Composable fun KitNavigationBar(
    tabs: List<KitTab>,
    selectedId: String,
    onSelect: (KitTab) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,   // 默认玻璃色 glassStrongTop
    badgeColor: Color? = null,       // 默认主色
)
```

### 纯函数（可独立测试）

```kotlin
fun coerceTabIndex(requested: Int, count: Int): Int          // 越界收敛，count<=0 返回 0
fun coerceTabId(requested: String?, valid: List<String>, fallback: String): String
```

## 三、本项目用法（AppNavigation.kt）

```kotlin
val navTabs = BottomBarDestination.entries.map { dest ->
    KitTab(
        id = dest.name,
        label = stringResource(dest.labelRes),
        icon = dest.icon,
        showBadge = networkRunning && dest == BottomBarDestination.Status,
    )
}
val navState = rememberNavTabState(navTabs)

// 业务跳转（消灭 selectedTab = 0 这类魔法数字）
navState.open(BottomBarDestination.Networks)

// 内容分发：按语义 id，else 兜底 Networks
when (navState.selectedId) {
    BottomBarDestination.Status.id -> StatusScreen(...)
    BottomBarDestination.Settings.id -> SettingsScreen(...)
    else -> NetworkListScreen(...)
}
```

悬浮形态对接（注意稳定 lambda）：

```kotlin
val selectedTabIndex = remember { { navState.selectedIndex } }  // ★ 必须 remember
FloatingBottomBar(
    selectedIndex = selectedTabIndex,
    onSelected = { navState.selectByIndex(it) },
    ...
)
```

## 四、易错点清单（按踩坑频率排序）

### 状态管理

1. **选中态不要用裸 Int 下标做持久化**。Tab 增删/排序后下标漂移；用 String id + `coerceTabId` 兜底。旧代码 `rememberSaveable { mutableIntStateOf(0) }` 已废弃。
2. **单一数据源**。两种底栏形态 + 页面跳转全部读写同一份 NavTabState；任何地方再存一份下标，必然出现「图标高亮和内容页不一致」。
3. **Tab 集合必须稳定**。`rememberNavTabState` 的 Saver 捕获了 ids，不要在重组中动态增删 Tab；动态徽标属于 Tab 的渲染属性（showBadge），不是集合变化。
4. **内容分发必须有 else 兜底**。即使状态已收敛，防御性 else 能把「未知状态白屏」降级为「回到首页」。

### 选中态同步

5. **传给 FloatingBottomBar 的 lambda 必须稳定**。`selectedIndex = { navState.selectedIndex }` 每次重组都是新实例，组件内部 `remember(selectedIndex)` / `LaunchedEffect(selectedIndex)` 会不停重启；用 `remember { { ... } }` 包一层。这是本项目实际存在过的问题。
6. **点击只回调、不内部改状态**。KitNavigationBar 选中态完全由外部下发（受控组件）；悬浮底栏内部动画索引（currentIndex）只是渲染副本，最终一致以 NavTabState 为准。
7. **悬浮底栏的拖拽路径**：内部拖完直接 `currentIndex = targetIndex` 再触发 `onSelected`，外部回写同值时 snapshotFlow 不会重复发射——不要在 onSelected 里再写别的副作用同步状态，会形成回环。

### 切换触发逻辑

8. **跨页跳转收敛到一个入口**。`navState.open(Dest)` 返回 Boolean；不要在各个 Screen 里自己改全局导航状态（旧代码 StatusScreen 的 `onStartNetwork = { selectedTab = 0 }` 已改）。
9. **徽标是派生态**。`showBadge = networkRunning && dest == Status` 在构建 tabs 时算好，两种形态自动一致；不要写 `index == 1` 这种魔法判断（Tab 换序即错）。
10. **切换 Tab 不进返回栈**。本项目返回键只处理二级页（编辑页 > 主题页 > 退出），Tab 切换不可用返回键撤销——这是有意设计；若要 Tab 进栈需换 Navigation 库，别在 callback 里手工拼栈。

### 边界情况

11. **越界恢复**：进程重建 / 旧版本数据 / Tab 数量变化 → `coerceTabIndex` / `coerceTabId` 兜底回合法值，绝不白屏。
12. **悬浮底栏开启时内容底部留白**：普通形态由 Scaffold bottomBar 撑开，悬浮形态 bottomBar 为空，必须给列表传 `contentBottomPadding = FloatingBottomBarBottomMargin + FloatingBottomBarHeight + navBarBottom`，否则最后一项被悬浮条遮住。
13. **徽标出现/消失不要引起图标跳动**：kit 的 BadgedIcon 徽标槽始终占位。
14. **深浅色/主题色跟随**：底栏颜色一律取 `LocalPalette`，不要硬编码 `MiuixTheme.colorScheme.primary`（两套色系在自定义主题色时会不一致）。
15. **二级页与底栏互斥**：`showBottomBar = editingInstanceId == null && !showThemeSettings`——进入二级页必须同时隐藏底栏，返回时恢复；忘了任一半都会出现「底栏点不动」或「二级页有底栏」。

## 五、测试覆盖

`app/src/test/java/top/easytier/miuix/ui/kit/navigation/NavTabStateTest.kt`（16 个用例）：

- `coerceTabIndex`：负数 / 越界 / 合法 / 空列表
- `coerceTabId`：null / 未知 id / 合法 id
- `NavTabState`：初始态 / select 合法与未知 / selectByIndex 越界 / open 返回值与副作用
- `Saver`：保存 selectedId / 恢复合法 id / 恢复未知 id 兜底
- 单 Tab 边界

## 六、变更历史

- 2026-11-21：提炼 NavTabState / KitTab / KitNavigationBar 进 UI-Kit，AppNavigation 迁移完成（兼容悬浮底栏与普通底栏两种形态），补 17 个 JVM 单测与本文档。
