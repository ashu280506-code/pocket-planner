package com.ashu.pocketplanner.logic

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object Fmt {
    private val short = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH)
    private val shortYear = DateTimeFormatter.ofPattern("EEE d MMM yyyy", Locale.ENGLISH)
    private val long = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)
    private val month = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
    private val weekday = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)

    fun day(d: LocalDate): String = d.format(if (d.year == LocalDate.now().year) short else shortYear)
    fun longDay(d: LocalDate): String = d.format(long)
    fun month(m: YearMonth): String = m.format(month)
    fun weekday(d: LocalDate): String = d.format(weekday)
    fun dayMonth(d: LocalDate): String = d.format(dayMonth)

    fun time(t: LocalTime): String {
        val h = t.hour % 12
        return "${if (h == 0) 12 else h}:${String.format(Locale.US, "%02d", t.minute)} ${if (t.hour < 12) "am" else "pm"}"
    }

    fun hour(h: Int): String {
        val x = h % 12
        return "${if (x == 0) 12 else x} ${if (h < 12) "am" else "pm"}"
    }

    fun rel(d: LocalDate, today: LocalDate): String {
        val n = ChronoUnit.DAYS.between(today, d)
        return when {
            n == 0L -> "Today"
            n == 1L -> "Tomorrow"
            n == -1L -> "Yesterday"
            n in 2..6 -> weekday(d)
            n > 0 -> "in $n days"
            else -> "${-n} days ago"
        }
    }

    fun late(d: LocalDate, today: LocalDate): String {
        val n = ChronoUnit.DAYS.between(d, today)
        return if (n == 1L) "from yesterday" else "$n days late"
    }

    fun countdown(title: String, days: Long): String = when (days) {
        0L -> "$title is today!"
        1L -> "$title tomorrow"
        else -> "$title in $days days"
    }
}
