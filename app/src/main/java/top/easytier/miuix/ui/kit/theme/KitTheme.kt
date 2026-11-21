package top.easytier.miuix.ui.kit.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 语义色板入口：页面/组件只读这里，不写死 `Color(0x…)`。 */
val LocalPalette = staticCompositionLocalOf { LightPalette }

/** 圆角体系：卡片 26dp，面板 20dp，列表行 14dp，小元素 10dp，弹窗 34dp。 */
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(34.dp),
)

/** 字阶：32 大标题 / 24 弹窗 / 19 卡片标题 / 16 分区 / 15 正文 / 13.5 说明 / 12 描述。 */
private val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.6).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        letterSpacing = 0.8.sp,
    ),
)

/** 用主题色（主色/错误色/主色上的文字色）修正亮暗色板，保证与 miuix 组件同色。 */
fun AppPalette.withAccent(
    primary: Color? = null,
    onPrimary: Color? = null,
    danger: Color? = null,
): AppPalette = copy(
    primary = primary ?: this.primary,
    primaryDeep = primary ?: this.primaryDeep,
    onPrimary = onPrimary ?: this.onPrimary,
    danger = danger ?: this.danger,
)

/**
 * UI-Kit 主题：下发 MaterialTheme（字阶/形状/M3 配色）与 [LocalPalette]。
 *
 * 与 miuix 的 `MiuixTheme` 共存：miuix 组件读 MiuixTheme，自研组件读 LocalPalette，
 * 两边主色由 [AppPalette.withAccent] 对齐。
 */
@Composable
fun KitTheme(
    palette: AppPalette,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.glassStrongTop,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.accent,
            onSecondary = palette.onPrimary,
            tertiary = palette.success,
            background = palette.backgroundMiddle,
            onBackground = palette.textPrimary,
            surface = palette.backgroundMiddle,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.fieldFill,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.fieldBorder,
            outlineVariant = palette.divider,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.glassStrongTop,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.accent,
            onSecondary = palette.onPrimary,
            tertiary = palette.success,
            background = palette.backgroundMiddle,
            onBackground = palette.textPrimary,
            surface = palette.backgroundMiddle,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.fieldFill,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.fieldBorder,
            outlineVariant = palette.divider,
            error = palette.danger,
        )
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
