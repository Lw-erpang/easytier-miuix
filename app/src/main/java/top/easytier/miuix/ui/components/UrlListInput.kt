package top.easytier.miuix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.easytier.miuix.R
import top.easytier.miuix.ui.kit.components.AppTextField
import top.easytier.miuix.ui.kit.components.IconCircleButton
import top.easytier.miuix.ui.kit.components.MiniButton
import top.easytier.miuix.ui.kit.components.VerticalGap
import top.easytier.miuix.ui.kit.theme.LocalPalette

/** 字符串列表编辑器（初始节点 / 代理网段 / 出口节点 / 手动路由通用）。 */
@Composable
fun UrlListInput(
    label: String,
    urls: List<String>,
    onUrlsChange: (List<String>) -> Unit,
    hint: String = "",
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
            )
            MiniButton(
                text = stringResource(R.string.common_add),
                onClick = { onUrlsChange(urls + "") },
                icon = Icons.Rounded.Add,
            )
        }

        if (urls.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
        }

        urls.forEachIndexed { index, url ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppTextField(
                    value = url,
                    onValueChange = { newUrl ->
                        val cleaned = newUrl.replace("\n", "").replace("\r", "").trim()
                        onUrlsChange(urls.toMutableList().apply { set(index, cleaned) })
                    },
                    label = "",
                    placeholder = hint,
                    monospace = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconCircleButton(
                    icon = Icons.Rounded.Close,
                    onClick = { onUrlsChange(urls.toMutableList().apply { removeAt(index) }) },
                    contentDescription = stringResource(R.string.config_port_forward_delete),
                    tint = palette.danger,
                )
            }
        }
        VerticalGap(2.dp)
    }
}
