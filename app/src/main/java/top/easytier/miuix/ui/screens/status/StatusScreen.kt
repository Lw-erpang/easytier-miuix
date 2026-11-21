package top.easytier.miuix.ui.screens.status

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import org.json.JSONObject
import top.easytier.miuix.R
import top.easytier.miuix.data.model.EventLevel
import top.easytier.miuix.data.model.NatType
import top.easytier.miuix.data.model.PeerClassifier
import top.easytier.miuix.data.model.PeerRoutePair
import top.easytier.miuix.ui.components.TrafficChart
import top.easytier.miuix.ui.kit.components.AppAlertDialog
import top.easytier.miuix.ui.kit.components.AppBadge
import top.easytier.miuix.ui.kit.components.AppCard
import top.easytier.miuix.ui.kit.components.AppTextField
import top.easytier.miuix.ui.kit.components.ChoiceChip
import top.easytier.miuix.ui.kit.components.GhostButton
import top.easytier.miuix.ui.kit.components.HairLine
import top.easytier.miuix.ui.kit.components.InfoRow
import top.easytier.miuix.ui.kit.components.MiniButton
import top.easytier.miuix.ui.kit.components.PageHeader
import top.easytier.miuix.ui.kit.components.PrimaryButton
import top.easytier.miuix.ui.kit.components.SectionTitle
import top.easytier.miuix.ui.kit.components.VerticalGap
import top.easytier.miuix.ui.kit.components.clickableNoRipple
import top.easytier.miuix.ui.kit.theme.LocalPalette

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
    onStartNetwork: () -> Unit = {},
    viewModel: StatusViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val instance by viewModel.currentInstance.collectAsState()
    val txRate by viewModel.txRate.collectAsState()
    val rxRate by viewModel.rxRate.collectAsState()
    val txHistory by viewModel.txHistory.collectAsState()
    val rxHistory by viewModel.rxHistory.collectAsState()

    // 事件 / 对端分区切换；事件过滤状态提升到页面级，切 Tab 不丢失
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var showAllEvents by rememberSaveable { mutableStateOf(false) }
    var warnOnly by rememberSaveable { mutableStateOf(false) }
    var clearedAt by rememberSaveable { mutableLongStateOf(0L) }
    // 本机信息展开状态：提升到页面级，切 Tab 回来不丢失
    var showNodeDetails by rememberSaveable { mutableStateOf(false) }

    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    fun copyToClipboard(value: String) {
        clipboard.setText(AnnotatedString(value))
        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
    }

    // 本机主机名重命名
    val renameResult by viewModel.renameResult.collectAsState()
    var renaming by remember { mutableStateOf(false) }
    var hostnameDraft by remember { mutableStateOf("") }

    if (instance == null || instance?.running != true) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 22.dp),
        ) {
            VerticalGap(18.dp)
            PageHeader(
                title = stringResource(R.string.nav_status),
                eyebrow = stringResource(R.string.status_eyebrow),
                subtitle = stringResource(R.string.status_empty_hint),
            )
            Spacer(Modifier.weight(1f))
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(22.dp),
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.primary.copy(alpha = if (palette.dark) 0.22f else 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Lan,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.status_no_running),
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton(
                        text = stringResource(R.string.status_empty_cta),
                        onClick = onStartNetwork,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.weight(1f))
        }
        return
    }

    val detail = instance?.detail
    val nodeInfo = detail?.myNodeInfo
    val peerPairs = viewModel.getPeerRoutePairs()
    val parsedEvents = detail?.parsedEvents ?: emptyList()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 22.dp),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = contentBottomPadding + 24.dp),
    ) {
        item { VerticalGap(18.dp) }

        item {
            PageHeader(
                title = stringResource(R.string.nav_status),
                eyebrow = instance?.name.orEmpty().ifBlank { stringResource(R.string.status_eyebrow) },
                subtitle = stringResource(
                    R.string.status_subtitle,
                    peerPairs.size,
                    peerPairs.count { it.route.isRelay },
                ),
                badge = stringResource(R.string.network_running),
                badgeTint = palette.success,
            )
        }

        // ---------- 本机信息 ----------
        item {
            AppCard(
                modifier = Modifier.fillMaxWidth(),
                strong = true,
                contentPadding = PaddingValues(18.dp),
            ) {
                SectionTitle(
                    title = stringResource(R.string.status_node_info),
                    icon = Icons.Rounded.Info,
                    action = {
                        if (nodeInfo != null && nodeInfo.hostname.isNotBlank()) {
                            AppBadge(text = nodeInfo.hostname, tint = palette.primary)
                        }
                    },
                )

                VerticalGap(14.dp)

                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.status_tx_rate),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textTertiary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(txRate, style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.status_rx_rate),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textTertiary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(rxRate, style = MaterialTheme.typography.titleLarge, color = palette.textPrimary)
                    }
                }

                VerticalGap(12.dp)

                TrafficChart(
                    txHistory = txHistory,
                    rxHistory = rxHistory,
                    txColor = palette.primary,
                    rxColor = palette.success,
                )

                VerticalGap(8.dp)

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LegendDot(color = palette.primary)
                    Text(
                        text = stringResource(R.string.peer_tx),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary,
                    )
                    Spacer(Modifier.width(12.dp))
                    LegendDot(color = palette.success)
                    Text(
                        text = stringResource(R.string.peer_rx),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary,
                    )
                    Spacer(Modifier.weight(1f))
                    val peak = (txHistory + rxHistory).maxOrNull()
                    Text(
                        text = stringResource(R.string.chart_peak, formatBytes(peak ?: 0) + "/s"),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary,
                    )
                }

                if (nodeInfo != null) {
                    VerticalGap(12.dp)
                    HairLine()
                    VerticalGap(10.dp)
                    NodeInfoRows(
                        nodeInfo = nodeInfo,
                        expanded = showNodeDetails,
                        onToggleExpanded = { showNodeDetails = !showNodeDetails },
                        onRenameHostname = {
                            hostnameDraft = nodeInfo.hostname
                            renaming = true
                        },
                        onLongPressCopy = { copyToClipboard(it) },
                    )
                }
            }
        }

        // ---------- 事件 / 对端切换 ----------
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(
                    text = "${stringResource(R.string.status_tab_peers)} (${peerPairs.size})",
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    modifier = Modifier.weight(1f),
                )
                ChoiceChip(
                    text = "${stringResource(R.string.status_events)} (${parsedEvents.size})",
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item {
            if (selectedSection == 0) {
                PeersContent(
                    peerPairs = peerPairs,
                    localIpv4 = nodeInfo?.virtualIpv4,
                    onLongPressCopy = { copyToClipboard(it) },
                )
            } else {
                EventsCard(
                    events = parsedEvents,
                    showAllEvents = showAllEvents,
                    warnOnly = warnOnly,
                    clearedAt = clearedAt,
                    onShowAllEventsChange = { showAllEvents = it },
                    onWarnOnlyChange = { warnOnly = it },
                    onClear = {
                        clearedAt = System.currentTimeMillis()
                        showAllEvents = false
                    },
                    onCopy = { copyToClipboard(it) },
                )
            }
        }
    }

    // ---------- 本机主机名重命名 ----------
    if (renaming) {
        AppAlertDialog(
            onDismissRequest = { renaming = false },
            title = stringResource(R.string.rename_hostname_title),
            text = {
                AppTextField(
                    value = hostnameDraft,
                    onValueChange = { hostnameDraft = it },
                    label = stringResource(R.string.status_hostname),
                    helper = stringResource(R.string.rename_hostname_hint),
                    singleLine = true,
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(R.string.save),
                    onClick = {
                        renaming = false
                        viewModel.renameHostname(hostnameDraft)
                    },
                )
            },
            dismissButton = {
                GhostButton(
                    text = stringResource(R.string.cancel),
                    onClick = { renaming = false },
                )
            },
        )
    }

    renameResult?.let { result ->
        AppAlertDialog(
            onDismissRequest = { viewModel.consumeRenameResult() },
            title = stringResource(R.string.rename_hostname_title),
            text = {
                Text(
                    text = when (result.error) {
                        RenameError.Empty -> stringResource(R.string.rename_hostname_empty)
                        RenameError.Missing -> stringResource(R.string.rename_hostname_missing)
                        null -> stringResource(
                            if (result.restarted) R.string.rename_hostname_restarted
                            else R.string.rename_hostname_saved,
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary,
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(R.string.ok),
                    onClick = { viewModel.consumeRenameResult() },
                )
            },
        )
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        Modifier
            .padding(end = 5.dp)
            .size(7.dp)
            .clip(CircleShape)
            .background(color),
    )
}

/**
 * 本机信息：UI-Kit 规范里「标签: 值」一律走纯文字信息行（不用徽章堆叠）。
 * 底部「显示更多 / 重命名」两个操作各占一半宽度，共同铺满整行。
 */
@Composable
private fun NodeInfoRows(
    nodeInfo: top.easytier.miuix.data.model.NodeInfo,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRenameHostname: () -> Unit,
    onLongPressCopy: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        CopyableInfoRow(stringResource(R.string.status_peer_id), nodeInfo.peerId.toString(), onLongPressCopy)
        CopyableInfoRow(stringResource(R.string.status_virtual_ip), nodeInfo.virtualIpv4.toString(), onLongPressCopy)
        CopyableInfoRow(stringResource(R.string.status_hostname), nodeInfo.hostname, onLongPressCopy)
        CopyableInfoRow(stringResource(R.string.status_version), nodeInfo.version, onLongPressCopy)

        AnimatedVisibility(visible = expanded) {
            Column {
                CopyableInfoRow(stringResource(R.string.status_public_ip), nodeInfo.publicIpv4.toString(), onLongPressCopy)
                nodeInfo.interfaceIpv4s.firstOrNull()?.let {
                    CopyableInfoRow(stringResource(R.string.status_local_ip), it.toString(), onLongPressCopy)
                }
                InfoRow(stringResource(R.string.status_nat_type), natTypeName(nodeInfo.stunInfo.udpNatType))
                nodeInfo.publicIpv6?.let {
                    CopyableInfoRow(stringResource(R.string.status_public_ipv6), it.toString(), onLongPressCopy)
                }
                nodeInfo.interfaceIpv6s.firstOrNull()?.let {
                    CopyableInfoRow(stringResource(R.string.status_local_ipv6), it.toString(), onLongPressCopy)
                }
            }
        }

        VerticalGap(10.dp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GhostButton(
                text = if (expanded) {
                    stringResource(R.string.status_show_less)
                } else {
                    stringResource(R.string.status_show_more)
                },
                onClick = onToggleExpanded,
                modifier = Modifier.weight(1f),
            )
            GhostButton(
                text = stringResource(R.string.status_rename_hostname),
                onClick = onRenameHostname,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** 纯文字信息行 + 长按复制 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CopyableInfoRow(label: String, value: String, onLongPressCopy: (String) -> Unit) {
    val copyModifier = if (value.isNotBlank()) {
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onLongClick = { onLongPressCopy(value) },
            onClick = {},
        )
    } else {
        Modifier
    }
    Box(Modifier.fillMaxWidth().then(copyModifier)) {
        InfoRow(label = label, value = value)
    }
}

/** 对端分区：网络设备在上、公共节点在下，分组依据见 [PeerClassifier] */
@Composable
private fun PeersContent(
    peerPairs: List<PeerRoutePair>,
    localIpv4: top.easytier.miuix.data.model.Ipv4Inet?,
    onLongPressCopy: (String) -> Unit,
) {
    val palette = LocalPalette.current
    // 同虚拟网段的对端一律算「网络设备」，避免链路在直连/中继之间切换时来回跳组
    val (devices, publicNodes) = PeerClassifier.split(peerPairs, localIpv4)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (peerPairs.isEmpty()) {
            AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(24.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.status_no_peers),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textTertiary,
                    )
                }
            }
        } else {
            if (devices.isNotEmpty()) {
                PeerSectionHeader(stringResource(R.string.status_section_devices), devices.size)
                devices.forEach { pair ->
                    PeerCard(pair, onLongPressCopy = onLongPressCopy)
                }
            }
            if (publicNodes.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                PeerSectionHeader(stringResource(R.string.status_section_public), publicNodes.size)
                publicNodes.forEach { pair ->
                    PeerCard(pair, onLongPressCopy = onLongPressCopy)
                }
            }
        }
    }
}

