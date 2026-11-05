package com.ashu.pocketplanner.notify

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ashu.pocketplanner.R
import com.ashu.pocketplanner.data.AppState
import com.ashu.pocketplanner.data.Reminder
import com.ashu.pocketplanner.data.Repeat
import com.ashu.pocketplanner.data.Repo
import com.ashu.pocketplanner.logic.Fmt
import com.ashu.pocketplanner.logic.Plan
import com.ashu.pocketplanner.ui.MainActivity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * One alarm at a time: always set for the soonest thing that needs attention
 * (a reminder's time, the morning summary, or midnight to refresh the widget).
 * When it fires, everything due is notified and the next alarm is set.
 */
object Alarms {

    const val CH_REMINDERS = "reminders"
    const val CH_DIGEST = "digest"
    private const val ACCENT = 0xFFA99BFF.toInt()

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CH_REMINDERS, "Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts at the time you set"
                enableVibration(true)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DIGEST, "Morning summary", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "What's on today, once a morning"
            }
        )
    }

    private fun ms(d: LocalDate, t: LocalTime): Long =
        d.atTime(t).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun alarmIntent(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx, 7,
        Intent(ctx, AlarmReceiver::class.java).setAction("com.ashu.pocketplanner.CHECK"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun canExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return false
        return am.canScheduleExactAlarms()
    }

    fun nextEventAt(s: AppState, now: Long): Long {
        val today = LocalDate.now()
        var best = ms(today.plusDays(1), LocalTime.MIDNIGHT) + 5_000
        if (s.settings.digest) {
            val t = LocalTime.of(s.settings.digestHour.coerceIn(0, 23), 0)
            val a = ms(today, t)
            val c = if (a > now) a else ms(today.plusDays(1), t)
            if (c < best) best = c
        }
        for (r in s.reminders) {
            val t = r.time ?: continue
            val d0 = r.date ?: continue
            if (r.done) continue
            if (r.repeat == Repeat.NONE) {
                val m = ms(d0, t)
                if (m in (now + 1)..<best) best = m
                continue
            }
            var d = if (d0.isAfter(today)) d0 else today
            var i = 0
            while (i < 400) {
                if (Plan.occursOn(r, d)) {
                    val m = ms(d, t)
                    if (m > now) {
                        if (m < best) best = m
                        break
                    }
                }
                d = d.plusDays(1)
                i++
            }
        }
        return best
    }

    fun reschedule(ctx: Context, s: AppState) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = alarmIntent(ctx)
        val at = nextEventAt(s, System.currentTimeMillis())
        am.cancel(pi)
        try {
            if (canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /** Called when the alarm fires: notify everything that came due since the last check. */
    suspend fun runDue(ctx: Context) {
        val repo = Repo.get(ctx)
        val s = repo.state.value
        val now = System.currentTimeMillis()
        val from = maxOf(s.lastCheck, now - 36L * 3600 * 1000)
        val zone = ZoneId.systemDefault()
        val startDay = Instant.ofEpochMilli(from).atZone(zone).toLocalDate()
        val today = LocalDate.now()
        for (r in s.reminders) {
            val t = r.time ?: continue
            if (r.done || r.date == null) continue
            var d = startDay
            while (!d.isAfter(today)) {
                if (Plan.occursOn(r, d)) {
                    val m = ms(d, t)
                    if (m > from && m <= now && m > r.updatedAt) notifyReminder(ctx, s, r, d)
                }
                d = d.plusDays(1)
            }
        }
        if (s.settings.digest) {
            val dt = ms(today, LocalTime.of(s.settings.digestHour.coerceIn(0, 23), 0))
            if (dt > from && dt <= now) notifyDigest(ctx, s, today)
        }
        repo.editNow { it.copy(lastCheck = now) }
    }

    private fun openApp(ctx: Context, code: Int): PendingIntent = PendingIntent.getActivity(
        ctx, code,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    @SuppressLint("MissingPermission")
    private fun notifyReminder(ctx: Context, s0: AppState, r: Reminder, d: LocalDate) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val nid = (r.id + d.toString()).hashCode()
        val done = PendingIntent.getBroadcast(
            ctx, nid,
            Intent(ctx, ActionReceiver::class.java)
                .setAction(ActionReceiver.DONE)
                .putExtra("id", r.id)
                .putExtra("date", d.toString())
                .putExtra("nid", nid),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val text = buildString {
            r.time?.let { append(Fmt.time(it)) }
            if (r.urgent) append(" · urgent")
            if (r.note.isNotBlank()) append(" · ").append(r.note.lineSequence().first())
        }
        val n = NotificationCompat.Builder(ctx, CH_REMINDERS)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(ACCENT)
            .setContentTitle((if (s0.settings.emojis) "${r.emoji} ${r.title}" else r.title).trim())
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx, nid))
            .addAction(0, "Done", done)
            .build()
        nm.notify(nid, n)
    }

    @SuppressLint("MissingPermission")
    private fun notifyDigest(ctx: Context, s: AppState, today: LocalDate) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val items = Plan.todayOpen(s, today)
        if (items.isEmpty()) return
        val style = NotificationCompat.InboxStyle()
        items.take(6).forEach { r ->
            style.addLine(listOfNotNull(r.emoji.takeIf { s.settings.emojis }, r.title, r.time?.let { Fmt.time(it) }).joinToString(" "))
        }
        if (items.size > 6) style.setSummaryText("+${items.size - 6} more")
        val title = if (items.size == 1) "1 thing today" else "${items.size} things today"
        val n = NotificationCompat.Builder(ctx, CH_DIGEST)
            .setSmallIcon(R.drawable.ic_stat)
            .setColor(ACCENT)
            .setContentTitle("☀️ $title")
            .setContentText(items.joinToString(", ") { it.title })
            .setStyle(style)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx, 99))
            .build()
        nm.notify(99, n)
    }
}
