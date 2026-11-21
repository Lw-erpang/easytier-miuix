package top.easytier.miuix.ui.kit.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 导航选中态核心逻辑的 JVM 单测。
 * NavTabState 内部用 Compose SnapshotState，在 JVM 单测中可直接读写（无需 Android 环境）。
 */
class NavTabStateTest {

    private val ids = listOf("networks", "status", "settings")

    private fun state(initial: String = "networks") = NavTabState(ids, initial)

    // ---------- coerceTabIndex ----------

    @Test
    fun `coerceTabIndex 负数收敛到 0`() {
        assertEquals(0, coerceTabIndex(-1, 3))
        assertEquals(0, coerceTabIndex(-100, 3))
    }

    @Test
    fun `coerceTabIndex 越界收敛到最后一页`() {
        assertEquals(2, coerceTabIndex(3, 3))
        assertEquals(2, coerceTabIndex(99, 3))
    }

    @Test
    fun `coerceTabIndex 合法下标原样返回`() {
        assertEquals(1, coerceTabIndex(1, 3))
    }

    @Test
    fun `coerceTabIndex 空列表返回 0 不崩溃`() {
        assertEquals(0, coerceTabIndex(0, 0))
        assertEquals(0, coerceTabIndex(5, 0))
    }

    // ---------- coerceTabId ----------

    @Test
    fun `coerceTabId 未知 id 回退 fallback`() {
        assertEquals("networks", coerceTabId("deleted_tab", ids, "networks"))
    }

    @Test
    fun `coerceTabId null 回退 fallback`() {
        assertEquals("status", coerceTabId(null, ids, "status"))
    }

    @Test
    fun `coerceTabId 合法 id 原样返回`() {
        assertEquals("settings", coerceTabId("settings", ids, "networks"))
    }

    // ---------- NavTabState 选中态 ----------

    @Test
    fun `初始选中第一个 Tab`() {
        val s = state()
        assertEquals("networks", s.selectedId)
        assertEquals(0, s.selectedIndex)
    }

    @Test
    fun `select 合法 id 切换选中`() {
        val s = state()
        s.select("settings")
        assertEquals("settings", s.selectedId)
        assertEquals(2, s.selectedIndex)
    }

    @Test
    fun `select 未知 id 静默忽略`() {
        val s = state()
        s.select("nope")
        assertEquals("networks", s.selectedId)
    }

    @Test
    fun `selectByIndex 越界下标收敛不崩溃`() {
        val s = state()
        s.selectByIndex(-1)
        assertEquals("networks", s.selectedId)
        s.selectByIndex(99)
        assertEquals("settings", s.selectedId)
        s.selectByIndex(1)
        assertEquals("status", s.selectedId)
    }

    @Test
    fun `open 已知 id 返回 true 并切换`() {
        val s = state()
        assertTrue(s.open("status"))
        assertEquals("status", s.selectedId)
    }

    @Test
    fun `open 未知 id 返回 false 且选中态不变`() {
        val s = state()
        assertFalse(s.open("nope"))
        assertEquals("networks", s.selectedId)
    }

    // ---------- Saver（恢复路径） ----------
    // 说明：save 需要 SaverScope 接收者（Compose 内部 API 形态随版本变化），
    // 它只是「返回 selectedId」一行代码；恢复路径才是风险所在，重点覆盖 restore。

    @Test
    fun `saver 恢复合法 id 原样还原`() {
        val restored = NavTabState.saver(ids).restore("status")!!
        assertEquals("status", restored.selectedId)
        assertEquals(1, restored.selectedIndex)
    }

    @Test
    fun `saver 恢复未知 id 兜底到第一个 Tab（跨版本 Tab 重排防白屏）`() {
        val restored = NavTabState.saver(ids).restore("removed_tab")!!
        assertEquals("networks", restored.selectedId)
    }

    // ---------- 单 Tab 与空集合边界 ----------

    @Test
    fun `单 Tab 状态 selectByIndex 任意值都落在唯一 Tab`() {
        val s = NavTabState(listOf("only"), "only")
        s.selectByIndex(42)
        assertEquals("only", s.selectedId)
        assertEquals(0, s.selectedIndex)
    }
}