/** 分组标题：细字 + 数量 + 一条淡分隔线 */
@Composable
private fun PeerSectionHeader(title: String, count: Int) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$title · $count",
            style = MaterialTheme.typography.labelMedium,
            color = palette.textTertiary,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(palette.divider),
        )
    }
}

/** 直连 / 中继 小标签 */
@Composable
private fun RouteModeBadge(relay: Boolean) {
    val palette = LocalPalette.current
    AppBadge(
        text = stringResource(if (relay) R.string.peer_relay else R.string.peer_p2p),
        tint = if (relay) palette.accent else palette.success,
    )
}

@Composable
private fun latencyColor(latencyUs: Long): Color {
    val palette = LocalPalette.current
    return when {
        latencyUs <= 0 -> palette.textTertiary
        latencyUs < 10_000 -> palette.success
        latencyUs < 50_000 -> palette.accent
        else -> palette.danger
    }
}

@Composable
private fun lossColor(rate: Float): Color {
    val palette = LocalPalette.current
    return when {
        rate <= 0f -> palette.textTertiary
        rate < 0.01f -> palette.success
        rate < 0.05f -> palette.accent
        else -> palette.danger
    }
}

@Composable
private fun PeerCard(pair: PeerRoutePair, onLongPressCopy: (String) -> Unit) {
    val palette = LocalPalette.current
    val route = pair.route
    val peer = pair.peer
    // 上游会用 default_conn_id 指定主连接；多路径（UDP + 中继）并存时
    // 直接取 firstOrNull 可能拿到非主连接，导致延迟/丢包/协议都不是用户以为的那条。
    val conn = peer?.primaryConn
    val stats = conn?.stats
    var expanded by remember { mutableStateOf(false) }
    // 中继对端常常没有 conn stats，此时退回端到端的 path_latency，避免永远显示 "- ms"
    val latencyUs = stats?.latencyUs?.takeIf { it > 0 } ?: route.pathLatencyUs
    val lossRate: Float? = if (stats != null) conn?.lossRate else null
    val dotColor = if (latencyUs > 0) latencyColor(latencyUs) else palette.textTertiary

    AppCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        onClick = { expanded = !expanded },
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .padding(end = 10.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = route.hostname.ifEmpty { route.ipv4Addr ?: "N/A" },
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = route.ipv4Addr.orEmpty(),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = palette.textTertiary,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = conn?.tunnel?.tunnelType?.uppercase() ?: "-",
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.primary,
                )
                Text(
                    text = if (latencyUs > 0) "%.1f ms".format(latencyUs / 1000.0) else "- ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (latencyUs > 0) latencyColor(latencyUs) else palette.textTertiary,
                )
            }
        }

        VerticalGap(10.dp)

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            StatColumn(stringResource(R.string.peer_tx), formatBytes(stats?.txBytes ?: 0), Modifier.weight(1f))
            StatColumn(stringResource(R.string.peer_rx), formatBytes(stats?.rxBytes ?: 0), Modifier.weight(1f))
            StatColumn(
                stringResource(R.string.status_loss),
                lossRate?.let { "%.1f%%".format(it * 100) } ?: "-",
                Modifier.weight(1f),
                valueColor = lossRate?.takeIf { it > 0f }?.let { lossColor(it) },
            )
            Spacer(Modifier.width(6.dp))
            RouteModeBadge(relay = pair.route.isRelay)
        }

        AnimatedVisibility(visible = expanded) {
            Column {
                VerticalGap(12.dp)
                HairLine()
                VerticalGap(8.dp)
                InfoRow(stringResource(R.string.peer_version), route.version.ifEmpty { "-" })
                InfoRow(stringResource(R.string.peer_id_label), peer?.peerId?.toString() ?: "-")
                conn?.tunnel?.localAddr?.url?.takeIf { it.isNotEmpty() }?.let {
                    InfoRow(stringResource(R.string.peer_tunnel_local), it)
                }
                conn?.tunnel?.remoteAddr?.url?.takeIf { it.isNotEmpty() }?.let {
                    InfoRow(stringResource(R.string.peer_tunnel_remote), it)
                }
                if (route.proxyCidrs.isNotEmpty()) {
                    VerticalGap(6.dp)
                    Text(
                        stringResource(R.string.status_route),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textTertiary,
                    )
                    route.proxyCidrs.forEach { cidr ->
                        Text(
                            cidr,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = palette.textPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String, modifier: Modifier = Modifier, valueColor: Color? = null) {
    val palette = LocalPalette.current
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = palette.textTertiary)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = valueColor ?: palette.textPrimary,
        )
    }
}

/** 事件卡片：过滤 + 清空 + 列表（点击行查看原始 JSON） */
@Composable
private fun EventsCard(
    events: List<top.easytier.miuix.data.model.EventInfo>,
    showAllEvents: Boolean,
    warnOnly: Boolean,
    clearedAt: Long,
    onShowAllEventsChange: (Boolean) -> Unit,
    onWarnOnlyChange: (Boolean) -> Unit,
    onClear: () -> Unit,
    onCopy: (String) -> Unit,
) {
    val palette = LocalPalette.current
    // timestamp 为 0 代表时间解析失败：这类事件不能被「清空」永久吞掉，
    // 否则列表会进入无法回退的空状态（连清空按钮都会一起消失）。
    val visibleEvents = events.asSequence()
        .filter { it.timestamp == 0L || it.timestamp >= clearedAt }
        .filter { !warnOnly || detectEventSeverity(it) != EventLevel.INFO }
        .toList()
    val displayEvents = visibleEvents.take(if (showAllEvents) 20 else 5)

    AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ChoiceChip(
                text = stringResource(R.string.events_filter_all),
                selected = !warnOnly,
                onClick = { onWarnOnlyChange(false) },
            )
            ChoiceChip(
                text = stringResource(R.string.events_filter_warnings),
                selected = warnOnly,
                onClick = { onWarnOnlyChange(true) },
            )
            Spacer(Modifier.weight(1f))
            if (visibleEvents.size > 5) {
                MiniButton(
                    text = if (showAllEvents) stringResource(R.string.status_show_less)
                    else stringResource(R.string.status_show_count, 20),
                    onClick = { onShowAllEventsChange(!showAllEvents) },
                )
            }
            if (visibleEvents.isNotEmpty()) {
                MiniButton(
                    text = stringResource(R.string.events_clear),
                    onClick = onClear,
                    tone = palette.textSecondary,
                )
            }
        }

        VerticalGap(8.dp)
        HairLine()
        VerticalGap(4.dp)

        if (displayEvents.isEmpty()) {
            Text(
                text = stringResource(R.string.events_cleared_hint),
                style = MaterialTheme.typography.bodySmall,
                color = palette.textTertiary,
                modifier = Modifier.padding(vertical = 10.dp),
            )
        } else {
            displayEvents.forEach { event ->
                EventLogItem(event, onCopy)
            }
        }
    }
}

