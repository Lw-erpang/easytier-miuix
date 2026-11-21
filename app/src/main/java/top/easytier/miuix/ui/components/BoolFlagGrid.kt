package top.easytier.miuix.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.easytier.miuix.ui.kit.components.KitSwitch
import top.easytier.miuix.ui.kit.theme.LocalPalette

data class BoolFlag(
    val key: String,
    val label: String,
    val value: Boolean,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BoolFlagGrid(
    flags: List<BoolFlag>,
    onFlagChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        maxItemsInEachRow = 2,
    ) {
        flags.forEach { flag ->
            BoolFlagItem(
                flag = flag,
                onToggle = { onFlagChange(flag.key, it) },
                modifier = Modifier.width(170.dp),
            )
        }
    }
}

@Composable
private fun BoolFlagItem(
    flag: BoolFlag,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = flag.label,
            style = MaterialTheme.typography.bodySmall,
            color = if (flag.value) palette.textPrimary else palette.textSecondary,
            modifier = Modifier.weight(1f),
        )
        KitSwitch(checked = flag.value, onCheckedChange = onToggle)
    }
}
