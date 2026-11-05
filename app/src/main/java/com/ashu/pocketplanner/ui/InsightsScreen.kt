package com.ashu.pocketplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashu.pocketplanner.data.AppState
import com.ashu.pocketplanner.data.Moods
import com.ashu.pocketplanner.data.Reminder
import com.ashu.pocketplanner.logic.Fmt
import com.ashu.pocketplanner.logic.Plan
import java.time.LocalDate

@Composable
fun InsightsScreen(
    s: AppState,
    today: LocalDate,
    modifier: Modifier,
    onOpen: (Reminder) -> Unit,
    onSettings: () -> Unit,
) {
    val p = LocalPalette.current
    var mode by rememberSaveable { mutableIntStateOf(0) }
    var weekOffset by rememberSaveable { mutableIntStateOf(0) }

    val weekStart = today.minusDays((today.dayOfWeek.value % 7).toLong()).plusWeeks(weekOffset.toLong())
    val days = (0..6).map { weekStart.plusDays(it.toLong()) }
    val weekDone = s.history.filter { !it.date.isBefore(weekStart) && it.date.isBefore(weekStart.plusDays(7)) }
    val counts = days.map { d -> weekDone.count { it.date == d } }
    val prevTotal = s.history.count { !it.date.isBefore(weekStart.minusWeeks(1)) && it.date.isBefore(weekStart) }
    val total = counts.sum()
    val bestIdx = counts.indices.maxByOrNull { counts[it] }?.takeIf { counts[it] > 0 }
    val top = weekDone.groupBy { it.title.lowercase() }.maxByOrNull { it.value.size }?.value
    val slipping = Plan.overdue(s, today)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header("Insights", if (mode == 0) "Your week at a glance" else "Everything you've finished", onSettings) }
        item {
            ChipRow {
                GlassChip("Weekly report", mode == 0) { mode = 0 }
                GlassChip("History", mode == 1) { mode = 1 }
            }
        }

        if (mode == 0) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { weekOffset -= 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week", tint = p.text)
                    }
                    Text(
                        (if (weekOffset == 0) "This week · " else "") + "${Fmt.dayMonth(weekStart)} – ${Fmt.dayMonth(weekStart.plusDays(6))}",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = p.text,
                    )
                    IconButton(onClick = { if (weekOffset < 0) weekOffset += 1 }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week", tint = if (weekOffset < 0) p.text else p.sub.copy(alpha = 0.3f))
                    }
                }
            }
            item {
                Glass(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("$total", style = MaterialTheme.typography.displayMedium, color = p.text, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.padding(bottom = 8.dp)) {
                                Text("tasks done", style = MaterialTheme.typography.bodyMedium, color = p.sub)
                                val diff = total - prevTotal
                                Text(
                                    when {
                                        prevTotal == 0 && total == 0 -> "Quiet week so far"
                                        diff > 0 -> "▲ $diff more than the week before"
                                        diff < 0 -> "▼ ${-diff} fewer than the week before"
                                        else -> "Same as the week before"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (diff >= 0) p.accent else p.warm,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        BarChart(counts, days.map { Fmt.day(it).take(2) }, bestIdx, days.indexOf(today))
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(
                        Modifier.weight(1f),
                        "Best day",
                        bestIdx?.let { Fmt.weekday(days[it]) } ?: "–",
                        bestIdx?.let { "${counts[it]} done" } ?: "Tick something off",
                    )
                    StatCard(
                        Modifier.weight(1f),
                        "Most done",
                        top?.first()?.let { "${it.emoji} ${it.title}" } ?: "–",
                        top?.let { "${it.size}× this week" } ?: "Nothing yet",
                    )
                }
            }
            item {
                Section("Mood this week") {
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 8.dp)) {
                        days.forEach { d ->
                            val m = s.moods[d]
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (m != null) androidx.compose.ui.graphics.Color(Moods.colors[m]).copy(alpha = 0.25f) else p.glass)
                                        .padding(6.dp),
                                ) {
                                    Text(m?.let { Moods.emojis[it] } ?: "·", fontSize = 20.sp, color = p.sub)
                                }
                                Text(Fmt.day(d).take(2), style = MaterialTheme.typography.labelSmall, color = p.sub)
                            }
                        }
                    }
                }
            }
            item {
                if (slipping.isEmpty()) {
                    EmptyCard("Nothing slipping 👌", "No overdue tasks right now.")
                } else {
                    Section("Kept slipping", "${slipping.size} overdue", color = p.danger) {
                        slipping.take(6).forEachIndexed { i, r ->
                            if (i > 0) RowDivider()
                            ReminderRow(r, today, tickable = false, onTick = {}, onOpen = { onOpen(r) })
                        }
                    }
                }
            }
        } else {
            val groups = s.history.sortedByDescending { it.at }.groupBy { it.date }
            if (groups.isEmpty()) {
                item { EmptyCard("No history yet", "Everything you tick off shows up here, day by day.") }
            }
            groups.forEach { (d, list) ->
                item(key = d.toString()) {
                    Section(Fmt.longDay(d), "${list.size} done") {
                        list.forEachIndexed { i, c ->
                            if (i > 0) RowDivider()
                            DoneRow(c, null)
                        }
                    }
                }
            }
            item {
                Text(
                    "History older than 6 months clears itself.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.sub,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, label: String, value: String, sub: String) {
    val p = LocalPalette.current
    Glass(modifier) {
        Column(Modifier.padding(16.dp)) {
            Label(label)
            Text(value, style = MaterialTheme.typography.titleMedium, color = p.text, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = p.sub, maxLines = 1)
        }
    }
}

@Composable
private fun BarChart(values: List<Int>, labels: List<String>, highlight: Int?, todayIdx: Int) {
    val p = LocalPalette.current
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    Row(
        Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        values.forEachIndexed { i, v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (v > 0) "$v" else "", style = MaterialTheme.typography.labelSmall, color = p.sub)
                val frac = v.toFloat() / max
                val brush = if (i == highlight) Brush.verticalGradient(listOf(p.warm, p.accent))
                else SolidColor(p.accent.copy(alpha = if (v == 0) 0.15f else 0.5f))
                Box(
                    Modifier
                        .padding(horizontal = 6.dp)
                        .fillMaxWidth()
                        .height((8f + 96f * frac).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(brush),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    labels[i],
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == todayIdx) p.warm else p.sub,
                    fontWeight = if (i == todayIdx) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
    }
}
