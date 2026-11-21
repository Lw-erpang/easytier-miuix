package top.easytier.miuix.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.easytier.miuix.R
import top.easytier.miuix.ui.kit.components.AppCard
import top.easytier.miuix.ui.kit.components.ChoiceChip
import top.easytier.miuix.ui.kit.components.HairLine
import top.easytier.miuix.ui.kit.components.PageHeader
import top.easytier.miuix.ui.kit.components.SectionTitle
import top.easytier.miuix.ui.kit.components.SwitchRow
import top.easytier.miuix.ui.kit.components.VerticalGap
import top.easytier.miuix.ui.kit.components.clickableNoRipple
import top.easytier.miuix.ui.kit.theme.LocalPalette
import top.easytier.miuix.ui.theme.AppSettings
import top.easytier.miuix.ui.theme.ColorMode
import top.easytier.miuix.ui.theme.keyColorOptions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemeSettingsScreen(
    appSettings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current

    val themeModes = listOf(
        stringResource(R.string.settings_theme_mode_system),
        stringResource(R.string.settings_theme_mode_light),
        stringResource(R.string.settings_theme_mode_dark),
    )

    val selectedThemeMode = when (appSettings.colorMode) {
        ColorMode.SYSTEM, ColorMode.MONET_SYSTEM -> 0
        ColorMode.LIGHT, ColorMode.MONET_LIGHT -> 1
        ColorMode.DARK, ColorMode.MONET_DARK -> 2
    }

    val isMonet = appSettings.colorMode.isMonet
    val colorValues = listOf(0) + keyColorOptions

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item { VerticalGap(18.dp) }

        item {
            PageHeader(
                title = stringResource(R.string.settings_theme),
                eyebrow = stringResource(R.string.theme_eyebrow),
                subtitle = stringResource(R.string.settings_theme_summary),
                badge = when (selectedThemeMode) {
                    1 -> themeModes[1]
                    2 -> themeModes[2]
                    else -> themeModes[0]
                },
            )
        }

        // 外观模式：三等分选择
        item {
            AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                SectionTitle(
                    title = stringResource(R.string.theme_mode_title),
                    subtitle = stringResource(R.string.theme_mode_subtitle),
                )
                VerticalGap(12.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    themeModes.forEachIndexed { index, label ->
                        ChoiceChip(
                            text = label,
                            selected = selectedThemeMode == index,
                            onClick = {
                                val newMode = when (index) {
                                    0 -> if (isMonet) ColorMode.MONET_SYSTEM else ColorMode.SYSTEM
                                    1 -> if (isMonet) ColorMode.MONET_LIGHT else ColorMode.LIGHT
                                    2 -> if (isMonet) ColorMode.MONET_DARK else ColorMode.DARK
                                    else -> appSettings.colorMode
                                }
                                onSettingsChange(appSettings.copy(colorMode = newMode))
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // 莫奈取色 + 主题色
        item {
            AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                SwitchRow(
                    title = stringResource(R.string.settings_monet),
                    summary = stringResource(R.string.settings_monet_summary),
                    checked = isMonet,
                    onCheckedChange = { enabled ->
                        val newMode = if (enabled) {
                            when (appSettings.colorMode) {
                                ColorMode.SYSTEM, ColorMode.MONET_SYSTEM -> ColorMode.MONET_SYSTEM
                                ColorMode.LIGHT, ColorMode.MONET_LIGHT -> ColorMode.MONET_LIGHT
                                ColorMode.DARK, ColorMode.MONET_DARK -> ColorMode.MONET_DARK
                            }
                        } else {
                            when (appSettings.colorMode) {
                                ColorMode.SYSTEM, ColorMode.MONET_SYSTEM -> ColorMode.SYSTEM
                                ColorMode.LIGHT, ColorMode.MONET_LIGHT -> ColorMode.LIGHT
                                ColorMode.DARK, ColorMode.MONET_DARK -> ColorMode.DARK
                            }
                        }
                        onSettingsChange(appSettings.copy(colorMode = newMode))
                    },
                )
                AnimatedVisibility(visible = isMonet) {
                    Column {
                        HairLine()
                        VerticalGap(12.dp)
                        Text(
                            text = stringResource(R.string.settings_key_color),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.textTertiary,
                        )
                        VerticalGap(10.dp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ChoiceChip(
                                text = stringResource(R.string.settings_key_color_default),
                                selected = appSettings.keyColor == 0,
                                onClick = { onSettingsChange(appSettings.copy(keyColor = 0)) },
                            )
                        }
                        VerticalGap(12.dp)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            keyColorOptions.forEachIndexed { index, argb ->
                                val color = Color(argb)
                                val selected = appSettings.keyColor == colorValues.getOrNull(index + 1)
                                ColorDot(
                                    color = color,
                                    selected = selected,
                                    onClick = { onSettingsChange(appSettings.copy(keyColor = argb)) },
                                )
                            }
                        }
                        VerticalGap(6.dp)
                    }
                }
            }
        }

        // 界面效果：模糊 / 悬浮底栏（必须保留）
        item {
            AppCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
                Column {
                    SectionTitle(
                        title = stringResource(R.string.theme_effects_title),
                        subtitle = stringResource(R.string.theme_effects_subtitle),
                    )
                }
                SwitchRow(
                    title = stringResource(R.string.settings_blur_effects),
                    summary = stringResource(R.string.settings_blur_effects_summary),
                    checked = appSettings.enableBlur,
                    onCheckedChange = { onSettingsChange(appSettings.copy(enableBlur = it)) },
                )
                HairLine()
                SwitchRow(
                    title = stringResource(R.string.settings_floating_bottom_bar),
                    summary = stringResource(R.string.settings_floating_bottom_bar_summary),
                    checked = appSettings.enableFloatingBottomBar,
                    onCheckedChange = { onSettingsChange(appSettings.copy(enableFloatingBottomBar = it)) },
                )
                AnimatedVisibility(visible = appSettings.enableFloatingBottomBar) {
                    Column {
                        HairLine()
                        SwitchRow(
                            title = stringResource(R.string.settings_floating_bar_blur),
                            summary = stringResource(R.string.settings_floating_bar_blur_summary),
                            checked = appSettings.enableFloatingBottomBarBlur,
                            onCheckedChange = { onSettingsChange(appSettings.copy(enableFloatingBottomBarBlur = it)) },
                        )
                    }
                }
            }
        }

        item {
            VerticalGap(4.dp)
            Text(
                text = stringResource(R.string.theme_footer),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textTertiary,
                letterSpacing = 0.6.sp,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

/** 主题色圆点：选中态外圈描边 + 中心勾 */
@Composable
private fun ColorDot(color: Color, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) palette.textPrimary else palette.fieldBorder,
                shape = CircleShape,
            )
            .clickableNoRipple(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
