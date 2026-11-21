package top.easytier.miuix.ui.kit.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * 语义色板（唯一色源，取值来自 `E:\Android\work\UI-Kit`）。
 *
 * 组件只消费语义名，亮/暗两套自动切换；运行时会用 miuix 主题的主色/错误色
 * 覆盖 [primary] / [danger]，保证「莫奈取色 / 主题色」改色后与 miuix 组件同色。
 */
@Immutable
data class AppPalette(
    val dark: Boolean,
    val backgroundTop: Color,
    val backgroundMiddle: Color,
    val backgroundBottom: Color,
    val blobPrimary: Color,
    val blobAccent: Color,
    val blobMint: Color,
    val primary: Color,
    val primaryDeep: Color,
    val onPrimary: Color,
    val accent: Color,
    val glassTop: Color,
    val glassBottom: Color,
    val glassBorder: Color,
    val glassStrongTop: Color,
    val glassStrongBottom: Color,
    val fieldFill: Color,
    val fieldBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val success: Color,
    val successContainer: Color,
    val danger: Color,
    val navIndicator: Color,
    val shadow: Color,
    val divider: Color,
)

val LightPalette = AppPalette(
    dark = false,
    backgroundTop = Color(0xFFEDF4FF),
    backgroundMiddle = Color(0xFFF9FCFF),
    backgroundBottom = Color(0xFFE6EFFB),
    blobPrimary = Color(0xFF6FA9FF),
    blobAccent = Color(0xFFFFC06B),
    blobMint = Color(0xFF7FDCC0),
    primary = Color(0xFF3482FF),
    primaryDeep = Color(0xFF2B6FD9),
    onPrimary = Color(0xFFFFFFFF),
    accent = Color(0xFFF7A429),
    glassTop = Color(0xCCFFFFFF),
    glassBottom = Color(0x8FFFFFFF),
    glassBorder = Color(0xE6FFFFFF),
    glassStrongTop = Color(0xF0FFFFFF),
    glassStrongBottom = Color(0xD1FFFFFF),
    fieldFill = Color(0x9EFFFFFF),
    fieldBorder = Color(0x1F0B1B2B),
    textPrimary = Color(0xFF263445),
    textSecondary = Color(0xFF5D7290),
    textTertiary = Color(0xFF8C93B0),
    success = Color(0xFF34C759),
    successContainer = Color(0xFFE3F8EC),
    danger = Color(0xFFE94634),
    navIndicator = Color(0x2E3482FF),
    shadow = Color(0x241B3A66),
    divider = Color(0x0F0B1B2B),
)

val DarkPalette = AppPalette(
    dark = true,
    backgroundTop = Color(0xFF060B14),
    backgroundMiddle = Color(0xFF0C1524),
    backgroundBottom = Color(0xFF070D18),
    blobPrimary = Color(0xFF1E63C8),
    blobAccent = Color(0xFF1F8A70),
    blobMint = Color(0xFF15755C),
    primary = Color(0xFF5AAEFF),
    primaryDeep = Color(0xFF2D7FD1),
    onPrimary = Color(0xFF04121F),
    accent = Color(0xFFFFB65C),
    glassTop = Color(0xC71B2740),
    glassBottom = Color(0x9E0E1728),
    glassBorder = Color(0x2EFFFFFF),
    glassStrongTop = Color(0xE022304C),
    glassStrongBottom = Color(0xB3131E31),
    fieldFill = Color(0x14FFFFFF),
    fieldBorder = Color(0x29FFFFFF),
    textPrimary = Color(0xFFEAF1FB),
    textSecondary = Color(0xFFA9BAD1),
    textTertiary = Color(0xFF7689A2),
    success = Color(0xFF3ED9A0),
    successContainer = Color(0xFF11322A),
    danger = Color(0xFFF12522),
    navIndicator = Color(0x2958AEFF),
    shadow = Color(0x8C000000),
    divider = Color(0x14FFFFFF),
)
