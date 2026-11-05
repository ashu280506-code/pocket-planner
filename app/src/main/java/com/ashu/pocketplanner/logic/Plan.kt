package com.ashu.pocketplanner.logic

import com.ashu.pocketplanner.data.AppState
import com.ashu.pocketplanner.data.Completion
import com.ashu.pocketplanner.data.Reminder
import com.ashu.pocketplanner.data.Repeat
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

/** All the date rules: what is due when, repeats, ticking, history. */
object Plan {

    private const val SIX_MONTHS_MS = 183L * 24 * 3600 * 1000

    fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(16)

    private fun clampDay(d: LocalDate, anchor: Int): LocalDate =
        d.withDayOfMonth(minOf(anchor.coerceAtLeast(1), d.lengthOfMonth()))

    fun next(r: Reminder, from: LocalDate): LocalDate = when (r.repeat) {
        Repeat.DAILY -> from.plusDays(1)
        Repeat.WEEKLY -> from.plusWeeks(1)
        Repeat.MONTHLY -> clampDay(from.plusMonths(1), r.anchorDay)
        Repeat.YEARLY -> clampDay(from.plusYears(1), r.anchorDay)
        Repeat.NONE -> from
    }

    fun occursOn(r: Reminder, day: LocalDate): Boolean {
        val d0 = r.date ?: return false
        if (r.done) return false
        if (r.repeat == Repeat.NONE) return d0 == day
        if (day.isBefore(d0)) return false
        return when (r.repeat) {
            Repeat.DAILY -> true
            Repeat.WEEKLY -> ChronoUnit.DAYS.between(d0, day) % 7 == 0L
            Repeat.MONTHLY -> day.dayOfMonth == minOf(r.anchorDay, day.lengthOfMonth())
            Repeat.YEARLY -> day.monthValue == d0.monthValue && day.dayOfMonth == minOf(r.anchorDay, day.lengthOfMonth())
            Repeat.NONE -> false
        }
    }

    fun isOverdue(r: Reminder, today: LocalDate): Boolean {
        val d = r.date ?: return false
        return !r.done && d.isBefore(today)
    }

    val order: Comparator<Reminder> = compareByDescending<Reminder> { it.urgent }
        .thenBy { it.time ?: LocalTime.MAX }
        .thenBy { it.createdAt }

    fun overdue(s: AppState, today: LocalDate): List<Reminder> =
        s.reminders.filter { isOverdue(it, today) }
            .sortedWith(compareBy<Reminder> { it.date }.then(order))

    /** Open items on [day]. On today, overdue items are listed separately, so they are left out here. */
    fun dayOpen(s: AppState, day: LocalDate, today: LocalDate): List<Reminder> =
        s.reminders.filter { occursOn(it, day) && !(day == today && isOverdue(it, today)) }.sortedWith(order)

    fun todayOpen(s: AppState, today: LocalDate): List<Reminder> = overdue(s, today) + dayOpen(s, today, today)

    fun doneOn(s: AppState, day: LocalDate): List<Completion> = s.history.filter { it.date == day }.sortedBy { it.at }

    fun countOn(s: AppState, day: LocalDate): Int = s.reminders.count { occursOn(it, day) }

    fun notes(s: AppState): List<Reminder> = s.reminders.filter { it.date == null }.sortedByDescending { it.updatedAt }

    fun nextOnOrAfter(r: Reminder, day: LocalDate): LocalDate? {
        val d = r.date ?: return null
        if (!d.isBefore(day) || r.repeat == Repeat.NONE) return d
        var n = d
        var i = 0
        while (n.isBefore(day) && i < 1000) { n = next(r, n); i++ }
        return n
    }

    /** Countdown items with the number of days left, nearest first. */
    fun countdowns(s: AppState, today: LocalDate): List<Pair<Reminder, Long>> =
        s.reminders.filter { it.countdown && !it.done && it.date != null }
            .mapNotNull { r ->
                val d = nextOnOrAfter(r, today) ?: return@mapNotNull null
                if (d.isBefore(today)) null else r to ChronoUnit.DAYS.between(today, d)
            }
            .sortedBy { it.second }

    fun todayProgress(s: AppState, today: LocalDate): Int {
        val open = todayOpen(s, today).size
        val done = doneOn(s, today).size
        val total = open + done
        return if (total == 0) 0 else done * 100 / total
    }

    fun applyTick(s: AppState, id: String, day: LocalDate, now: Long): AppState {
        val r = s.reminders.find { it.id == id } ?: return s
        val d0 = r.date ?: return s
        val comp = Completion(newId(), r.id, r.title, r.emoji, day, now)
        val updated = if (r.repeat == Repeat.NONE) {
            r.copy(done = true, doneAt = now, updatedAt = now)
        } else {
            val today = LocalDate.now()
            var n = next(r, if (day.isAfter(d0)) day else d0)
            var i = 0
            while (n.isBefore(today) && i < 1000) { n = next(r, n); i++ }
            r.copy(date = n, updatedAt = now)
        }
        return s.copy(
            reminders = s.reminders.map { if (it.id == id) updated else it },
            history = s.history + comp,
        )
    }

    fun applyUntick(s: AppState, completionId: String, now: Long): AppState {
        val c = s.history.find { it.id == completionId } ?: return s
        val reminders = s.reminders.map { r ->
            if (r.id != c.reminderId) r
            else if (r.repeat == Repeat.NONE) r.copy(done = false, doneAt = 0L, updatedAt = now)
            else {
                val d = r.date
                if (d != null && c.date.isBefore(d)) r.copy(date = c.date, updatedAt = now) else r
            }
        }
        return s.copy(reminders = reminders, history = s.history.filterNot { it.id == completionId })
    }

    /** Clears history older than 6 months and finished one-off items older than that. */
    fun prune(s: AppState, now: Long): AppState {
        val cutoff = now - SIX_MONTHS_MS
        val moodCutoff = LocalDate.now().minusDays(400)
        return s.copy(
            history = s.history.filter { it.at >= cutoff },
            reminders = s.reminders.filterNot { it.done && it.repeat == Repeat.NONE && it.doneAt in 1 until cutoff },
            moods = s.moods.filterKeys { !it.isBefore(moodCutoff) },
        )
    }
}
