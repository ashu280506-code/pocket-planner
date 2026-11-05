package com.ashu.pocketplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ashu.pocketplanner.data.AppState
import com.ashu.pocketplanner.data.Completion
import com.ashu.pocketplanner.data.Moods
import com.ashu.pocketplanner.data.Reminder
import com.ashu.pocketplanner.data.Repeat
import com.ashu.pocketplanner.logic.Fmt
import com.ashu.pocketplanner.logic.Plan
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen(
    s: AppState,
    today: LocalDate,
    selected: LocalDate,
    modifier: Modifier,
    onSelect: (LocalDate) -> Unit,
    onTick: (Reminder, LocalDate) -> Unit,
    onUntick: (Completion) -> Unit,
    onOpen: (Reminder) -> Unit,
    onAdd: (LocalDate) -> Unit,
    onMood: (LocalDate, Int?) -> Unit,
    onSettings: () -> Unit,
) {
    val p = LocalPalette.current
    var month by remember { mutableStateOf(YearMonth.from(selected)) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header("Calendar", Fmt.month(month), onSettings) }

        item {
            Glass(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { month = month.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = p.text)
                        }
                        Text(
                            Fmt.month(month),
                            modifier = Modifier.weight(1f).clickable {
                                month = YearMonth.from(today)
                                onSelect(today)
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleMedium,
                            color = p.text,
                            fontWeight = FontWeight.SemiBold,
                        )
                        IconButton(onClick = { month = month.plusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = p.text)
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                            Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = p.sub)
                        }
                    }
                    val first = month.atDay(1)
                    val offset = first.dayOfWeek.value % 7
                    val days = month.lengthOfMonth()
                    val rows = (offset + days + 6) / 7
                    for (w in 0 until rows) {
                        Row(Modifier.fillMaxWidth()) {
                            for (c in 0 until 7) {
                                val n = w * 7 + c - offset + 1
                                if (n < 1 || n > days) {
                                    Spacer(Modifier.weight(1f).aspectRatio(0.88f))
                                } else {
                                    val d = month.atDay(n)
                                    DayCell(s, d, today, d == selected) { onSelect(d) }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            val open = Plan.dayOpen(s, selected, today) + if (selected == today) Plan.overdue(s, today) else emptyList()
            val done = Plan.doneOn(s, selected)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(Fmt.longDay(selected), style = MaterialTheme.typography.titleLarge, color = p.text, fontWeight = FontWeight.Bold)
                        Text(Fmt.rel(selected, today), style = MaterialTheme.typography.bodySmall, color = p.sub)
                    }
                    OutlinedButton(onClick = { onAdd(selected) }) { Text("Add", color = p.text) }
                }
                if (!selected.isAfter(today)) {
                    Glass(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Mood", style = MaterialTheme.typography.bodyMedium, color = p.sub, modifier = Modifier.weight(1f))
                            MoodPicker(s.moods[selected]) { onMood(selected, it) }
                        }
                    }
                }
                if (open.isEmpty() && done.isEmpty()) {
                    EmptyCard("Nothing planned", "Tap Add to put something on ${Fmt.day(selected)}.")
                }
                if (open.isNotEmpty()) {
                    Glass(Modifier.fillMaxWidth()) {
                        open.forEachIndexed { i, r ->
                            if (i > 0) RowDivider()
                            val canTick = r.repeat == Repeat.NONE || r.date == selected || Plan.isOverdue(r, today)
                            ReminderRow(
                                r, today, tickable = canTick,
                                onTick = { onTick(r, if (Plan.isOverdue(r, today)) (r.date ?: selected) else selected) },
                                onOpen = { onOpen(r) },
                            )
                        }
                    }
                }
                if (done.isNotEmpty()) {
                    Section("Done", "${done.size}") {
                        done.forEachIndexed { i, c ->
                            if (i > 0) RowDivider()
                            val r = s.reminders.find { it.id == c.reminderId }
                            DoneRow(c, if (r != null && r.repeat == Repeat.NONE) ({ onUntick(c) }) else null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(s: AppState, d: LocalDate, today: LocalDate, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val count = Plan.countOn(s, d)
    val doneCount = s.history.count { it.date == d }
    val mood = s.moods[d]
    val shape = RoundedCornerShape(12.dp)
    val bg = when {
        selected -> p.accent
        mood != null -> Color(Moods.colors[mood]).copy(alpha = 0.26f)
        else -> Color.Transparent
    }
    Box(
        Modifier
            .weight(1f)
            .aspectRatio(0.88f)
            .padding(2.dp)
            .clip(shape)
            .background(bg)
            .then(if (d == today && !selected) Modifier.border(1.5.dp, p.warm, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${d.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) p.onAccent else if (d.isBefore(today)) p.sub else p.text,
                fontWeight = if (d == today || selected) FontWeight.Bold else FontWeight.Normal,
            )
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(5.dp)) {
                repeat(minOf(count, 3)) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (selected) p.onAccent else p.warm))
                }
                if (count == 0 && doneCount > 0) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (selected) p.onAccent else p.accent))
                }
            }
        }
    }
}
