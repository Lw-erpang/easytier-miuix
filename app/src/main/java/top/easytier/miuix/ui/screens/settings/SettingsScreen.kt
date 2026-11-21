package top.easytier.miuix.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import top.easytier.miuix.R
import top.easytier.miuix.ui.dialogs.AboutDialog
import top.easytier.miuix.ui.dialogs.ConfigServerDialog
import top.easytier.miuix.ui.kit.components.AppAlertDialog
import top.easytier.miuix.ui.kit.components.AppCard
import top.easytier.miuix.ui.kit.components.ChoiceRow
import top.easytier.miuix.ui.kit.components.GhostButton
import top.easytier.miuix.ui.kit.components.HairLine
import top.easytier.miuix.ui.kit.components.PageHeader
import top.easytier.miuix.ui.kit.components.SettingRow
import top.easytier.miuix.ui.kit.components.VerticalGap
import top.easytier.miuix.ui.kit.theme.LocalPalette
import top.easytier.miuix.ui.theme.AppLanguage
import top.easytier.miuix.ui.theme.AppSettings

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    contentBottomPadding: Dp = 0.dp,
    listState: LazyListState = rememberLazyListState(),
    appSettings: AppSettings = AppSettings(),
    onSettingsChange: (AppSettings) -> Unit = {},
    onOpenTheme: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val palette = LocalPalette.current
    var showConfigServer by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }

    val configServerUrl by viewModel.configServerUrl.collectAsState()
    val configServerConnected by viewModel.configServerConnected.collectAsState()

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
                title = stringResource(R.string.nav_settings),
                eyebrow = stringResource(R.string.settings_eyebrow),
                subtitle = stringResource(R.string.settings_subtitle),
            )
        }

        // 连接
        item { SectionLabel(stringResource(R.string.settings_group_connection)) }
        item {
            AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp)) {
                SettingRow(
                    title = stringResource(R.string.settings_config_server),
                    summary = when {
                        configServerUrl == null -> stringResource(R.string.settings_config_server_not_configured)
                        configServerConnected -> stringResource(R.string.config_server_state_connected)
                        else -> stringResource(R.string.config_server_state_disconnected)
                    },
                    onClick = { showConfigServer = true },
                )
            }
        }

        // 外观：主题与语言同属显示偏好
        item { SectionLabel(stringResource(R.string.settings_group_appearance)) }
        item {
            AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp)) {
                Column {
                    SettingRow(
                        title = stringResource(R.string.settings_theme),
                        summary = stringResource(R.string.settings_theme_summary),
                        onClick = onOpenTheme,
                    )
                    HairLine()
                    SettingRow(
                        title = stringResource(R.string.settings_language),
                        summary = appSettings.language.displayName,
                        onClick = { showLanguage = true },
                    )
                }
            }
        }

        // 关于
        item { SectionLabel(stringResource(R.string.settings_group_about)) }
        item {
            AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 18.dp)) {
                SettingRow(
                    title = stringResource(R.string.settings_about),
                    summary = stringResource(R.string.settings_about_summary),
                    onClick = { showAbout = true },
                )
            }
        }

        item {
            VerticalGap(6.dp)
            Text(
                text = stringResource(R.string.settings_footer),
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 0.6.sp,
                color = palette.textTertiary,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }

    if (showConfigServer) {
        ConfigServerDialog(
            currentUrl = configServerUrl,
            connected = configServerConnected,
            onConnect = { url, onResult -> viewModel.connectConfigServer(url, onResult) },
            onDisconnect = { viewModel.disconnectConfigServer() },
            onDismiss = { showConfigServer = false },
        )
    }
    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }
    if (showLanguage) {
        AppAlertDialog(
            onDismissRequest = { showLanguage = false },
            title = stringResource(R.string.settings_language),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    AppLanguage.entries.forEach { language ->
                        ChoiceRow(
                            text = language.displayName,
                            selected = appSettings.language == language,
                            onClick = {
                                onSettingsChange(appSettings.copy(language = language))
                                showLanguage = false
                            },
                        )
                    }
                }
            },
            confirmButton = {
                GhostButton(
                    text = stringResource(R.string.cancel),
                    onClick = { showLanguage = false },
                )
            },
        )
    }
}

/** 分组小标题：11sp 弱文字 + 字距，与卡片间距 6dp */
@Composable
private fun SectionLabel(text: String) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(start = 6.dp, top = 4.dp)) {
        Text(
            text = text,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            color = palette.textTertiary,
        )
    }
}
