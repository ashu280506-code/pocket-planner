package com.ashu.pocketplanner.widget

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.ashu.pocketplanner.R
import com.ashu.pocketplanner.data.AppState
import com.ashu.pocketplanner.data.Completion
import com.ashu.pocketplanner.data.FontChoice
import com.ashu.pocketplanner.data.Moods
import com.ashu.pocketplanner.data.Reminder
import com.ashu.pocketplanner.data.Repo
import com.ashu.pocketplanner.data.WidgetSkin
import com.ashu.pocketplanner.fx.Photos
import com.ashu.pocketplanner.fx.Sound
import com.ashu.pocketplanner.logic.Fmt
import com.ashu.pocketplanner.logic.Plan
import com.ashu.pocketplanner.ui.MainActivity
import java.time.LocalDate

private val White = Color(0xFFF4F2FF)
private val Sub = Color(0xB8F4F2FF)
private val Faint = Color(0x80F4F2FF)
private val Urgent = Color(0xFFFF8A9A)
private val Gold = Color(0xFFFFD28A)
private val PlusInk = Color(0xFF2A1600)

val KEY_ID = ActionParameters.Key<String>("id")
val KEY_DATE = ActionParameters.Key<String>("date")
val KEY_MOOD = ActionParameters.Key<Int>("mood")

class PocketWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(120.dp, 60.dp),   // 2x1 / 4x1 strip
            DpSize(120.dp, 120.dp),  // 2x2
            DpSize(260.dp, 60.dp),   // 4x1
            DpSize(260.dp, 120.dp),  // 4x2 (main)
            DpSize(260.dp, 190.dp),  // 4x3
            DpSize(260.dp, 260.dp),  // 4x4
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = Repo.get(context)
        provideContent {
            val s by repo.state.collectAsState()
            WidgetBody(s)
        }
    }
}

class PocketWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PocketWidget()
}

class TickAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[KEY_ID] ?: return
        val day = parameters[KEY_DATE]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        val repo = Repo.get(context)
        if (repo.state.value.settings.sound) Sound.pop()
        repo.tickNow(id, day)
    }
}

class UntickAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[KEY_ID] ?: return
        Repo.get(context).untickNow(id)
    }
}

class MoodAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val mood = parameters[KEY_MOOD] ?: return
        val repo = Repo.get(context)
        val today = LocalDate.now()
        val current = repo.state.value.moods[today]
        repo.setMoodNow(today, if (current == mood) null else mood)
    }
}

private fun family(f: FontChoice): FontFamily = when (f) {
    FontChoice.CLEAN, FontChoice.ROUNDED -> FontFamily.SansSerif
    FontChoice.HAND -> FontFamily.Cursive
    FontChoice.DOT -> FontFamily.Monospace
    FontChoice.RETRO -> FontFamily.Serif
}

private fun ts(
    size: Int,
    color: Color,
    fam: FontFamily,
    bold: Boolean = false,
    strike: Boolean = false,
    align: TextAlign? = null,
) = TextStyle(
    color = ColorProvider(color),
    fontSize = size.sp,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontFamily = fam,
    textDecoration = if (strike) TextDecoration.LineThrough else null,
    textAlign = align,
)

