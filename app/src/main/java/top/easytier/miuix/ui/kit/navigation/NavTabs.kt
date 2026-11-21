package top.easytier.miuix.ui.kit.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * # 底部导航页签（UI-Kit 统一模型）
 *
 * 用 [id]（字符串）作为页签的唯一标识，与显示顺序解耦：
 * - 持久化保存的是 id 而不是下标，未来调整 Tab 顺序/数量时不会错位；
 * - 业务跳转（如「状态页 → 去启动网络」）用语义 id，消灭魔法数字。
 */
data class KitTab(
    val id: String,
    val label: String,
    val icon: ImageVector,
    /** 是否显示小圆点徽标（如「运行中」的 live 提示）。 */
    val showBadge: Boolean = false,
)

/**
 * 纯函数：把任意「恢复的/请求的」下标收敛到合法区间。
 *
 * 场景：rememberSaveable 恢复旧状态、Tab 数量随版本变化、外部传入越界值。
 * 原则：**任何来源的下标进入渲染/选择前必须过一遍这个函数**，否则可能白屏或崩溃。
 */
fun coerceTabIndex(requested: Int, count: Int): Int =
    if (count <= 0) 0 else requested.coerceIn(0, count - 1)

/**
 * 纯函数：把恢复的 id 收敛到当前合法集合；未知 id（跨版本 Tab 重命名/删除）回退到 fallback。
 * 持久化 String id 的安全性全靠这一层兜底。
 */
fun coerceTabId(requested: String?, valid: List<String>, fallback: String): String =
    if (requested != null && requested in valid) requested else fallback

/**
 * # 底部导航选中态（单一数据源）
 *
 * 设计要点：
 * - **单一数据源**：底部条（普通/悬浮两种形态）只读这里，选中态不会出现两份真值；
 * - **防脏状态**：构造与恢复时都会经过 [coerceTabId]，越界 id 自动回退第一个 Tab；
 * - **可测试**：核心逻辑不依赖 Android，普通 JVM 单测即可覆盖。
 *
 * 不暴露 setter 给随意赋值，只允许 [select] / [selectByIndex] / [open]——
 * 所有写入路径都在同一处做合法性检查，避免「下标和 id 各改一份」。
 */
@Stable
class NavTabState internal constructor(
    private val ids: List<String>,
    initialId: String,
) {
    var selectedId: String by mutableStateOf(initialId)
        private set

    /** 当前选中下标（与 [ids] 对齐；理论上是冗余视图，但底部条组件大多按下标交互）。 */
    val selectedIndex: Int
        get() = ids.indexOf(selectedId).coerceAtLeast(0)

    /** 选中指定 Tab；未知 id 静默忽略（业务侧用 [open] 可以拿到结果）。 */
    fun select(id: String) {
        if (id in ids) selectedId = id
    }

    /** 按下标选中；越界下标自动收敛，不会抛异常。 */
    fun selectByIndex(index: Int) {
        ids.getOrNull(coerceTabIndex(index, ids.size))?.let { selectedId = it }
    }

    /** 业务跳转：目标存在时切换并返回 true，不存在返回 false（不破坏当前选中态）。 */
    fun open(id: String): Boolean =
        if (id in ids) {
            selectedId = id
            true
        } else {
            false
        }

    companion object {
        /**
         * Saver 只持久化 [selectedId]（字符串），恢复时经 [coerceTabId] 兜底。
         * 注意：saver 捕获了 [ids]，Tab 集合必须在整个生命周期内稳定（底部导航通常如此）。
         */
        fun saver(ids: List<String>): Saver<NavTabState, String> =
            object : Saver<NavTabState, String> {
                override fun SaverScope.save(value: NavTabState): String = value.selectedId

                override fun restore(value: String): NavTabState =
                    NavTabState(ids, coerceTabId(value, ids, ids.first()))
            }
    }
}

/**
 * 创建可跨进程重建恢复的导航状态。
 *
 * 为什么用 id 而不是下标做 rememberSaveable：
 * 下标会随 Tab 增删/排序漂移；id 失效时还有 [coerceTabId] 兜底回第一页。
 *
 * 注意：[tabs] 的 id 集合必须稳定（不要在 recomposition 里动态增删 Tab），
 * 否则恢复的 id 可能被判为非法而回退。
 */
@Composable
fun rememberNavTabState(tabs: List<KitTab>): NavTabState {
    val ids = tabs.map { it.id }
    val fallback = ids.first()
    return rememberSaveable(saver = NavTabState.saver(ids)) {
        NavTabState(ids, fallback)
    }
}
