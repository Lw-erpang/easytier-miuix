package top.easytier.miuix.ui.screens.networks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import top.easytier.miuix.R
import top.easytier.miuix.data.model.ImportError
import top.easytier.miuix.data.model.NetworkInstance
import top.easytier.miuix.ui.kit.components.AppAlertDialog
import top.easytier.miuix.ui.kit.components.AppBadge
import top.easytier.miuix.ui.kit.components.AppCard
import top.easytier.miuix.ui.kit.components.GhostButton
import top.easytier.miuix.ui.kit.components.PageHeader
import top.easytier.miuix.ui.kit.components.PrimaryButton
import top.easytier.miuix.ui.kit.components.VerticalGap
import top.easytier.miuix.ui.kit.components.clickableNoRipple
import top.easytier.miuix.ui.kit.theme.LocalPalette
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState

@Composable
fun NetworkListScreen(
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
    onEditNetwork: (String) -> Unit = {},
    onCreateNetwork: () -> Unit = {},
    viewModel: NetworkListViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val instances by viewModel.instances.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val selectedId by viewModel.selectedInstanceId.collectAsState()
    val runningId by viewModel.runningInstanceId.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshConfigs() }

    // 播放卡片展示「正在运行的那个」；没有运行中的网络时展示当前选中项
    val runningInstance = instances.firstOrNull { it.instanceId == runningId }
    val selectedInstance = instances.firstOrNull { it.instanceId == selectedId }
    val controlInstance = runningInstance ?: selectedInstance

    // 系统文件选择器：配置文件不限扩展名（JSON/TOML 都可能被识别成 octet-stream），
    // 格式问题交给解析层给出可读的失败提示。
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importConfigs(uri)
    }
    val onImportConfig: () -> Unit = { importLauncher.launch(arrayOf("*/*")) }

    var isRefreshing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp),
    ) {
        VerticalGap(18.dp)

        PageHeader(
            title = stringResource(R.string.nav_networks),
            eyebrow = stringResource(R.string.networks_eyebrow),
            subtitle = stringResource(R.string.networks_subtitle),
            badge = if (instances.isNotEmpty()) {
                stringResource(R.string.networks_count, instances.size)
            } else {
                null
            },
        )

        VerticalGap(16.dp)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryButton(
                text = stringResource(R.string.network_create),
                onClick = onCreateNetwork,
                icon = Icons.Rounded.Add,
                modifier = Modifier.weight(1.5f),
            )
            GhostButton(
                text = stringResource(R.string.network_import),
                onClick = onImportConfig,
                icon = Icons.Rounded.FileOpen,
                modifier = Modifier.weight(1f),
            )
        }

        VerticalGap(14.dp)

        Box(modifier = Modifier.weight(1f)) {
            if (instances.isEmpty()) {
                EmptyNetworkCard(
                    onCreateNetwork = onCreateNetwork,
                    onImportConfig = onImportConfig,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                PullToRefresh(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        viewModel.refreshConfigs { isRefreshing = false }
                    },
                    pullToRefreshState = rememberPullToRefreshState(),
                ) {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 12.dp),
                    ) {
                        items(instances, key = { it.instanceId }) { instance ->
                            NetworkInstanceCard(
                                instance = instance,
                                selected = instance.instanceId == selectedId,
                                onSelect = { viewModel.selectNetwork(instance.instanceId) },
                                onEdit = { onEditNetwork(instance.instanceId) },
                            )
                        }
                    }
                }
            }
        }

        // 底部控制卡片：状态 + 设备名称 + 虚拟 IP，播放按钮在卡片内水平居中
        if (instances.isNotEmpty()) {
            val running = runningId != null
            val nodeInfo = controlInstance?.detail?.myNodeInfo
            NetworkControlCard(
                running = running,
                deviceName = nodeInfo?.hostname.orEmpty().ifBlank { "—" },
                address = nodeInfo?.virtualIpv4?.toString()
                    ?.takeIf { it.isNotBlank() && it != "0.0.0.0/0" }
                    .orEmpty(),
                onToggle = { viewModel.toggleSelectedNetwork() },
            )
            Spacer(Modifier.height(contentBottomPadding + 12.dp))
        }
    }

    // 导入结果：成功与失败共用同一个提示框，失败标题用「错误」区分
    importState.result?.let { result ->
        AppAlertDialog(
            onDismissRequest = { viewModel.consumeImportResult() },
            title = if (result is ImportResult.Success) {
                stringResource(R.string.network_import)
            } else {
                stringResource(R.string.error)
            },
            text = {
                Text(
                    text = when (result) {
                        is ImportResult.Success -> stringResource(
                            R.string.import_success_msg,
                            result.count,
                            result.names.joinToString("、"),
                        )
                        is ImportResult.Failure -> importFailureText(result)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(R.string.ok),
                    onClick = { viewModel.consumeImportResult() },
                )
            },
        )
    }
}

