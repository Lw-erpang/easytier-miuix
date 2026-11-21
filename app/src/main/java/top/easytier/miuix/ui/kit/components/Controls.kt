package top.easytier.miuix.ui.kit.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.easytier.miuix.ui.kit.theme.LocalPalette

/** 主按钮：纯色实底（蓝/红/灰禁用），52dp 高，按压 0.97 缩放。每张卡片最多一个。 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    shape: Shape = RoundedCornerShape(18.dp),
) {
    val palette = LocalPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && enabled) 0.97f else 1f, label = "btnScale")

    val bgColor = when {
        !enabled -> palette.textTertiary.copy(alpha = 0.30f)
        danger -> palette.danger
        else -> palette.primary
    }

    Box(
        modifier
            .widthIn(min = 88.dp)
            .height(52.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (enabled) 10.dp else 0.dp, shape, ambientColor = palette.shadow, spotColor = palette.shadow)
            .clip(shape)
            .background(bgColor)
            .clickableNoRipple(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (enabled) palette.onPrimary else palette.textTertiary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) palette.onPrimary else palette.textTertiary,
            )
        }
    }
}

/** 次按钮：fieldFill 底 + 1dp 描边 + 次文字色，52dp 高。 */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    danger: Boolean = false,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
) {
    val palette = LocalPalette.current
    val contentColor = when {
        !enabled -> palette.textTertiary.copy(alpha = 0.6f)
        danger -> palette.danger
        else -> palette.textSecondary
    }

    Box(
        modifier
            .widthIn(min = 88.dp)
            .height(52.dp)
            .clip(shape)
            .background(palette.fieldFill)
            .border(1.dp, if (danger) palette.danger.copy(alpha = 0.35f) else palette.fieldBorder, shape)
            .clickableNoRipple(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge, color = contentColor)
        }
    }
}

/** 小胶囊按钮：卡片内联次级操作（编辑/清空/重选），32dp 高。 */
@Composable
fun MiniButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tone: Color? = null,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    val color = when {
        !enabled -> palette.textTertiary.copy(alpha = 0.65f)
        else -> tone ?: palette.primary
    }

    Row(
        modifier
            .height(32.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (palette.dark) 0.16f else 0.10f))
            .border(1.dp, color.copy(alpha = 0.28f), CircleShape)
            .clickableNoRipple(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
        }
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 圆形图标按钮：返回 / 刷新等，38dp。 */
@Composable
fun IconCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 38.dp,
    tint: Color? = null,
    background: Color? = null,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(background ?: palette.fieldFill)
            .border(1.dp, palette.fieldBorder, CircleShape)
            .clickableNoRipple(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint ?: palette.textSecondary,
            modifier = Modifier.size(size * 0.48f),
        )
    }
}

/** 选择胶囊（单选组：主题模式 / 过滤等）。 */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    val background by animateColorAsState(
        targetValue = if (selected) {
            palette.primary.copy(alpha = if (palette.dark) 0.26f else 0.14f)
        } else {
            palette.fieldFill
        },
        label = "chipBg",
    )
    val contentColor by animateColorAsState(
        targetValue = when {
            !enabled -> palette.textTertiary.copy(alpha = 0.6f)
            selected -> palette.primary
            else -> palette.textSecondary
        },
        label = "chipFg",
    )

    Row(
        modifier
            .heightIn(min = 38.dp)
            .clip(CircleShape)
            .background(background)
            .border(
                1.dp,
                if (selected) palette.primary.copy(alpha = 0.45f) else palette.fieldBorder,
                CircleShape,
            )
            .clickableNoRipple(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 整行选择项（弹窗内的单选列表）：左侧文字 + 选中时右侧对勾，48dp 高。 */
@Composable
fun ChoiceRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val background by animateColorAsState(
        targetValue = if (selected) {
            palette.primary.copy(alpha = if (palette.dark) 0.24f else 0.12f)
        } else {
            palette.fieldFill
        },
        label = "choiceRowBg",
    )
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .border(
                1.dp,
                if (selected) palette.primary.copy(alpha = 0.42f) else palette.fieldBorder,
                RoundedCornerShape(14.dp),
            )
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = if (selected) palette.primary else palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** 主题开关：轨道用主色，未选中轨道用弱底，与 UI-Kit 色板一致。 */
@Composable
fun KitSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = palette.onPrimary,
            checkedTrackColor = palette.primary,
            checkedBorderColor = palette.primary,
            uncheckedThumbColor = palette.textTertiary,
            uncheckedTrackColor = palette.fieldFill,
            uncheckedBorderColor = palette.fieldBorder,
            disabledCheckedTrackColor = palette.primary.copy(alpha = 0.4f),
        ),
    )
}

/** 设置行：标题 + 说明 + 右侧开关。整行可点。 */
@Composable
fun SwitchRow(
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clickableNoRipple(enabled = enabled) { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) palette.textPrimary else palette.textTertiary,
            )
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        KitSwitch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

/** 跳转行：标题 + 说明 + 右侧箭头。整行可点。 */
@Composable
fun SettingRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
            )
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary,
                )
            }
        }
        if (trailing != null) {
            trailing()
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 下拉选择行：左侧标题 + 右侧当前值与箭头，点击弹出菜单。用于表单内的枚举选择。 */
@Composable
fun DropdownRow(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelectedIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickableNoRipple { expanded = true }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = options.getOrElse(selectedIndex) { "" },
                style = MaterialTheme.typography.bodySmall,
                color = palette.primary,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = palette.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (index == selectedIndex) palette.primary else palette.textPrimary,
                        )
                    },
                    onClick = {
                        onSelectedIndexChange(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

/** 可展开行：标题 + 说明 + 右侧箭头（展开时旋转 180°）。 */
@Composable
fun ExpandableRow(
    title: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
) {
    val palette = LocalPalette.current
    Row(
        modifier
            .fillMaxWidth()
            .clickableNoRipple(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
            )
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary,
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.KeyboardArrowDown,
            contentDescription = null,
            tint = palette.textTertiary,
            modifier = Modifier
                .size(20.dp)
                .graphicsLayer { rotationZ = if (expanded) 180f else 0f },
        )
    }
}