/** 事件级别由解析层从 GlobalCtxEvent 变体名推导（上游从不序列化 level）。 */
private fun detectEventSeverity(event: top.easytier.miuix.data.model.EventInfo): EventLevel = event.level

/** 常见事件的用户可读标题，未映射的事件回退为原始类型名 */
private fun eventTitleRes(eventType: String): Int? = when (eventType) {
    "PeerConnAdded" -> R.string.event_peer_conn_added
    "PeerConnRemoved" -> R.string.event_peer_conn_removed
    "ConnAdded" -> R.string.event_conn_added
    "ConnRemoved" -> R.string.event_conn_removed
    "PeerAdded" -> R.string.event_peer_added
    "PeerRemoved" -> R.string.event_peer_removed
    "ListenerAdded" -> R.string.event_listener_added
    "ListenerRemoved" -> R.string.event_listener_removed
    else -> null
}

/** 摘要行优先展示人类可读字段；conn_id/my_peer_id 等技术标识不出现在摘要里 */
private val eventPreferredSummaryFields =
    listOf("remote_addr", "local_addr", "url", "error", "reason", "msg", "message", "name", "cost")

@Composable
private fun EventLogItem(event: top.easytier.miuix.data.model.EventInfo, onCopy: (String) -> Unit) {
    val palette = LocalPalette.current
    val severity = detectEventSeverity(event)
    val severityColor = when (severity) {
        EventLevel.ERROR -> palette.danger
        EventLevel.WARN -> palette.accent
        EventLevel.INFO -> palette.primary
    }
    val eventType = event.type
    val extraFields = remember(event.raw, eventType) {
        try {
            val obj = JSONObject(unescapeJson(event.raw))
            val metaKeys = setOf("time", "timestamp", "ts", "event")
            val detailObj = obj.optJSONObject("event")?.optJSONObject(eventType)
                ?: obj.optJSONObject(eventType)
            val pairs = mutableListOf<Pair<String, String>>()
            if (detailObj != null) {
                detailObj.keys().forEach { key ->
                    val value = unescapeJson(detailObj.optString(key, detailObj.opt(key)?.toString() ?: ""))
                    if (value.isNotBlank() && value != "null") pairs.add(key to value)
                }
            } else {
                obj.keys().forEach { key ->
                    if (key !in metaKeys && key != eventType) {
                        val value = unescapeJson(obj.optString(key, obj.opt(key)?.toString() ?: ""))
                        if (value.isNotBlank() && value != "null") pairs.add(key to value)
                    }
                }
            }
            pairs
        } catch (_: Exception) {
            emptyList<Pair<String, String>>()
        }
    }
    var showJson by remember { mutableStateOf(false) }
    var capturedJson by remember { mutableStateOf("") }
    var capturedTitle by remember { mutableStateOf("") }
    val formattedJson = remember(event.raw) {
        val cleaned = unescapeJson(event.raw)
        try {
            JSONObject(cleaned).toString(2)
        } catch (_: Exception) {
            cleaned
        }
    }
    val timeStr = if (event.timestamp > 0) {
        java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date(event.timestamp))
    } else ""
    val inlineSummary = extraFields
        .firstOrNull { it.first in eventPreferredSummaryFields }
        ?.let { "${it.first}: ${it.second}" }
        ?: ""
    val titleText = eventTitleRes(eventType)?.let { stringResource(it) } ?: eventType

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickableNoRipple {
                capturedJson = formattedJson
                capturedTitle = eventType
                showJson = true
            }
            .padding(vertical = 9.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(severityColor),
            )
            if (severity != EventLevel.INFO) {
                AppBadge(text = severity.name, tint = severityColor)
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (timeStr.isNotEmpty()) {
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = palette.textTertiary,
                )
            }
        }
        if (inlineSummary.isNotEmpty()) {
            Text(
                text = inlineSummary,
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 15.dp),
            )
        }
    }

    if (showJson) {
        AppAlertDialog(
            onDismissRequest = { showJson = false },
            title = capturedTitle.ifEmpty { stringResource(R.string.event_detail_title) },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = capturedJson,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = palette.textPrimary,
                    )
                }
            },
            confirmButton = {
                PrimaryButton(
                    text = stringResource(R.string.ok),
                    onClick = { showJson = false },
                )
            },
            dismissButton = {
                GhostButton(
                    text = stringResource(R.string.copy_action),
                    onClick = { onCopy(capturedJson) },
                )
            },
        )
    }
}

