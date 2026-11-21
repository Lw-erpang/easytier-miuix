package top.easytier.miuix.ui.kit.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.easytier.miuix.ui.kit.theme.LocalPalette

/** 无涟漪点击，避免破坏玻璃质感。 */
@Composable
fun Modifier.clickableNoRipple(
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        onClick = onClick,
    )
}

/**
 * 玻璃卡片：半透明渐变底 + 顶部高光线 + 1dp 亮描边 + 柔和投影。
 * 内边距由调用方通过 [contentPadding] 指定。
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    strong: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    val top = if (strong) palette.glassStrongTop else palette.glassTop
    val bottom = if (strong) palette.glassStrongBottom else palette.glassBottom

    val borderBrush = Brush.linearGradient(
        listOf(
            palette.glassBorder,
            palette.glassBorder.copy(alpha = palette.glassBorder.alpha * 0.25f),
        ),
    )

    var surface = modifier.shadow(
        elevation = if (palette.dark) 12.dp else 18.dp,
        shape = shape,
        clip = false,
        ambientColor = palette.shadow,
        spotColor = palette.shadow,
    )
    if (onClick != null) {
        surface = surface.clickableNoRipple(onClick = onClick)
    }
    surface = surface
        .clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        // 顶部高光线：drawBehind 只画不参与测量
        .drawBehind {
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = if (palette.dark) 0.18f else 0.75f),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, 1.dp.toPx()),
            )
        }
        .border(1.dp, borderBrush, shape)

    Box(surface) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/**
 * 小标签：亮色实底胶囊（无描边），暗色半透明底 + 细描边。
 * 文字永不换行——放置容器用 FlowRow，空间不足时整枚标签换行。
 */
@Composable
fun AppBadge(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    icon: ImageVector? = null,
) {
    val palette = LocalPalette.current
    val color = tint ?: palette.textSecondary
    val tagModifier = if (palette.dark) {
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.18f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    } else {
        Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    }
    Row(
        tagModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        }
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = if (palette.dark) 0.6.sp else 0.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 页面标题区：eyebrow（可选，11sp 主色 + 引线）→ 大标题 32sp → 副标题 13.5sp。 */
@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    eyebrow: String? = null,
    badge: String? = null,
    badgeTint: Color? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    subtitleContent: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        if (!eyebrow.isNullOrBlank()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = eyebrow,
                    color = palette.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.6.sp,
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .height(1.dp)
                        .width(26.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(palette.primary.copy(alpha = 0.7f), Color.Transparent),
                            ),
                        ),
                )
                Spacer(Modifier.weight(1f))
                if (badge != null) AppBadge(text = badge, tint = badgeTint)
                trailing?.invoke(this)
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.displaySmall,
                color = palette.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (titleTrailing != null) {
                Spacer(Modifier.width(12.dp))
                titleTrailing()
            }
        }
        Spacer(Modifier.height(6.dp))
        if (subtitleContent != null) {
            subtitleContent()
        } else {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.textSecondary,
            )
        }
    }
}

/** 卡片内分区标题 + 可选图标底 + 右侧操作。 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(palette.primary.copy(alpha = if (palette.dark) 0.22f else 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = palette.primary, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = palette.textPrimary,
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textTertiary,
                )
            }
        }
        if (action != null) action()
    }
}

/** 纯文字「标签: 值」信息行（不要用徽章展示这类只读信息）。 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelWidth: Dp = 84.dp,
    monospace: Boolean = false,
) {
    val palette = LocalPalette.current
    Row(modifier = modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textTertiary,
            modifier = Modifier.width(labelWidth),
        )
        Text(
            text = value,
            style = if (monospace) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodySmall
            },
            color = palette.textPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun HairLine(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.divider),
    )
}

@Composable
fun VerticalGap(height: Dp) = Spacer(Modifier.height(height))

/**
 * 极光背景：三段基础渐变 + 三个缓慢漂移的柔光团。
 * 放在内容之下，卡片半透明才能「透」出光团；[animated] = false 时完全静止（不启动动画帧）。
 */
@Composable
fun AuroraBackground(modifier: Modifier = Modifier, animated: Boolean = true) {
    val palette = LocalPalette.current
    // 注意：animated = false 时绝不能用「时长 1ms 的无限动画」——
    // 光斑会逐帧在两端之间跳变，高刷新率屏幕上表现为背景闪烁。
    val drift: Float
    val drift2: Float
    if (animated) {
        val transition = rememberInfiniteTransition(label = "aurora")
        drift = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 21000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "drift",
        ).value
        drift2 = transition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 27000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "drift2",
        ).value
    } else {
        // 固定一个观感均衡的光斑姿态
        drift = 0.30f
        drift2 = 0.55f
    }

    val primaryAlpha = if (palette.dark) 0.50f else 0.42f
    val accentAlpha = if (palette.dark) 0.34f else 0.30f
    val mintAlpha = if (palette.dark) 0.26f else 0.24f

    Canvas(modifier.fillMaxSize()) {
        drawRect(
            Brush.verticalGradient(
                listOf(palette.backgroundTop, palette.backgroundMiddle, palette.backgroundBottom),
            ),
        )

        val w = size.width
        val h = size.height

        fun glow(center: Offset, radius: Float, color: Color, alpha: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        glow(
            center = Offset(w * (0.06f + 0.16f * drift), h * (0.02f + 0.10f * drift2)),
            radius = w * 0.95f,
            color = palette.blobPrimary,
            alpha = primaryAlpha,
        )
        glow(
            center = Offset(w * (0.86f - 0.14f * drift2), h * (0.10f + 0.08f * drift)),
            radius = w * 0.62f,
            color = palette.blobAccent,
            alpha = accentAlpha,
        )
        glow(
            center = Offset(w * (0.08f + 0.10f * drift2), h * (0.86f - 0.06f * drift)),
            radius = w * 0.75f,
            color = palette.blobMint,
            alpha = mintAlpha,
        )
    }
}
