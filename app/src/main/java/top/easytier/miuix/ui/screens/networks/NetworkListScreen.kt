package top.easytier.miuix.ui.screens.networks

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.delay
import top.easytier.miuix.R
import top.easytier.miuix.data.model.ImportError
import top.easytier.miuix.data.model.NetworkInstance
import top.easytier.miuix.ui.kit.components.AppAlertDialog
import top.easytier.miuix.ui.kit.components.AppBadge
import top.easytier.miuix.ui.kit.components.AppCard
import top.easytier.miuix.ui.kit.components.GhostButton
import top.easytier.miuix.ui.kit.components.InlineNotice
import top.easytier.miuix.ui.kit.components.PageHeader
import top.easytier.miuix.ui.kit.components.PrimaryButton
import top.easytier.miuix.ui.kit.components.VerticalGap
import top.easytier.miuix.ui.kit.components.clickableNoRipple
import top.easytier.miuix.ui.kit.theme.LocalPalette

/**
 * 网络页
 *
 * 布局：大标题 → 正中电源按钮（外圈动画）→ 底部左侧「当前网络」卡 + 右侧「创建 / 导入」双卡。
 * 启动按钮从卡片里独立出来，状态（未连接 / 连接中 / 已连接 / 正在断开）由按钮下方文案承载；
 * 网络列表收进左下卡的「切换」面板，避免与主操作抢视觉重心。
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun NetworkListScreen(
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    // 保留参数：占位导航栏的跨 Tab 滚动状态，当前版式不再滚动列表
    listState: LazyListState = rememberLazyListState(),
    onEditNetwork: (String) -> Unit = {},
    onCreateNetwork: () -> Unit = {},
    viewModel: NetworkListViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    val instances by viewModel.instances.collectAsState()
    val configs by viewModel.configs.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val selectedId by viewModel.selectedInstanceId.collectAsState()
    val runningId by viewModel.runningInstanceId.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshConfigs() }

    // 电源按钮控制「运行中的那个」；没有运行中的网络时控制当前选中项
    val running = runningId != null
    val controlInstance = instances.firstOrNull { it.instanceId == runningId }
        ?: instances.firstOrNull { it.instanceId == selectedId }
    val controlConfig = configs.firstOrNull { it.instanceId == controlInstance?.instanceId }
    val nodeInfo = controlInstance?.detail?.myNodeInfo

    val deviceName = nodeInfo?.hostname.orEmpty().ifBlank { controlConfig?.hostname.orEmpty() }
    // 只展示地址本体：去掉 "/24" 这类前缀长度，卡片窄，带掩码必被省略号截断
    fun ipOnly(raw: String?): String = raw.orEmpty()
        .substringBefore('/')
        .trim()
        .takeIf { it.isNotBlank() && it != "0.0.0.0" }
        .orEmpty()
    val address = ipOnly(nodeInfo?.virtualIpv4?.toString())
        .ifBlank { ipOnly(controlConfig?.virtualIpv4) }

    // 启停过渡态：点击立刻进入「连接中 / 正在断开」，真实运行状态翻转后收起
    var pending by remember { mutableStateOf(false) }
    var pickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(running) { if (pending) pending = false }
    // 底层报错时立即结束过渡态，别让"连接中"和错误提示同时挂在屏幕上
    LaunchedEffect(controlInstance?.errorMsg) {
        if (pending && !controlInstance?.errorMsg.isNullOrBlank()) pending = false
    }
    LaunchedEffect(pending) {
        if (pending) {
            // 兜底：启动失败或底层无回调时，动画不能一直转下去
            delay(20_000)
            pending = false
        }
    }
    // 打开切换面板时刷新配置，等价于旧版的下拉刷新入口
    LaunchedEffect(pickerOpen) { if (pickerOpen) viewModel.refreshConfigs() }

    // 系统文件选择器：配置文件不限扩展名（JSON/TOML 都可能被识别成 octet-stream），
    // 格式问题交给解析层给出可读的失败提示。
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) viewModel.importConfigs(uri)
    }
    val onImportConfig: () -> Unit = { importLauncher.launch(arrayOf("*/*")) }

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

        // 中部：电源按钮（唯一主操作），上下留白由 weight 自动分配
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (instances.isEmpty()) {
                EmptyNetworkCard(
                    onCreateNetwork = onCreateNetwork,
                    onImportConfig = onImportConfig,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                PowerPanel(
                    running = running,
                    pending = pending,
                    errorMsg = controlInstance?.errorMsg.orEmpty(),
                    onToggle = {
                        if (!pending) {
                            pending = true
                            viewModel.toggleSelectedNetwork()
                        }
                    },
                )
            }
        }

        // 底部：左「当前网络」卡 + 右「创建 / 导入」双卡
        if (instances.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CurrentNetworkCard(
                    modifier = Modifier
                        .weight(1.18f)
                        .fillMaxHeight(),
                    name = controlInstance?.name.orEmpty(),
                    deviceName = deviceName,
                    address = address,
                    onSwitch = { pickerOpen = true },
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ActionTile(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        icon = Icons.Rounded.Add,
                        title = stringResource(R.string.network_create),
                        onClick = onCreateNetwork,
                    )
                    ActionTile(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        icon = Icons.Rounded.FileOpen,
                        title = stringResource(R.string.network_import),
                        onClick = onImportConfig,
                    )
                }
            }
        }

        Spacer(Modifier.height(contentBottomPadding + 12.dp))
    }

    if (pickerOpen) {
        NetworkPickerDialog(
            instances = instances,
            selectedId = selectedId,
            runningId = runningId,
            onDismiss = { pickerOpen = false },
            onSelect = { id ->
                viewModel.selectNetwork(id)
                pickerOpen = false
            },
            onEdit = { id ->
                pickerOpen = false
                onEditNetwork(id)
            },
        )
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

/** 中部电源区：大圆按钮 + 状态 + 提示，错误时追加内联提示条。 */
@Composable
private fun PowerPanel(
    running: Boolean,
    pending: Boolean,
    errorMsg: String,
    onToggle: () -> Unit,
) {
    val palette = LocalPalette.current
    val statusText = stringResource(
        when {
            pending && running -> R.string.network_disconnecting
            pending -> R.string.network_connecting
            running -> R.string.network_connected
            else -> R.string.network_disconnected
        },
    )
    val statusColor = when {
        pending -> palette.primary
        running -> palette.success
        else -> palette.textPrimary
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PowerOrb(running = running, pending = pending, onClick = onToggle)

        Spacer(Modifier.height(20.dp))

        Text(
            text = statusText,
            style = MaterialTheme.typography.titleLarge,
            color = statusColor,
        )

        Spacer(Modifier.height(6.dp))

        // 过渡态不显示"点击按钮…"，避免与"连接中"自相矛盾
        if (!pending) {
            Text(
                text = stringResource(
                    if (running) R.string.network_tap_to_disconnect else R.string.network_tap_to_connect,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
                textAlign = TextAlign.Center,
            )
        }

        if (errorMsg.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            InlineNotice(text = errorMsg)
        }
    }
}

/**
 * 圆形电源按钮：外圈为旋转弧 + 呼吸光晕，内圆为玻璃底 + 主色图标。
 * - 空闲：弧静止在顶部，光晕最暗；
 * - 连接中 / 断开中：弧加速旋转，内圆改画转圈指示；
 * - 运行中：弧慢速旋转 + 光晕呼吸。
 */
@Composable
private fun PowerOrb(
    running: Boolean,
    pending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    // 已连接用成功绿（与下方"已连接"文字同色），未连接/过渡态沿用主色蓝
    val accent = if (running) palette.success else palette.primary
    val active = running || pending

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "orbPress",
    )

    val transition = rememberInfiniteTransition(label = "orb")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (pending) 900 else 3600,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbSpin",
    )
    val breath by transition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbBreath",
    )

    Box(modifier = modifier.size(188.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val outer = size.minDimension / 2f
            val ringRadius = outer - 7.dp.toPx()

            // 光晕：运行时更亮，随呼吸起伏
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = (if (active) 0.24f else 0.13f) * breath),
                        Color.Transparent,
                    ),
                    center = c,
                    radius = outer,
                ),
                radius = outer,
                center = c,
            )

            // 底环
            drawCircle(
                color = accent.copy(alpha = 0.22f),
                radius = ringRadius,
                center = c,
                style = Stroke(width = 3.dp.toPx()),
            )

            // 进度弧：空闲时静止在顶部（微弱），激活时旋转
            rotate(degrees = if (active) spin else 0f, pivot = c) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            accent.copy(alpha = if (active) 0.95f else 0.55f),
                            Color.Transparent,
                        ),
                        center = c,
                    ),
                    startAngle = -22f,
                    sweepAngle = 104f,
                    useCenter = false,
                    topLeft = Offset(c.x - ringRadius, c.y - ringRadius),
                    size = Size(ringRadius * 2, ringRadius * 2),
                    style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(118.dp)
                .graphicsLayer {
                    scaleX = pressScale
                    scaleY = pressScale
                }
                // 不用 Modifier.shadow：彩色投影落在浅色半透明圆盘上会被渲染成一团亮色模糊，
                // 在图标周围形成"白圈"；层次由外圈 radialGradient 光晕承担。
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(palette.glassStrongTop, palette.glassStrongBottom),
                    ),
                )
                .border(1.dp, accent.copy(alpha = 0.35f), CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (pending) {
                Canvas(Modifier.size(44.dp)) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        color = accent.copy(alpha = 0.18f),
                        radius = r,
                        style = Stroke(width = 3.dp.toPx()),
                    )
                    rotate(degrees = spin, pivot = center) {
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 96f,
                            useCenter = false,
                            topLeft = Offset(center.x - r, center.y - r),
                            size = Size(r * 2, r * 2),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }
                }
            } else {
                Icon(
                    imageVector = if (running) Icons.Rounded.Stop else Icons.Rounded.PowerSettingsNew,
                    contentDescription = stringResource(
                        if (running) R.string.network_stop_action else R.string.network_start_action,
                    ),
                    tint = accent,
                    modifier = Modifier.size(52.dp),
                )
            }
        }
    }
}