/** 空态卡片：图标 + 说明 + 与顶部一致的两个入口 */
@Composable
private fun EmptyNetworkCard(
    onCreateNetwork: () -> Unit,
    onImportConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    AppCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(22.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.primary.copy(alpha = if (palette.dark) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Lan, contentDescription = null, tint = palette.primary, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.networks_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.network_empty),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * 底部控制卡片：播放按钮放大并**水平居中**，「运行中」状态文字放在按钮下方，
 * 运行时才显示设备名称与虚拟 IP（两者同字号，IP 用等宽字体）。
 */
@Composable
private fun NetworkControlCard(
    running: Boolean,
    deviceName: String,
    address: String,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    AppCard(
        modifier = modifier.fillMaxWidth(),
        strong = true,
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 18.dp),
    ) {
        // 放大的播放/停止按钮：卡片中轴
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PlayStopButton(running = running, onClick = onToggle, size = 92.dp)
        }

        Spacer(Modifier.height(10.dp))

        // 状态行：圆点 + 运行中/已停止（按钮正下方，水平居中）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (running) palette.success else palette.textTertiary.copy(alpha = 0.45f),
                    ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(if (running) R.string.network_running else R.string.network_stopped),
                style = MaterialTheme.typography.labelMedium,
                color = if (running) palette.success else palette.textTertiary,
            )
        }

        // 设备名称 + 虚拟 IP：仅在运行时展示（未运行时还没有这些信息）
        if (running) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.network_device_name_value, deviceName),
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (address.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 圆形播放/停止按钮：未运行时主色播放，运行中变红显示停止。 */
@Composable
private fun PlayStopButton(
    running: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val palette = LocalPalette.current
    val containerColor = if (running) palette.danger else palette.primary
    val contentColor = palette.onPrimary
    Box(
        modifier = modifier
            .size(size)
            .shadow(size / 7, CircleShape, ambientColor = containerColor, spotColor = containerColor)
            .clip(CircleShape)
            .background(containerColor)
            .clickableNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (running) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = stringResource(
                if (running) R.string.network_stop_action else R.string.network_start_action,
            ),
            tint = contentColor,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

/** 失败原因 → 本地化文案；解析器只回传错误类型，技术细节追加在末尾便于排查 */
@Composable
private fun importFailureText(failure: ImportResult.Failure): String {
    val base = stringResource(
        when (failure.error) {
            ImportError.Empty -> R.string.import_error_empty
            ImportError.Unsupported -> R.string.import_error_unsupported
            ImportError.JsonSyntax -> R.string.import_error_json
            ImportError.TomlSyntax -> R.string.import_error_toml
            ImportError.NoConfig -> R.string.import_error_no_config
            ImportError.ReadFailed -> R.string.import_error_read
            ImportError.TooLarge -> R.string.import_error_too_large
        },
    )
    return if (failure.detail.isBlank()) base else "$base\n\n${failure.detail}"
}

@Composable
private fun NetworkInstanceCard(
    instance: NetworkInstance,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
) {
    val palette = LocalPalette.current
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, top = 14.dp, end = 12.dp, bottom = 14.dp),
        onClick = onSelect,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                // 名称 + 名称后的小编辑图标：编辑入口收敛到这里，删除在编辑页内
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = instance.name.ifEmpty { instance.instanceId.take(8) },
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .clickableNoRipple(onClick = onEdit),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.network_edit),
                            tint = palette.textTertiary,
                            modifier = Modifier.size(17.dp),
                        )
                    }
                }
                if (instance.running || selected) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (instance.running) {
                            AppBadge(text = stringResource(R.string.network_running), tint = palette.success)
                        }
                        if (selected) {
                            AppBadge(text = stringResource(R.string.network_selected), tint = palette.primary)
                        }
                    }
                }
                if (instance.errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = instance.errorMsg,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.danger,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = stringResource(
                    if (selected) R.string.network_selected else R.string.network_select,
                ),
                tint = if (selected) palette.primary else palette.textTertiary.copy(alpha = 0.5f),
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(22.dp),
            )
        }
    }
}
