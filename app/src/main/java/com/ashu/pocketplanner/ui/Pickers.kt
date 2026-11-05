package com.ashu.pocketplanner.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashu.pocketplanner.logic.Fmt
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.math.abs

/**
 * One scrolling drum column. Two blank rows above and below let the first and last items
 * reach the middle; the middle row is the selected one.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Wheel(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 44.dp,
) {
    val p = LocalPalette.current
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected.coerceIn(0, labels.lastIndex))
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val center by remember {
        derivedStateOf {
            (state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0)
                .coerceIn(0, labels.lastIndex)
        }
    }
    LaunchedEffect(center) { if (center != selected) onSelect(center) }
    LaunchedEffect(selected) {
        if (!state.isScrollInProgress && center != selected) state.animateScrollToItem(selected.coerceIn(0, labels.lastIndex))
    }
    Box(modifier.height(itemHeight * 5)) {
        LazyColumn(state = state, flingBehavior = fling, modifier = Modifier.fillMaxSize()) {
            items(labels.size + 4) { li ->
                val i = li - 2
                Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                    if (i in labels.indices) {
                        val dist = abs(i - center)
                        Text(
                            labels[i],
                            fontSize = if (dist == 0) 26.sp else 20.sp,
                            fontWeight = if (dist == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (dist == 0) p.text else p.sub,
                            fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                            modifier = Modifier
                                .graphicsLayer { alpha = when (dist) { 0 -> 1f; 1 -> 0.6f; else -> 0.28f } }
                                .clickable { onSelect(i) },
                        )
                    }
                }
            }
        }
    }
}

private val HOURS = (1..12).map { "$it" }
private val MINUTES = (0..59).map { if (it < 10) "0$it" else "$it" }
private val HALVES = listOf("am", "pm")

/** Hour / minute / am-pm wheels, like an alarm clock. */
@Composable
fun TimeWheel(time: LocalTime, onChange: (LocalTime) -> Unit) {
    val p = LocalPalette.current
    val h12 = if (time.hour % 12 == 0) 12 else time.hour % 12
    val pm = time.hour >= 12
    fun build(hour12: Int, minute: Int, isPm: Boolean): LocalTime {
        val h = (hour12 % 12) + if (isPm) 12 else 0
        return LocalTime.of(h, minute)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Fmt.time(time), style = MaterialTheme.typography.headlineMedium, color = p.text, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(p.accent.copy(alpha = 0.16f))
                    .border(1.dp, p.accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Wheel(HOURS, h12 - 1, { onChange(build(it + 1, time.minute, pm)) }, Modifier.weight(1f))
                Text(":", fontSize = 26.sp, color = p.text, fontWeight = FontWeight.Bold)
                Wheel(MINUTES, time.minute, { onChange(build(h12, it, pm)) }, Modifier.weight(1f))
                Wheel(HALVES, if (pm) 1 else 0, { onChange(build(h12, time.minute, it == 1)) }, Modifier.weight(1f))
            }
        }
    }
}

/** A month grid for picking a day, shown right inside the add sheet. */
@Composable
fun MiniCalendar(selected: LocalDate?, today: LocalDate, onSelect: (LocalDate) -> Unit) {
    val p = LocalPalette.current
    var month by remember { mutableStateOf(YearMonth.from(selected ?: today)) }
    LaunchedEffect(selected) { if (selected != null) month = YearMonth.from(selected) }
    Glass(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month", tint = p.text)
                }
                Text(
                    Fmt.month(month),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    color = p.text,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = { month = month.plusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month", tint = p.text)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                    Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium, color = p.sub)
                }
            }
            val offset = month.atDay(1).dayOfWeek.value % 7
            val days = month.lengthOfMonth()
            val rows = (offset + days + 6) / 7
            for (w in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val n = w * 7 + c - offset + 1
                        if (n < 1 || n > days) {
                            Spacer(Modifier.weight(1f).aspectRatio(1.15f))
                        } else {
                            val d = month.atDay(n)
                            val on = d == selected
                            val shape = RoundedCornerShape(12.dp)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1.15f)
                                    .padding(2.dp)
                                    .clip(shape)
                                    .background(if (on) p.accent else Color.Transparent)
                                    .then(if (d == today && !on) Modifier.border(1.5.dp, p.warm, shape) else Modifier)
                                    .clickable { onSelect(d) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "$n",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (on || d == today) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        on -> p.onAccent
                                        d.isBefore(today) -> p.sub.copy(alpha = 0.45f)
                                        else -> p.text
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