/** 左下「当前网络」卡：名称 + 虚拟 IP / 主机名，点整卡打开切换面板。 */
@Composable
private fun CurrentNetworkCard(
    name: String,
    deviceName: String,
    address: String,
    onSwitch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    AppCard(
        modifier = modifier,
        onClick = onSwitch,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.network_current),
                fontSize = 11.sp,
                color = palette.textTertiary,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Rounded.SwapHoriz,
                contentDescription = null,
                tint = palette.primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = stringResource(R.string.network_switch),
                fontSize = 11.sp,
                color = palette.primary,
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = name.ifBlank { "—" },
            style = MaterialTheme.typography.titleMedium,
            color = palette.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(6.dp))

        // 文案与「状态」页保持一致：虚拟 IP / 主机名
        CompactInfoRow(
            label = stringResource(R.string.status_virtual_ip),
            value = address.ifBlank { "—" },
            monospace = true,
        )
        if (deviceName.isNotBlank()) {
            CompactInfoRow(
                label = stringResource(R.string.status_hostname),
                value = deviceName,
            )
        }
    }
}

/** 卡片内的紧凑信息行：标签固定宽度，值单行省略。 */
@Composable
private fun CompactInfoRow(label: String, value: String, monospace: Boolean = false) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = palette.textTertiary,
            maxLines = 1,
            modifier = Modifier.width(52.dp),
        )
        Text(
            text = value,
            fontSize = 11.5.sp,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            color = palette.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 右侧操作小卡：图标 + 文案 + 箭头。 */
@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    AppCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        palette.primary.copy(alpha = if (palette.dark) 0.22f else 0.12f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(17.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = palette.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

/** 切换网络面板：单选网络，行尾可进编辑页。 */
@Composable
private fun NetworkPickerDialog(
    instances: List<NetworkInstance>,
    selectedId: String?,
    runningId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    onEdit: (String) -> Unit,
) {
    val palette = LocalPalette.current
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.network_pick_title),
        text = {
            if (instances.isEmpty()) {
                Text(
                    text = stringResource(R.string.network_pick_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textTertiary,
                )
            } else {
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    instances.forEach { instance ->
                        PickerRow(
                            instance = instance,
                            selected = instance.instanceId == selectedId,
                            running = instance.instanceId == runningId,
                            onClick = { onSelect(instance.instanceId) },
                            onEdit = { onEdit(instance.instanceId) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = stringResource(R.string.ok),
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun PickerRow(
    instance: NetworkInstance,
    selected: Boolean,
    running: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickableNoRipple(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = instance.name.ifEmpty { instance.instanceId.take(8) },
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (running || instance.errorMsg.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (running) {
                        AppBadge(
                            text = stringResource(R.string.network_running),
                            tint = palette.success,
                        )
                    }
                    if (instance.errorMsg.isNotEmpty()) {
                        AppBadge(
                            text = stringResource(R.string.error),
                            tint = palette.danger,
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickableNoRipple(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.network_edit),
                tint = palette.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
        Icon(
            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = stringResource(
                if (selected) R.string.network_selected else R.string.network_select,
            ),
            tint = if (selected) palette.primary else palette.textTertiary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp),
        )
    }
}

/** 空态卡片：图标 + 说明 + 创建/导入入口（此状态下底部操作卡不出现） */
@Composable
private fun EmptyNetworkCard(
    onCreateNetwork: () -> Unit,
    onImportConfig: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    AppCard(modifier = modifier, contentPadding = PaddingValues(22.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.primary.copy(alpha = if (palette.dark) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lan,
                    contentDescription = null,
                    tint = palette.primary,
                    modifier = Modifier.size(28.dp),
                )
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
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.network_create),
                    onClick = onCreateNetwork,
                    icon = Icons.Rounded.Add,
                    modifier = Modifier.weight(1.4f),
                )
                GhostButton(
                    text = stringResource(R.string.network_import),
                    onClick = onImportConfig,
                    icon = Icons.Rounded.FileOpen,
                    modifier = Modifier.weight(1f),
                )
            }
        }
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