private fun unescapeJson(s: String): String {
    var result = s
    result = result.replace("\\\\\"", "\"")
    result = result.replace("\\\\/", "/")
    result = result.replace("\\\\n", "\n")
    result = result.replace("\\\\t", "\t")
    result = result.replace("\\\\r", "\r")
    result = result.replace("\\\\\\\\", "\\\\")
    result = result.replace("\\\"", "\"")
    result = result.replace("\\/", "/")
    result = result.replace("\\n", "\n")
    result = result.replace("\\t", "\t")
    result = result.replace("\\r", "\r")
    result = result.replace("\\\\", "\\")
    return result
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}

/**
 * NAT 类型标签。
 *
 * 取值来自上游 `common.proto` 的 NatType 枚举（JSON 里是枚举名字符串），
 * 不再是「0..3 的序号」那套早已不存在的映射。
 */
@Composable
private fun natTypeName(natType: NatType): String = when (natType) {
    NatType.Unknown -> stringResource(R.string.status_nat_unknown)
    NatType.OpenInternet -> stringResource(R.string.status_nat_open_internet)
    NatType.NoPat -> stringResource(R.string.status_nat_no_pat)
    NatType.FullCone -> stringResource(R.string.status_nat_full_cone)
    NatType.Restricted -> stringResource(R.string.status_nat_restricted)
    NatType.PortRestricted -> stringResource(R.string.status_nat_port_restricted)
    NatType.Symmetric -> stringResource(R.string.status_nat_symmetric)
    NatType.SymUdpFirewall -> stringResource(R.string.status_nat_sym_udp_firewall)
    NatType.SymmetricEasyInc -> stringResource(R.string.status_nat_sym_easy_inc)
    NatType.SymmetricEasyDec -> stringResource(R.string.status_nat_sym_easy_dec)
}
