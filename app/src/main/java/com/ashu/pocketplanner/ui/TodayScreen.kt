package com.ashu.pocketplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ashu.pocketplanner.data.AppState
import com.ashu.pocketplanner.data.Completion
import com.ashu.pocketplanner.data.Reminder
import com.ashu.pocketplanner.data.Repeat
import com.ashu.pocketplanner.logic.EmojiGuess
import com.ashu.pocketplanner.logic.Fmt
import com.ashu.pocketplanner.logic.Plan
import com.ashu.pocketplanner.logic.QuickParse
import java.time.LocalDate
import java.time.LocalTime

private fun greeting(): String = when (LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Night owl mode"
}

@Composable
fun TodayScreen(
    s: AppState,
    today: LocalDate,
    modifier: Modifier,
    quickText: String,
    onQuickText: (String) -> Unit,
    onQuickAdd: (String) -> Unit,
    onTick: (Reminder, LocalDate) -> Unit,
    onUntick: (Completion) -> Unit,
    onOpen: (Reminder) -> Unit,
    onMood: (Int?) -> Unit,
    onSettings: () -> Unit,
) {
    val p = LocalPalette.current
    val overdue = Plan.overdue(s, today)
    val open = Plan.dayOpen(s, today, today)
    val done = Plan.doneOn(s, today)
    val total = overdue.size + open.size + done.size
    val countdowns = Plan.countdowns(s, today)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header(greeting(), Fmt.longDay(today), onSettings) }

        item {
            Glass(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    ProgressRing(done.size, total)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                total == 0 -> "A free day"
                                done.size == total -> "All done. Nice!"
                                else -> "${done.size} of $total done"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = p.text,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("How's today feeling?", style = MaterialTheme.typography.bodySmall, color = p.sub)
                        Spacer(Modifier.width(4.dp))
                        MoodPicker(s.moods[today], onMood)
                    }
                }
            }
        }

        if (countdowns.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(countdowns, key = { it.first.id }) { (r, days) -> CountdownCard(r, days) { onOpen(r) } }
                }
            }
        }

        item { QuickAddBar(quickText, onQuickText, today, onQuickAdd) }

        if (overdue.isNotEmpty()) {
            item {
                Section("Overdue", "${overdue.size}", color = p.danger) {
                    overdue.forEachIndexed { i, r ->
                        if (i > 0) RowDivider()
                        ReminderRow(r, today, tickable = true, onTick = { onTick(r, r.date ?: today) }, onOpen = { onOpen(r) })
                    }
                }
            }
        }

        item {
            if (open.isNotEmpty()) {
                Section("Today", "${open.size} to do") {
                    open.forEachIndexed { i, r ->
                        if (i > 0) RowDivider()
                        ReminderRow(r, today, tickable = true, onTick = { onTick(r, today) }, onOpen = { onOpen(r) })
                    }
                }
            } else if (overdue.isEmpty()) {
                EmptyCard(
                    title = if (done.isEmpty()) "Nothing due today" else "Day cleared ✨",
                    body = if (done.isEmpty()) "Type above in plain words, or tap one of these to try it." else "Everything for today is done. Enjoy it.",
                    chips = if (done.isEmpty()) listOf("Drink water every day at 9am", "Call home tomorrow at 7pm", "Pay rent on 1st dec") else emptyList(),
                    onChip = onQuickText,
                )
            }
        }

        if (done.isNotEmpty()) {
            item {
                Section("Done today", "${done.size}") {
                    done.forEachIndexed { i, c ->
                        if (i > 0) RowDivider()
                        val r = s.reminders.find { it.id == c.reminderId }
                        val canUndo = r != null && r.repeat == Repeat.NONE
                        DoneRow(c, if (canUndo) ({ onUntick(c) }) else null)
                    }
                }
            }
        }
    }
}

@Composable
private fun CountdownCard(r: Reminder, days: Long, onClick: () -> Unit) {
    val p = LocalPalette.current
    Glass(Modifier.width(150.dp), shape = RoundedCornerShape(20.dp)) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text("${r.emoji} ${r.title}".trim(), style = MaterialTheme.typography.bodyMedium, color = p.text, maxLines = 1, fontWeight = FontWeight.Medium)
            Text(
                when (days) {
                    0L -> "Today!"
                    1L -> "Tomorrow"
                    else -> "$days days"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = p.warm,
                fontWeight = FontWeight.Bold,
            )
            Text(r.date?.let { Fmt.day(it) } ?: "", style = MaterialTheme.typography.labelSmall, color = p.sub)
        }
    }
}

@Composable
private fun QuickAddBar(text: String, onText: (String) -> Unit, today: LocalDate, onAdd: (String) -> Unit) {
    val p = LocalPalette.current
    val parsed = remember(text, today) { if (text.isBlank()) null else QuickParse.parse(text, today, today) }
    fun submit() {
        if (text.isNotBlank()) onAdd(text.trim())
    }
    Glass(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = text,
                onValueChange = onText,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Call mom tomorrow at 7pm", color = p.sub) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = p.text,
                    unfocusedTextColor = p.text,
                    cursorColor = p.accent,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
            FilledIconButton(
                onClick = { submit() },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = p.accent, contentColor = p.onAccent),
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add")
            }
        }
        if (parsed != null) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Hint(EmojiGuess.guess(parsed.title) + " " + parsed.title.ifBlank { "…" })
                Hint("📅 " + if (parsed.date == today) "Today" else Fmt.day(parsed.date))
                parsed.time?.let { Hint("⏰ " + Fmt.time(it)) }
                if (parsed.repeat != Repeat.NONE) Hint("↻ " + parsed.repeat.label.lowercase())
                if (parsed.urgent) Hint("🔥 urgent")
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    val p = LocalPalette.current
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(p.warm.copy(alpha = 0.16f))
            .border(1.dp, p.warm.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = p.text, maxLines = 1)
    }
}
