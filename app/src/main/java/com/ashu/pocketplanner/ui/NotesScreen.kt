package com.ashu.pocketplanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashu.pocketplanner.data.AppState
import com.ashu.pocketplanner.data.Reminder
import com.ashu.pocketplanner.logic.Plan

@Composable
fun NotesScreen(
    s: AppState,
    modifier: Modifier,
    onOpen: (Reminder) -> Unit,
    onNew: () -> Unit,
    onSettings: () -> Unit,
) {
    val p = LocalPalette.current
    val notes = Plan.notes(s)
    LazyVerticalGrid(
        columns = GridCells.Adaptive(158.dp),
        modifier = modifier,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Header("Notes", if (notes.isEmpty()) "Things to keep, no date" else "${notes.size} saved", onSettings)
        }
        if (notes.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyCard(
                    "Your notes live here",
                    "Lists, ideas, things to buy. Notes have no date, so they never nag you.",
                    listOf("Start a note"),
                ) { onNew() }
            }
        }
        items(notes, key = { it.id }) { n ->
            val shape = RoundedCornerShape(22.dp)
            Glass(
                Modifier.fillMaxWidth().heightIn(min = 120.dp).clip(shape).clickable { onOpen(n) },
                shape = shape,
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(n.emoji.ifBlank { "📝" }, fontSize = 24.sp)
                    Text(
                        n.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = p.text,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (n.note.isNotBlank()) {
                        Text(
                            n.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = p.sub,
                            maxLines = 7,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
