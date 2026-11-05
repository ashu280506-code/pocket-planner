package com.ashu.pocketplanner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ashu.pocketplanner.data.Completion
import com.ashu.pocketplanner.data.Reminder
import com.ashu.pocketplanner.data.Repo
import com.ashu.pocketplanner.data.Settings
import com.ashu.pocketplanner.data.Snapshot
import com.ashu.pocketplanner.fx.GlyphFx
import com.ashu.pocketplanner.fx.Photos
import com.ashu.pocketplanner.fx.Sound
import com.ashu.pocketplanner.logic.EmojiGuess
import com.ashu.pocketplanner.logic.Fmt
import com.ashu.pocketplanner.logic.Plan
import com.ashu.pocketplanner.logic.QuickParse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun rememberToday(): LocalDate {
    var d by remember { mutableStateOf(LocalDate.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            val n = LocalDate.now()
            if (n != d) d = n
        }
    }
    return d
}

@Composable
fun rememberAppPhoto(s: Settings): ImageBitmap? {
    val ctx = LocalContext.current
    return remember(s.photoVersion) {
        if (s.photoVersion == 0L) null else Photos.loadApp(ctx)?.asImageBitmap()
    }
}

@Composable
private fun AskForNotifications(asked: Boolean, onAsked: () -> Unit) {
    if (Build.VERSION.SDK_INT < 33 || asked) return
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onAsked() }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) onAsked()
        else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
