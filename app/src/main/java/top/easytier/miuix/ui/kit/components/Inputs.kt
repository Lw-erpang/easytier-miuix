package top.easytier.miuix.ui.kit.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.easytier.miuix.ui.kit.theme.LocalPalette

/**
 * 输入框：label 在框上方，玻璃填充底 + 16dp 圆角；
 * 聚焦主色描边，错误时 label / 边框 / 提示同步变红。
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    error: String? = null,
    helper: String? = null,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    monospace: Boolean = false,
    obscure: Boolean = false,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Done,
) {
    val palette = LocalPalette.current
    val colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = palette.fieldFill,
        unfocusedContainerColor = palette.fieldFill,
        disabledContainerColor = palette.fieldFill.copy(alpha = 0.45f),
        errorContainerColor = palette.danger.copy(alpha = 0.06f),
        focusedBorderColor = palette.primary,
        unfocusedBorderColor = palette.fieldBorder,
        disabledBorderColor = palette.fieldBorder.copy(alpha = 0.45f),
        errorBorderColor = palette.danger,
        focusedTextColor = palette.textPrimary,
        unfocusedTextColor = palette.textPrimary,
        disabledTextColor = palette.textTertiary,
        errorTextColor = palette.textPrimary,
        cursorColor = palette.primary,
        focusedPlaceholderColor = palette.textTertiary,
        unfocusedPlaceholderColor = palette.textTertiary,
    )

    val textStyle = MaterialTheme.typography.bodyLarge.merge(
        if (monospace) {
            TextStyle(
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.4.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            TextStyle()
        },
    )

    Column(modifier) {
        if (label.isNotBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (error != null) palette.danger else palette.textSecondary,
                modifier = Modifier.padding(start = 4.dp, bottom = 7.dp),
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = if (placeholder.isEmpty()) null else {
                { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = palette.textTertiary) }
            },
            leadingIcon = if (leadingIcon == null) null else {
                {
                    Icon(
                        leadingIcon,
                        contentDescription = null,
                        tint = palette.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            },
            trailingIcon = trailing,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else minLines,
            maxLines = if (singleLine) 1 else maxLines,
            textStyle = textStyle,
            shape = RoundedCornerShape(16.dp),
            visualTransformation = if (obscure) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(
                capitalization = capitalization,
                keyboardType = keyboardType,
                imeAction = imeAction,
            ),
            colors = colors,
        )
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = palette.danger,
                modifier = Modifier.padding(start = 6.dp, top = 6.dp),
            )
        } else if (helper != null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary,
                modifier = Modifier.padding(start = 6.dp, top = 6.dp),
            )
        }
    }
}
