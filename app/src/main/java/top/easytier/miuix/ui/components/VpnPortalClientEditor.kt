package top.easytier.miuix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import top.easytier.miuix.R
import top.easytier.miuix.data.model.VpnPortalClient
import top.easytier.miuix.ui.kit.components.AppTextField
import top.easytier.miuix.ui.kit.components.GhostButton
import top.easytier.miuix.ui.kit.components.MiniButton
import top.easytier.miuix.ui.kit.components.VerticalGap
import top.easytier.miuix.ui.kit.theme.LocalPalette

/**
 * `[[vpn_portal_config.clients]]` 编辑器。
 *
 * 上游已用「具名客户端」取代旧的 `client_cidr` 全局网段，所以这里收集名称与
 * 带前缀长的虚拟 IP（例如 `10.14.14.2/24`）。列表为空时只写 `wireguard_listen`
 * 也是合法的——门户仍会启动，只是需要客户端自行配置。
 */
@Composable
fun VpnPortalClientEditor(
    clients: List<VpnPortalClient>,
    onClientsChange: (List<VpnPortalClient>) -> Unit,
    modifier: Modifier = Modifier,
    errorFor: (Int) -> String? = { null },
) {
    val palette = LocalPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        if (clients.isEmpty()) {
            Text(
                text = stringResource(R.string.config_vpn_portal_clients_empty),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary,
            )
            VerticalGap(10.dp)
        }

        clients.forEachIndexed { index, client ->
            VpnClientRow(
                index = index,
                client = client,
                error = errorFor(index),
                onClientChange = { newClient ->
                    onClientsChange(clients.toMutableList().apply { set(index, newClient) })
                },
                onDelete = {
                    onClientsChange(clients.toMutableList().apply { removeAt(index) })
                },
            )
            VerticalGap(10.dp)
        }

        GhostButton(
            text = stringResource(R.string.config_vpn_portal_client_add),
            onClick = { onClientsChange(clients + VpnPortalClient()) },
            icon = Icons.Rounded.Add,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun VpnClientRow(
    index: Int,
    client: VpnPortalClient,
    error: String?,
    onClientChange: (VpnPortalClient) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.fieldFill, RoundedCornerShape(18.dp))
            .border(1.dp, palette.fieldBorder, RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = client.name.ifEmpty {
                    stringResource(R.string.config_vpn_portal_client, index + 1)
                },
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
            )
            MiniButton(
                text = stringResource(R.string.config_port_forward_delete),
                onClick = onDelete,
                icon = Icons.Rounded.Delete,
                tone = palette.danger,
            )
        }

        VerticalGap(10.dp)

        AppTextField(
            value = client.name,
            onValueChange = { onClientChange(client.copy(name = it)) },
            label = stringResource(R.string.config_vpn_portal_client_name),
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalGap(8.dp)
        AppTextField(
            value = client.virtualIp,
            onValueChange = { onClientChange(client.copy(virtualIp = it)) },
            label = stringResource(R.string.config_vpn_portal_client_ip),
            monospace = true,
            error = error,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