fun PlannerApp(repo: Repo, addKey: Int) {
    val s by repo.state.collectAsState()
    if (!s.settings.onboarded) {
        PlannerTheme(s.settings) {
            WelcomeScreen(s.settings) { n -> repo.settings { it.copy(name = n, onboarded = true) } }
        }
        return
    }
    PlannerTheme(s.settings) {
        val p = LocalPalette.current
        val scope = rememberCoroutineScope()
        val view = LocalView.current
        val today = rememberToday()
        val photo = rememberAppPhoto(s.settings)

        var tab by rememberSaveable { mutableIntStateOf(0) }
        var settingsOpen by rememberSaveable { mutableStateOf(false) }
        var editing by remember { mutableStateOf<Editing?>(null) }
        var shot by remember { mutableStateOf(Shot(0, false)) }
        var calDay by remember { mutableStateOf(LocalDate.now()) }
        var quickText by rememberSaveable { mutableStateOf("") }
        val snack = remember { SnackbarHostState() }

        LaunchedEffect(addKey) { if (addKey > 0) editing = Editing.New(LocalDate.now()) }
        LaunchedEffect(Unit) {
            delay(900)
            if (s.settings.glyph) GlyphFx.progress(Plan.todayProgress(s, LocalDate.now()))
        }
        if (s.settings.onboarded) AskForNotifications(s.settings.askedNotif) { repo.settings { it.copy(askedNotif = true) } }

        fun say(msg: String, undo: Snapshot? = null) {
            scope.launch {
                snack.currentSnackbarData?.dismiss()
                val res = snack.showSnackbar(msg, actionLabel = if (undo != null) "Undo" else null, duration = SnackbarDuration.Short)
                if (res == SnackbarResult.ActionPerformed && undo != null) repo.restore(undo)
            }
        }

        fun buzz() {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
            )
        }

        fun tick(r: Reminder, day: LocalDate) {
            val res = repo.tick(r.id, day) ?: return
            val st = repo.state.value.settings
            if (st.sound) { if (res.dayCleared) Sound.chime() else Sound.pop() }
            buzz()
            if (st.confetti) shot = Shot(shot.id + 1, res.dayCleared)
            if (st.glyph) {
                if (res.dayCleared) GlyphFx.celebrate() else {
                    GlyphFx.flash()
                    GlyphFx.progress(res.progress)
                }
            }
            val next = res.next
            say(
                when {
                    res.dayCleared -> "Day cleared! 🎉"
                    next != null -> "Done · next ${Fmt.day(next)}"
                    else -> "Done ✓"
                },
                res.before,
            )
        }

        fun untick(c: Completion) {
            repo.untick(c.id)
        }

        fun quickAdd(text: String) {
            val q = QuickParse.parse(text, LocalDate.now())
            if (q.title.isBlank()) {
                say("Add a few words about what to do")
                return
            }
            val now = System.currentTimeMillis()
            repo.upsert(
                Reminder(
                    id = Plan.newId(),
                    title = q.title,
                    emoji = EmojiGuess.guess(q.title),
                    date = q.date,
                    time = q.time,
                    repeat = q.repeat,
                    anchorDay = q.date.dayOfMonth,
                    urgent = q.urgent,
                    createdAt = now,
                )
            )
            quickText = ""
            buzz()
            say(if (q.date == LocalDate.now()) "Added for today" else "Added for ${Fmt.day(q.date)}")
        }

        Box(Modifier.fillMaxSize()) {
            AppBackground(s.settings, photo)
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = p.text,
                snackbarHost = {
                    SnackbarHost(snack) { data ->
                        Snackbar(data, containerColor = p.sheet, contentColor = p.text, actionColor = p.warm)
                    }
                },
                bottomBar = { NavBar(tab) { tab = it } },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            editing = Editing.New(
                                when (tab) {
                                    1 -> calDay
                                    2 -> null
                                    else -> LocalDate.now()
                                }
                            )
                        },
                        containerColor = p.warm,
                        contentColor = Color(0xFF2A1600),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add")
                    }
                },
            ) { pad ->
                val mod = Modifier.fillMaxSize().padding(pad)
                when (tab) {
                    0 -> TodayScreen(
                        s, today, mod,
                        name = s.settings.name,
                        quickText = quickText,
                        onQuickText = { quickText = it },
                        onQuickAdd = { quickAdd(it) },
                        onTick = { r, d -> tick(r, d) },
                        onUntick = { untick(it) },
                        onOpen = { editing = Editing.Edit(it) },
                        onMood = { repo.setMood(LocalDate.now(), it) },
                        onRemark = { repo.setRemark(LocalDate.now(), it) },
                        onSettings = { settingsOpen = true },
                    )
                    1 -> CalendarScreen(
                        s, today, calDay, mod,
                        onSelect = { calDay = it },
                        onTick = { r, d -> tick(r, d) },
                        onUntick = { untick(it) },
                        onOpen = { editing = Editing.Edit(it) },
                        onAdd = { editing = Editing.New(it) },
                        onMood = { d, m -> repo.setMood(d, m) },
                        onRemark = { d, t -> repo.setRemark(d, t) },
                        onSettings = { settingsOpen = true },
                    )
                    2 -> NotesScreen(
                        s, mod,
                        onOpen = { editing = Editing.Edit(it) },
                        onNew = { editing = Editing.New(null) },
                        onSettings = { settingsOpen = true },
                    )
                    else -> InsightsScreen(
                        s, today, mod,
                        onOpen = { editing = Editing.Edit(it) },
                        onSettings = { settingsOpen = true },
                    )
                }
            }
            ConfettiOverlay(shot)
            if (settingsOpen) SettingsScreen(s, repo, photo) { settingsOpen = false }
        }

        editing?.let { e ->
            EditSheet(
                editing = e,
                today = today,
                onDismiss = { editing = null },
                onSave = { r ->
                    repo.upsert(r)
                    editing = null
                    val d = r.date
                    say(
                        when {
                            d == null -> "Note saved"
                            d == LocalDate.now() -> "Saved for today"
                            else -> "Saved for ${Fmt.day(d)}"
                        }
                    )
                },
                onDelete = { r ->
                    val before = repo.delete(r.id)
                    editing = null
                    say("Deleted", before)
                },
            )
        }
    }
}

@Composable
private fun NavBar(tab: Int, onTab: (Int) -> Unit) {
    val p = LocalPalette.current
    val items: List<Pair<String, ImageVector>> = listOf(
        "Today" to Icons.Filled.Home,
        "Calendar" to Icons.Filled.DateRange,
        "Notes" to Icons.Filled.Edit,
        "Insights" to Icons.Filled.Star,
    )
    val shape = RoundedCornerShape(26.dp)
    Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(p.sheet.copy(alpha = 0.88f))
                .border(1.dp, p.border, shape)
                .padding(6.dp),
        ) {
            items.forEachIndexed { i, (label, icon) ->
                val sel = i == tab
                Column(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (sel) p.accent.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable { onTab(i) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(icon, contentDescription = label, tint = if (sel) p.accent else p.sub, modifier = Modifier.size(22.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall, color = if (sel) p.accent else p.sub, maxLines = 1)
                }
            }
        }
    }
}