@Composable
private fun WidgetBody(s: AppState) {
    val ctx = LocalContext.current
    val size = LocalSize.current
    val today = LocalDate.now()
    val open = Plan.todayOpen(s, today)
    val done = Plan.doneOn(s, today)
    val fam = family(s.settings.widgetFont)
    val skin = s.settings.widgetSkin
    val photoVersion = s.settings.widgetPhotoVersion
    val photo: Bitmap? = remember(skin, photoVersion) {
        if (skin == WidgetSkin.PHOTO && photoVersion != 0L) Photos.loadWidget(ctx) else null
    }

    val openApp = actionStartActivity(
        Intent(ctx, MainActivity::class.java)
            .setData(Uri.parse("pocketplanner://open"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )
    val addNew = actionStartActivity(
        Intent(ctx, MainActivity::class.java)
            .setData(Uri.parse("pocketplanner://add"))
            .putExtra(MainActivity.EXTRA_ADD, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    )

    val base = GlanceModifier.fillMaxSize().cornerRadius(26.dp)
    val bg = when {
        photo != null -> base.background(Color(0xFF15122A))
        skin == WidgetSkin.DARK -> base.background(ImageProvider(R.drawable.widget_bg_dark))
        else -> base.background(ImageProvider(R.drawable.widget_bg_glass))
    }

    Box(modifier = bg) {
        if (photo != null) {
            Image(
                provider = ImageProvider(photo),
                contentDescription = null,
                modifier = GlanceModifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        val w = size.width.value
        val h = size.height.value
        when {
            h < 100f -> Slim(open, today, fam, openApp, addNew)
            w < 200f -> Small(open, done, today, fam, openApp, addNew)
            else -> Full(s, open, done, today, fam, h, openApp, addNew)
        }
    }
}

@Composable
private fun PlusButton(add: Action, diameter: Int = 36) {
    Box(
        modifier = GlanceModifier.size(diameter.dp).background(ImageProvider(R.drawable.w_plus)).clickable(add),
        contentAlignment = Alignment.Center,
    ) {
        Text("+", style = TextStyle(color = ColorProvider(PlusInk), fontSize = 22.sp, fontWeight = FontWeight.Bold))
    }
}

private fun tickAction(r: Reminder, today: LocalDate) = actionRunCallback<TickAction>(
    actionParametersOf(KEY_ID to r.id, KEY_DATE to (r.date ?: today).let { if (it.isAfter(today)) today else it }.toString())
)

@Composable
private fun ItemRow(r: Reminder, today: LocalDate, fam: FontFamily) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier.size(28.dp).clickable(tickAction(r, today)),
            contentAlignment = Alignment.Center,
        ) {
            Image(ImageProvider(R.drawable.w_ring), contentDescription = "Mark done", modifier = GlanceModifier.size(19.dp))
        }
        Spacer(GlanceModifier.width(6.dp))
        Text(
            text = "${r.emoji} ${r.title}".trim(),
            modifier = GlanceModifier.defaultWeight(),
            style = ts(14, White, fam),
            maxLines = 1,
        )
        if (r.urgent) Text(" !", style = ts(14, Urgent, fam, bold = true))
        val d = r.date
        val late = d != null && d.isBefore(today)
        val t = r.time
        val right = when {
            late && d != null -> Fmt.late(d, today)
            t != null -> Fmt.time(t)
            else -> ""
        }
        if (right.isNotEmpty()) {
            Spacer(GlanceModifier.width(6.dp))
            Text(right, style = ts(11, if (late) Urgent else Sub, fam))
        }
    }
}

@Composable
private fun DoneRow(c: Completion, fam: FontFamily) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier.size(28.dp).clickable(actionRunCallback<UntickAction>(actionParametersOf(KEY_ID to c.id))),
            contentAlignment = Alignment.Center,
        ) {
            Image(ImageProvider(R.drawable.w_ring_done), contentDescription = "Mark not done", modifier = GlanceModifier.size(19.dp))
        }
        Spacer(GlanceModifier.width(6.dp))
        Text("${c.emoji} ${c.title}".trim(), style = ts(14, Faint, fam, strike = true), maxLines = 1)
    }
}

/** 4x1 and other one-row sizes: the next thing to do. */
@Composable
private fun Slim(open: List<Reminder>, today: LocalDate, fam: FontFamily, openApp: Action, add: Action) {
    Row(
        modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val first = open.firstOrNull()
        if (first != null) {
            Row(modifier = GlanceModifier.defaultWeight(), verticalAlignment = Alignment.CenterVertically) {
                ItemRow(first, today, fam)
            }
            if (open.size > 1) Text(" +${open.size - 1}", style = ts(12, Sub, fam))
        } else {
            Text(
                "All clear today ✨",
                modifier = GlanceModifier.defaultWeight().clickable(openApp),
                style = ts(14, White, fam),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.width(8.dp))
        PlusButton(add, 34)
    }
}

/** 2x2: how many are left and what's next. */
@Composable
private fun Small(open: List<Reminder>, done: List<Completion>, today: LocalDate, fam: FontFamily, openApp: Action, add: Action) {
    Box(modifier = GlanceModifier.fillMaxSize()) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(14.dp).clickable(openApp)) {
            Text(Fmt.day(today), style = ts(12, Sub, fam))
            Text(if (open.isEmpty()) "✨" else "${open.size}", style = ts(34, White, fam, bold = true))
            Text(
                when {
                    open.isEmpty() && done.isNotEmpty() -> "all done"
                    open.isEmpty() -> "free day"
                    else -> "left · ${done.size} done"
                },
                style = ts(11, Sub, fam),
            )
            Spacer(GlanceModifier.defaultWeight())
            val next = open.firstOrNull()
            if (next != null) {
                Text(
                    "${next.emoji} ${next.title}".trim(),
                    style = ts(12, White, fam),
                    maxLines = 1,
                    modifier = GlanceModifier.padding(end = 40.dp),
                )
            }
        }
        Box(modifier = GlanceModifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomEnd) {
            PlusButton(add, 32)
        }
    }
}

