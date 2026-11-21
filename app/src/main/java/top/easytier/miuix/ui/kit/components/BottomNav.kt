package top.easytier.miuix.ui.kit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.easytier.miuix.ui.kit.navigation.KitTab
import top.easytier.miuix.ui.kit.theme.LocalPalette

/**
 * 徽标小圆点：包在图标外层，对齐到图标右上角，不占额外布局空间。
 * 无徽标时也保持同样的盒子结构，避免出现/消失导致图标跳动。
 */
@Composable
private fun BadgedIcon(
    visible: Boolean,
    dotColor: Color,
    icon: ImageVector,
    contentDescription: String?,
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(imageVector = icon, contentDescription = contentDescription)
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(8.dp)
                .then(
                    if (visible) {
                        Modifier
                            .clip(CircleShape)
                            .background(dotColor)
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

/**
 * # Kit 底部导航栏（普通形态）
 *
 * UI-Kit 的统一底部导航：
 * - 颜色全部来自 [LocalPalette]（玻璃底色、主色、文字色），深浅色主题自动适配；
 * - 徽标内建（[KitTab.showBadge]），调用方不再手写「index == 1」这类魔法判断；
 * - 选中态由调用方持有的 NavTabState 下发，本组件只读渲染（单向数据流）；
 * - 点击只回调 [onSelect]，不内部改状态——保证两种形态（普通/悬浮）行为一致。
 *
 * 悬浮形态请继续使用 `ui/components/FloatingBottomBar`（依赖 miuix-blur 液态玻璃，
 * 不进 kit），两者共用同一份 [KitTab] + NavTabState。
 *
 * @param containerColor 栏底色，默认玻璃色；传 [Color.Transparent] 可配合外层模糊
 * @param badgeColor 徽标圆点颜色，默认主色
 */
@Composable
fun KitNavigationBar(
    tabs: List<KitTab>,
    selectedId: String,
    onSelect: (KitTab) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    badgeColor: Color? = null,
) {
    val palette = LocalPalette.current
    val barColor = containerColor ?: palette.glassStrongTop
    val dotColor = badgeColor ?: palette.primary

    NavigationBar(
        modifier = modifier,
        containerColor = barColor,
        tonalElevation = 0.dp,
    ) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.id == selectedId,
                onClick = { onSelect(tab) },
                icon = {
                    BadgedIcon(
                        visible = tab.showBadge,
                        dotColor = dotColor,
                        icon = tab.icon,
                        contentDescription = tab.label,
                    )
                },
                label = { Text(text = tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.primary,
                    selectedTextColor = palette.primary,
                    unselectedIconColor = palette.textTertiary,
                    unselectedTextColor = palette.textTertiary,
                    indicatorColor = palette.primary.copy(alpha = 0.14f),
                ),
            )
        }
    }
}
