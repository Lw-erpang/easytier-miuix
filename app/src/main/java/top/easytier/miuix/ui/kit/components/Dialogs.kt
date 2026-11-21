package top.easytier.miuix.ui.kit.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.easytier.miuix.ui.kit.theme.LocalPalette

/**
 * 主题化对话框：玻璃强色底 + 34dp 圆角，imePadding 保证键盘弹出时不遮挡输入框。
 * 不要在页面里直接用裸 AlertDialog。
 */
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    text: @Composable ColumnScope.() -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.imePadding(),
        containerColor = palette.glassStrongTop,
        tonalElevation = 0.dp,
        titleContentColor = palette.textPrimary,
        textContentColor = palette.textSecondary,
        title = { Text(title, fontWeight = FontWeight.SemiBold) },
        // 底部留白：Material3 1.4 的动作按钮紧贴正文，避免最后一行被按钮压住
        text = { Column(Modifier.padding(bottom = 8.dp), content = text) },
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    )
}

/** 内联提示条（错误 / 信息 / 成功），可恢复错误不要用 Toast 表达。 */
@Composable
fun InlineNotice(
    text: String,
    modifier: Modifier = Modifier,
    tone: Color? = null,
) {
    val palette = LocalPalette.current
    val color = tone ?: palette.danger
    Row(
        modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(color.copy(alpha = if (palette.dark) 0.14f else 0.08f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(9.dp))
        Text(text = text, color = color, fontSize = 12.sp, lineHeight = 17.sp)
    }
}
