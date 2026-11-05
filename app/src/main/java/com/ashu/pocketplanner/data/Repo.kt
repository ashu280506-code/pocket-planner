package com.ashu.pocketplanner.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.ashu.pocketplanner.logic.Plan
import com.ashu.pocketplanner.notify.Alarms
import com.ashu.pocketplanner.widget.PocketWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

data class Snapshot(val reminders: List<Reminder>, val history: List<Completion>)

data class TickResult(
    val before: Snapshot,
    /** Next date for a repeating reminder, null for one-off ones. */
    val next: LocalDate?,
    val dayCleared: Boolean,
    val progress: Int,
)

/** Single source of truth shared by the app, the widget and notifications. */
class Repo private constructor(private val app: Context) {

    private val file = File(app.filesDir, "planner.json")
    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Mutex()

    companion object {
        @Volatile private var instance: Repo? = null
        fun get(ctx: Context): Repo = instance ?: synchronized(this) {
            instance ?: Repo(ctx.applicationContext).also { instance = it }
        }
    }

    private fun load(): AppState {
        val now = System.currentTimeMillis()
        val loaded = runCatching { if (file.exists()) Json.decode(file.readText()) else null }.getOrNull()
        val s = loaded ?: AppState()
        return Plan.prune(if (s.lastCheck == 0L) s.copy(lastCheck = now) else s, now)
    }

    /** Change state now; saving, alarms and widget refresh happen in the background. */
    fun edit(fn: (AppState) -> AppState) {
        _state.update(fn)
        scope.launch { commit() }
    }

    /** Change state and wait until everything is saved (used by widget, notifications, alarms). */
    suspend fun editNow(fn: (AppState) -> AppState) {
        _state.update(fn)
        commit()
    }

    suspend fun commit() {
        lock.withLock {
            val snap = _state.value
            withContext(Dispatchers.IO) {
                runCatching {
                    val text = Json.encode(snap)
                    val tmp = File(file.path + ".tmp")
                    tmp.writeText(text)
                    if (!tmp.renameTo(file)) {
                        file.writeText(text)
                        tmp.delete()
                    }
                }
            }
            runCatching { Alarms.reschedule(app, snap) }
            runCatching { PocketWidget().updateAll(app) }
        }
    }

    fun snapshot(): Snapshot = _state.value.let { Snapshot(it.reminders, it.history) }

    fun upsert(r: Reminder) = edit { s ->
        val now = System.currentTimeMillis()
        val exists = s.reminders.any { it.id == r.id }
        val stamped = r.copy(updatedAt = now, createdAt = if (r.createdAt == 0L) now else r.createdAt)
        s.copy(reminders = if (exists) s.reminders.map { if (it.id == r.id) stamped else it } else s.reminders + stamped)
    }

    fun delete(id: String): Snapshot {
        val before = snapshot()
        edit { s -> s.copy(reminders = s.reminders.filterNot { it.id == id }) }
        return before
    }

    fun restore(sn: Snapshot) = edit { it.copy(reminders = sn.reminders, history = sn.history) }

    fun tick(id: String, day: LocalDate): TickResult? {
        if (_state.value.reminders.none { it.id == id }) return null
        val before = snapshot()
        val now = System.currentTimeMillis()
        edit { Plan.applyTick(it, id, day, now) }
        val s1 = _state.value
        val today = LocalDate.now()
        val r1 = s1.reminders.find { it.id == id }
        val open = Plan.todayOpen(s1, today).size
        val done = Plan.doneOn(s1, today).size
        return TickResult(
            before = before,
            next = r1?.takeIf { it.repeat != Repeat.NONE }?.date,
            dayCleared = open == 0 && done > 0 && (before.reminders.isNotEmpty()),
            progress = Plan.todayProgress(s1, today),
        )
    }

    suspend fun tickNow(id: String, day: LocalDate) {
        val now = System.currentTimeMillis()
        editNow { Plan.applyTick(it, id, day, now) }
    }

    fun untick(completionId: String) {
        val now = System.currentTimeMillis()
        edit { Plan.applyUntick(it, completionId, now) }
    }

    suspend fun untickNow(completionId: String) {
        val now = System.currentTimeMillis()
        editNow { Plan.applyUntick(it, completionId, now) }
    }

    fun setMood(day: LocalDate, mood: Int?) = edit { s ->
        s.copy(moods = if (mood == null) s.moods - day else s.moods + (day to mood))
    }

    suspend fun setMoodNow(day: LocalDate, mood: Int?) = editNow { s ->
        s.copy(moods = if (mood == null) s.moods - day else s.moods + (day to mood))
    }

    fun setRemark(day: LocalDate, text: String) = edit { s ->
        s.copy(remarks = if (text.isBlank()) s.remarks - day else s.remarks + (day to text))
    }

    fun settings(fn: (Settings) -> Settings) = edit { it.copy(settings = fn(it.settings)) }
}
