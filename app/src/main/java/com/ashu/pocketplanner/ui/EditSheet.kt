@file:OptIn(ExperimentalMaterial3Api::class)

package com.ashu.pocketplanner.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashu.pocketplanner.data.Reminder
import com.ashu.pocketplanner.data.Repeat
import com.ashu.pocketplanner.logic.EmojiGuess
import com.ashu.pocketplanner.logic.Fmt
import com.ashu.pocketplanner.logic.Plan
import java.time.LocalDate
import java.time.LocalTime

private const val NO_EMOJI = "none"

sealed interface Editing {
    data class New(val date: LocalDate?) : Editing
    data class Edit(val r: Reminder) : Editing
}

private val EMOJIS = listOf(
    "📌", "📞", "📝", "📚", "🧪", "💻", "🏋️", "🏃", "🧘", "💧", "💊", "🩺", "💸", "🛒", "🍽️", "☕",
    "🎂", "🎉", "🎁", "🎬", "🎵", "🎮", "✈️", "🧳", "🏠", "🧺", "🪴", "🙏", "✉️", "🤝", "⏰", "💡",
    "❤️", "⭐", "🔥", "🎯", "😴", "🐶", "⚽", "🏏",
)


@Composable
fun EditSheet(
    editing: Editing,
    today: LocalDate,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit,
    onDelete: (Reminder) -> Unit,
) {
    val p = LocalPalette.current
    val orig = (editing as? Editing.Edit)?.r
    val startDate = when (editing) {
        is Editing.Edit -> editing.r.date
        is Editing.New -> editing.date
    }
    var title by remember { mutableStateOf(orig?.title ?: "") }
    var note by remember { mutableStateOf(orig?.note ?: "") }
    var pickedEmoji by remember { mutableStateOf(if (orig != null && orig.emoji.isBlank()) NO_EMOJI else orig?.emoji ?: "") }
    var date by remember { mutableStateOf(startDate) }
    var time by remember { mutableStateOf(orig?.time) }
    var repeat by remember { mutableStateOf(orig?.repeat ?: Repeat.NONE) }
    var urgent by remember { mutableStateOf(orig?.urgent ?: false) }
    var countdown by remember { mutableStateOf(orig?.countdown ?: false) }
    var showEmoji by remember { mutableStateOf(false) }
    var openRow by remember { mutableStateOf<String?>(null) }

    val emoji = when (pickedEmoji) {
        NO_EMOJI -> ""
        "" -> EmojiGuess.guess(title)
        else -> pickedEmoji
    }
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = p.accent,
        unfocusedBorderColor = p.border,
        focusedTextColor = p.text,
        unfocusedTextColor = p.text,
        cursorColor = p.accent,
        focusedPlaceholderColor = p.sub,
        unfocusedPlaceholderColor = p.sub,
    )

    fun save() {
        if (title.isBlank()) return
        val d = date
        val now = System.currentTimeMillis()
        val base = orig ?: Reminder(id = Plan.newId(), title = "", createdAt = now)
        onSave(
            base.copy(
                title = title.trim(),
                note = note.trim(),
                emoji = emoji,
                date = d,
                time = if (d == null) null else time,
                repeat = if (d == null) Repeat.NONE else repeat,
                anchorDay = d?.dayOfMonth ?: 1,
                urgent = urgent,
                countdown = d != null && countdown,
                done = if (orig != null && orig.date == d) orig.done else false,
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = p.sheet,
        contentColor = p.text,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                when {
                    orig != null -> if (date == null) "Edit note" else "Edit reminder"
                    date == null -> "New note"
                    else -> "New reminder"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = p.text,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(p.glassHi)
                        .border(1.dp, p.border, RoundedCornerShape(18.dp))
                        .clickable { showEmoji = !showEmoji },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emoji.ifBlank { "∅" }, fontSize = if (emoji.isBlank()) 22.sp else 28.sp, color = p.sub)
                }
                Spacer(Modifier.width(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("What's it about?") },
                    maxLines = 3,
                    colors = fieldColors,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
            }

            if (showEmoji) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    EMOJIS.chunked(EMOJIS.size / 2).forEach { rowList ->
                        Row(Modifier.horizontalScroll(rememberScrollState())) {
                            rowList.forEach { e ->
                                Box(
                                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable {
                                        pickedEmoji = e
                                        showEmoji = false
                                    },
                                    contentAlignment = Alignment.Center,
                                ) { Text(e, fontSize = 24.sp) }
                            }
                        }
                    }
                    Row {
                        TextButton(onClick = { pickedEmoji = NO_EMOJI; showEmoji = false }) { Text("No emoji", color = p.sub) }
                        TextButton(onClick = { pickedEmoji = ""; showEmoji = false }) { Text("Pick automatically", color = p.accent) }
                    }
                }
            }

            DropRow(
                icon = "📅",
                label = "Date",
                value = date?.let { "${Fmt.day(it)} · ${Fmt.rel(it, today)}" } ?: "No date (note)",
                expanded = openRow == "date",
                onToggle = { openRow = if (openRow == "date") null else "date" },
            ) {
                MiniCalendar(date, today, framed = false) {
                    date = it
                    openRow = "time"
                    if (time == null) time = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                }
                Row {
                    TextButton(onClick = { date = null; time = null; openRow = null }) { Text("No date, keep as a note", color = p.sub) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { if (date == null) date = today; openRow = null }) { Text("Done", color = p.accent, fontWeight = FontWeight.Bold) }
                }
            }

            val chosen = date
            if (chosen != null) {
                DropRow(
                    icon = "⏰",
                    label = "Time",
                    value = time?.let { Fmt.time(it) } ?: "No time",
                    expanded = openRow == "time",
                    onToggle = {
                        openRow = if (openRow == "time") null else "time"
                        if (openRow == "time" && time == null) time = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
                    },
                ) {
                    val t = time
                    if (t != null) TimeWheel(t) { time = it }
                    Row {
                        TextButton(onClick = { time = null; openRow = null }) { Text("No time", color = p.sub) }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = { openRow = null }) { Text("Done", color = p.accent, fontWeight = FontWeight.Bold) }
                    }
                }

                Label("Repeat")
                ChipRow {
                    Repeat.entries.forEach { rp -> GlassChip(rp.label, repeat == rp) { repeat = rp } }
                }

                Glass(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        ToggleRow("🔥 Urgent", "Floats to the top of your list", urgent) { urgent = it }
                        ToggleRow("🎯 Countdown", "Shows the days left on Today and the widget", countdown) { countdown = it }
                    }
                }
            }

            Label("Notes")
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (date == null) "Write your note…" else "Anything else to remember") },
                minLines = 3,
                colors = fieldColors,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (orig != null) {
                    OutlinedButton(onClick = { onDelete(orig) }) { Text("Delete", color = p.danger) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("Cancel", color = p.sub) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { save() },
                    enabled = title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
                ) { Text("Save", fontWeight = FontWeight.SemiBold) }
            }
        }
    }

}

/** A row that opens like a drop-down menu: tap to show the picker under it. */
@Composable
private fun DropRow(
    icon: String,
    label: String,
    value: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val p = LocalPalette.current
    val turn by androidx.compose.animation.core.animateFloatAsState(if (expanded) 180f else 0f, label = "arrow")
    Glass(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = p.sub, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.bodyLarge, color = p.text, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Close" else "Open",
                tint = p.sub,
                modifier = Modifier.graphicsLayer { rotationZ = turn },
            )
        }
        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
            Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 6.dp)) { content() }
        }
    }
}
