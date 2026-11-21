package top.easytier.miuix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import top.easytier.miuix.R
import top.easytier.miuix.ui.kit.components.AppTextField
import top.easytier.miuix.ui.kit.components.IconCircleButton
import top.easytier.miuix.ui.kit.components.MiniButton
import top.easytier.miuix.ui.kit.components.VerticalGap
import top.easytier.miuix.ui.kit.theme.LocalPalette

// 对齐上游 easytier 57afa29：
// 协议全集 IpScheme = tcp/udp/wg/quic/ws/wss/faketcp（tunnel/mod.rs），
// 端口按基准 11010 + protocol_port_offset 展开（tcp/udp +0，wg/ws +1，quic/wss +2，faketcp +3）
private val LISTENER_PRESETS = listOf(
    "tcp://0.0.0.0:11010",
    "udp://0.0.0.0:11010",
    "wg://0.0.0.0:11011",
    "ws://0.0.0.0:11011",
    "quic://0.0.0.0:11012",
    "wss://0.0.0.0:11012",
    "faketcp://0.0.0.0:11013",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListenerPicker(
    label: String,
    urls: List<String>,
    onUrlsChange: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = palette.textPrimary,
        )

        // 预设协议：一点即加，比下拉少一步
        val available = remember(urls) { LISTENER_PRESETS.filter { it !in urls } }
        if (available.isNotEmpty()) {
            VerticalGap(8.dp)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                available.forEach { preset ->
                    MiniButton(
                        text = preset,
                        icon = Icons.Rounded.Add,
                        onClick = { onUrlsChange(urls + preset) },
                    )
                }
            }
        }

        // 自定义监听地址：上游 #1967 起支持 URL 路径（如 ws://host:11011/path），
        // 固定预设覆盖不到反代场景，所以保留一个自由输入入口。
        var customUrl by remember { mutableStateOf("") }
        VerticalGap(8.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppTextField(
                value = customUrl,
                onValueChange = { customUrl = it.replace("\n", "").replace("\r", "").trim() },
                label = "",
                placeholder = stringResource(R.string.config_listeners_hint),
                monospace = true,
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            IconCircleButton(
                icon = Icons.Rounded.Add,
                onClick = {
                    val url = customUrl.trim()
                    if (url.isNotEmpty() && url !in urls) {
                        onUrlsChange(urls + url)
                        customUrl = ""
                    }
                },
                contentDescription = stringResource(R.string.config_add_listener),
                tint = palette.primary,
                enabled = customUrl.isNotBlank(),
            )
        }

        // 已添加的监听地址
        if (urls.isNotEmpty()) {
            VerticalGap(10.dp)
            urls.forEachIndexed { index, url ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(palette.fieldFill, RoundedCornerShape(14.dp))
                            .border(1.dp, palette.fieldBorder, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                    ) {
                        Text(
                            text = url,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconCircleButton(
                        icon = Icons.Rounded.Close,
                        onClick = { onUrlsChange(urls.toMutableList().apply { removeAt(index) }) },
                        contentDescription = stringResource(R.string.config_port_forward_delete),
                        tint = palette.danger,
                    )
                }
            }
        }
    }
}
