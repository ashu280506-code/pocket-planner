package com.ashu.pocketplanner.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashu.pocketplanner.data.Completion
import com.ashu.pocketplanner.data.Moods
import com.ashu.pocketplanner.data.Reminder
import com.ashu.pocketplanner.data.Repeat
import com.ashu.pocketplanner.logic.Fmt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** A frosted-glass panel. */
@Composable
fun Glass(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    Column(
        modifier
            .clip(shape)
            .background(if (p.dark) Color.Black.copy(alpha = 0.16f) else Color.Transparent)
            .background(Brush.verticalGradient(listOf(p.glassHi, p.glass)))
            .border(1.dp, p.border, shape),
        content = content,
    )
}

@Composable
fun GlassChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) p.accent else p.glassHi)
            .border(1.dp, if (selected) p.accent else p.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            color = if (selected) p.onAccent else p.text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun Label(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = LocalPalette.current.sub,
        letterSpacing = 1.2.sp,
    )
}

@Composable
fun Header(title: String, subtitle: String, onSettings: () -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = p.text, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = p.sub)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = p.text)
        }
    }
}

@Composable
fun Section(title: String, trailing: String = "", color: Color? = null, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = color ?: p.sub,
                letterSpacing = 1.2.sp,
                modifier = Modifier.weight(1f),
            )
            if (trailing.isNotEmpty()) Text(trailing, style = MaterialTheme.typography.labelMedium, color = p.sub)
        }
        Glass(Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun RowDivider() {
    HorizontalDivider(Modifier.padding(start = 56.dp), thickness = 0.5.dp, color = LocalPalette.current.border)
}

@Composable
fun TickCircle(done: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val p = LocalPalette.current
    val fill by animateFloatAsState(if (done) 1f else 0f, spring(dampingRatio = 0.45f, stiffness = 500f), label = "tick")
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .border(2.dp, if (done) p.accent else p.sub.copy(alpha = if (enabled) 0.9f else 0.35f), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .graphicsLayer {
                    scaleX = fill
                    scaleY = fill
                    alpha = fill.coerceIn(0f, 1f)
                }
                .clip(CircleShape)
                .background(p.accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = p.onAccent, modifier = Modifier.size(17.dp))
        }
    }
}

@Composable
fun UrgentTag() {
    val p = LocalPalette.current
    Box(
        Modifier.clip(RoundedCornerShape(50)).background(p.danger.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text("urgent", color = p.danger, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ReminderRow(
    r: Reminder,
    today: LocalDate,
    tickable: Boolean,
    showDate: Boolean = false,
    onTick: () -> Unit,
    onOpen: () -> Unit,
) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TickCircle(done = false, enabled = tickable, onClick = onTick)
        Spacer(Modifier.width(12.dp))
        Text(r.emoji.ifBlank { "📌" }, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                r.title,
                style = MaterialTheme.typography.bodyLarge,
                color = p.text,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val d = r.date
            val late = d != null && d.isBefore(today) && !r.done
            val meta = buildList {
                if (late && d != null) add(Fmt.late(d, today))
                r.time?.let { add(Fmt.time(it)) }
                if (showDate && d != null && !late) add(Fmt.day(d))
                if (r.repeat != Repeat.NONE) add("↻ " + r.repeat.label.lowercase())
                if (r.note.isNotBlank()) add(r.note.lineSequence().first())
            }
            if (meta.isNotEmpty()) {
                Text(
                    meta.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (late) p.danger else p.sub,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (r.urgent) {
            Spacer(Modifier.width(8.dp))
            UrgentTag()
        }
    }
}

@Composable
fun DoneRow(c: Completion, onUntick: (() -> Unit)?) {
    val p = LocalPalette.current
    val at = Instant.ofEpochMilli(c.at).atZone(ZoneId.systemDefault()).toLocalTime()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TickCircle(done = true, enabled = onUntick != null) { onUntick?.invoke() }
        Spacer(Modifier.width(12.dp))
        Text(c.emoji.ifBlank { "📌" }, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Text(
            c.title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = p.sub,
            textDecoration = TextDecoration.LineThrough,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(Fmt.time(at), style = MaterialTheme.typography.labelSmall, color = p.sub)
    }
}

@Composable
fun ProgressRing(done: Int, total: Int, diameter: Dp = 66.dp) {
    val p = LocalPalette.current
    val frac = if (total == 0) 0f else done.toFloat() / total
    val a by animateFloatAsState(frac, tween(700), label = "ring")
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw = 7.dp.toPx()
            val inset = sw / 2
            val arc = Size(size.width - sw, size.height - sw)
            drawArc(
                color = p.border,
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arc,
                style = Stroke(width = sw, cap = StrokeCap.Round),
            )
            if (a > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(p.accent, p.warm, p.accent)),
                    startAngle = -90f, sweepAngle = 360f * a, useCenter = false,
                    topLeft = Offset(inset, inset), size = arc,
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            if (total == 0) "–" else "${(frac * 100).roundToInt()}%",
            style = MaterialTheme.typography.labelLarge,
            color = p.text,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun MoodPicker(selected: Int?, onPick: (Int?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Moods.emojis.forEachIndexed { i, e ->
            val on = selected == i
            val scale by animateFloatAsState(if (on) 1.18f else 1f, spring(dampingRatio = 0.4f), label = "mood")
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (on) Color(Moods.colors[i]).copy(alpha = 0.32f) else Color.Transparent)
                    .clickable { onPick(if (on) null else i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(e, fontSize = 22.sp, modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale })
            }
        }
    }
}

@Composable
fun EmptyCard(title: String, body: String, chips: List<String> = emptyList(), onChip: (String) -> Unit = {}) {
    val p = LocalPalette.current
    Glass(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = p.text, fontWeight = FontWeight.Bold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = p.sub)
            if (chips.isNotEmpty()) {
                Spacer(Modifier.size(4.dp))
                ChipRow { chips.forEach { c -> GlassChip(c, selected = false) { onChip(c) } } }
            }
        }
    }
}

@Composable
fun ToggleRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = p.text, fontWeight = FontWeight.Medium)
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = p.sub)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = p.onAccent,
                checkedTrackColor = p.accent,
                uncheckedThumbColor = p.sub,
                uncheckedTrackColor = p.glassHi,
                uncheckedBorderColor = p.border,
            ),
        )
    }
}