/** 4x2, 4x3, 4x4: the list, countdown, and on the biggest size the week strip and mood. */
@Composable
private fun Full(
    s: AppState,
    open: List<Reminder>,
    done: List<Completion>,
    today: LocalDate,
    fam: FontFamily,
    h: Float,
    openApp: Action,
    add: Action,
) {
    val tall = h >= 250f
    val countdown = Plan.countdowns(s, today).firstOrNull()
    val total = open.size + done.size
    val reserved = 44f + (if (countdown != null) 30f else 0f) + (if (tall) 96f else 0f)
    val rows = ((h - reserved) / 26f).toInt().coerceIn(1, 8)

    Box(modifier = GlanceModifier.fillMaxSize()) {
        Column(modifier = GlanceModifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().clickable(openApp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Fmt.day(today), modifier = GlanceModifier.defaultWeight(), style = ts(16, White, fam, bold = true))
                Text(
                    if (total == 0) "Free day" else "${done.size} of $total done",
                    style = ts(11, Sub, fam),
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            if (tall) {
                WeekStrip(s, today, fam)
                Spacer(GlanceModifier.height(6.dp))
            }
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                if (open.isEmpty() && done.isEmpty()) {
                    Text(
                        "Nothing today. Tap + to add something ✨",
                        style = ts(13, Sub, fam),
                        modifier = GlanceModifier.clickable(add),
                    )
                } else {
                    val shown = open.take(rows)
                    shown.forEach { ItemRow(it, today, fam) }
                    val spare = rows - shown.size
                    if (spare > 0) done.takeLast(spare).forEach { DoneRow(it, fam) }
                    if (open.size > rows) Text("+${open.size - rows} more", style = ts(11, Sub, fam), modifier = GlanceModifier.clickable(openApp))
                }
            }
            Spacer(GlanceModifier.defaultWeight())
            if (countdown != null) {
                Text(
                    "🎯 " + Fmt.countdown(countdown.first.title, countdown.second),
                    style = ts(11, Gold, fam),
                    maxLines = 1,
                    modifier = GlanceModifier.background(ImageProvider(R.drawable.w_pill)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            if (tall) {
                Spacer(GlanceModifier.height(6.dp))
                MoodRow(s.moods[today], fam)
            }
        }
        Box(modifier = GlanceModifier.fillMaxSize().padding(10.dp), contentAlignment = Alignment.BottomEnd) {
            PlusButton(add)
        }
    }
}

@Composable
private fun WeekStrip(s: AppState, today: LocalDate, fam: FontFamily) {
    Row(modifier = GlanceModifier.fillMaxWidth()) {
        for (i in 0 until 7) {
            val d = today.plusDays(i.toLong())
            val n = Plan.countOn(s, d) + (if (i == 0) Plan.overdue(s, today).size else 0)
            val mod = if (i == 0) GlanceModifier.defaultWeight().background(ImageProvider(R.drawable.w_chip)) else GlanceModifier.defaultWeight()
            Column(modifier = mod.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.day(d).take(2), style = ts(10, Sub, fam, align = TextAlign.Center))
                Text("${d.dayOfMonth}", style = ts(13, White, fam, bold = true, align = TextAlign.Center))
                Text(if (n == 0) " " else "•".repeat(minOf(n, 3)), style = ts(10, Gold, fam, align = TextAlign.Center))
            }
        }
    }
}

@Composable
private fun MoodRow(selected: Int?, fam: FontFamily) {
    Row(modifier = GlanceModifier.padding(end = 44.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Mood ", style = ts(11, Sub, fam))
        Moods.emojis.forEachIndexed { i, e ->
            val mod = GlanceModifier.size(30.dp).clickable(actionRunCallback<MoodAction>(actionParametersOf(KEY_MOOD to i)))
            Box(
                modifier = if (selected == i) mod.background(ImageProvider(R.drawable.w_chip)) else mod,
                contentAlignment = Alignment.Center,
            ) {
                Text(e, style = TextStyle(fontSize = if (selected == i) 19.sp else 16.sp))
            }
        }
    }
}
