package top.easytier.miuix.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import top.easytier.miuix.data.model.PortForwardConfig
import top.easytier.miuix.ui.kit.components.AppTextField
import top.easytier.miuix.ui.kit.components.ChoiceChip
import top.easytier.miuix.ui.kit.components.GhostButton
import top.easytier.miuix.ui.kit.components.MiniButton
import top.easytier.miuix.ui.kit.components.VerticalGap
import top.easytier.miuix.ui.kit.theme.LocalPalette

@Composable
fun PortForwardEditor(
    portForwards: List<PortForwardConfig>,
    onPortForwardsChange: (List<PortForwardConfig>) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        portForwards.forEachIndexed { index, pf ->
            PortForwardRow(
                index = index,
                config = pf,
                onConfigChange = { newPf ->
                    onPortForwardsChange(portForwards.toMutableList().apply { set(index, newPf) })
                },
                onDelete = {
                    onPortForwardsChange(portForwards.toMutableList().apply { removeAt(index) })
                },
            )
            VerticalGap(10.dp)
        }

        GhostButton(
            text = stringResource(R.string.config_port_forward_add),
            onClick = {
                onPortForwardsChange(
                    portForwards + PortForwardConfig(proto = "tcp", bindPort = 65535, dstPort = 65535),
                )
            },
            icon = Icons.Rounded.Add,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PortForwardRow(
    index: Int,
    config: PortForwardConfig,
    onConfigChange: (PortForwardConfig) -> Unit,
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
                text = stringResource(R.string.config_port_forward_item, index + 1),
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

        // 协议：TCP / UDP
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.config_port_forward_protocol),
                style = MaterialTheme.typography.labelMedium,
                color = palette.textTertiary,
            )
            Spacer(Modifier.width(10.dp))
            ChoiceChip(
                text = "TCP",
                selected = config.proto == "tcp",
                onClick = { onConfigChange(config.copy(proto = "tcp")) },
            )
            Spacer(Modifier.width(8.dp))
            ChoiceChip(
                text = "UDP",
                selected = config.proto == "udp",
                onClick = { onConfigChange(config.copy(proto = "udp")) },
            )
        }

        VerticalGap(10.dp)

        // 绑定地址
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppTextField(
                value = config.bindIp,
                onValueChange = { onConfigChange(config.copy(bindIp = it)) },
                label = stringResource(R.string.config_port_forward_bind_ip),
                placeholder = "0.0.0.0",
                monospace = true,
                modifier = Modifier.weight(1.4f),
            )
            AppTextField(
                value = config.bindPort.toString(),
                onValueChange = { onConfigChange(config.copy(bindPort = it.toIntOrNull() ?: 0)) },
                label = stringResource(R.string.config_port_forward_bind_port),
                modifier = Modifier.weight(1f),
            )
        }

        VerticalGap(8.dp)

        // 目标地址
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppTextField(
                value = config.dstIp,
                onValueChange = { onConfigChange(config.copy(dstIp = it)) },
                label = stringResource(R.string.config_port_forward_dst_ip),
                placeholder = "127.0.0.1",
                monospace = true,
                modifier = Modifier.weight(1.4f),
            )
            AppTextField(
                value = config.dstPort.toString(),
                onValueChange = { onConfigChange(config.copy(dstPort = it.toIntOrNull() ?: 0)) },
                label = stringResource(R.string.config_port_forward_dst_port),
                modifier = Modifier.weight(1f),
            )
        }
    }
}
