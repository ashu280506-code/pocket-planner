package com.ashu.pocketplanner.logic

import com.ashu.pocketplanner.data.Repeat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

/**
 * Turns "call mom tomorrow at 7pm" into a title, date, time and repeat.
 * Understands: today, tomorrow, tonight, day after tomorrow, monday / next fri, 12 nov, nov 12, 25/12,
 * in 3 days, at 5pm, 5:30pm, at 18:30, morning/evening, every day/week/month/year, every sunday,
 * and "urgent" or "!" for priority.
 */
object QuickParse {

    data class Result(
        val title: String,
        val date: LocalDate,
        val time: LocalTime?,
        val repeat: Repeat,
        val urgent: Boolean,
        val dated: Boolean,
    )

    private const val WD = "(sun|mon|tue|tues|wed|wednes|thu|thur|thurs|fri|sat|satur)(?:day)?"
    private const val MON = "(jan|feb|mar|apr|may|jun|jul|aug|sep|sept|oct|nov|dec)[a-z]*"
    private val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

    private fun dow(w: String): DayOfWeek = when (w.lowercase().take(3)) {
        "sun" -> DayOfWeek.SUNDAY
        "mon" -> DayOfWeek.MONDAY
        "tue" -> DayOfWeek.TUESDAY
        "wed" -> DayOfWeek.WEDNESDAY
        "thu" -> DayOfWeek.THURSDAY
        "fri" -> DayOfWeek.FRIDAY
        else -> DayOfWeek.SATURDAY
    }

    private fun mon(m: String): Int = months.indexOf(m.lowercase().take(3)) + 1

    private fun nextDow(today: LocalDate, d: DayOfWeek, forceNext: Boolean): LocalDate {
        var n = (d.value - today.dayOfWeek.value + 7) % 7
        if (forceNext && n == 0) n = 7
        return today.plusDays(n.toLong())
    }

    private fun mk(today: LocalDate, day: Int, month: Int, year: String?): LocalDate? {
        if (month !in 1..12 || day !in 1..31) return null
        var y = year?.toIntOrNull()?.let { if (it < 100) 2000 + it else it } ?: today.year
        var d = LocalDate.of(y, month, day.coerceAtMost(YearMonth.of(y, month).lengthOfMonth()))
        if (year == null && d.isBefore(today)) {
            y += 1
            d = LocalDate.of(y, month, day.coerceAtMost(YearMonth.of(y, month).lengthOfMonth()))
        }
        return d
    }

    private fun safeTime(h: Int, m: Int): LocalTime? = if (h in 0..23 && m in 0..59) LocalTime.of(h, m) else null

    fun parse(raw: String, base: LocalDate, today: LocalDate = LocalDate.now()): Result {
        var s = " $raw "
        var date = base
        var time: LocalTime? = null
        var repeat = Repeat.NONE
        var urgent = false
        var dated = false

        fun take(pattern: String, fn: (MatchResult) -> Unit): Boolean {
            val m = Regex(pattern, RegexOption.IGNORE_CASE).find(s) ?: return false
            fn(m)
            s = s.replaceRange(m.range, " ")
            return true
        }

        take("\\b(urgent|asap|important)\\b") { urgent = true }
        take("!+") { urgent = true }

        take("\\b(every ?day|daily)\\b") { repeat = Repeat.DAILY }
        take("\\bevery $WD\\b") {
            repeat = Repeat.WEEKLY
            date = nextDow(today, dow(it.groupValues[1]), false)
            dated = true
        }
        take("\\b(every week|weekly)\\b") { repeat = Repeat.WEEKLY }
        take("\\b(every month|monthly)\\b") { repeat = Repeat.MONTHLY }
        take("\\b(every year|yearly|annually)\\b") { repeat = Repeat.YEARLY }

        if (!dated) dated = take("\\bday after tomorrow\\b") { date = today.plusDays(2) }
        if (!dated) dated = take("\\b(tomorrow|tmrw|tmr|tomorow)\\b") { date = today.plusDays(1) }
        if (!dated) dated = take("\\btonight\\b") { date = today; time = LocalTime.of(21, 0) }
        if (!dated) dated = take("\\btoday\\b") { date = today }
        if (!dated) dated = take("\\bin (\\d{1,3}) (days?|weeks?|months?)\\b") {
            val n = it.groupValues[1].toLong()
            date = when (it.groupValues[2].lowercase()[0]) {
                'w' -> today.plusWeeks(n)
                'm' -> today.plusMonths(n)
                else -> today.plusDays(n)
            }
        }
        if (!dated) dated = take("\\b(?:on )?(\\d{1,2})(?:st|nd|rd|th)? $MON(?:,? (\\d{4}))?\\b") {
            mk(today, it.groupValues[1].toInt(), mon(it.groupValues[2]), it.groupValues[3].ifEmpty { null })?.let { d -> date = d }
        }
        if (!dated) dated = take("\\b(?:on )?$MON (\\d{1,2})(?:st|nd|rd|th)?(?:,? (\\d{4}))?\\b") {
            mk(today, it.groupValues[2].toInt(), mon(it.groupValues[1]), it.groupValues[3].ifEmpty { null })?.let { d -> date = d }
        }
        if (!dated) dated = take("\\b(?:on )?(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?\\b") {
            mk(today, it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3].ifEmpty { null })?.let { d -> date = d }
        }
        if (!dated) dated = take("\\b(?:on |this )?(next )?$WD\\b") {
            date = nextDow(today, dow(it.groupValues[2]), it.groupValues[1].isNotEmpty())
        }

        val gotTime = take("\\b(?:at |@ ?)?(\\d{1,2})(?:[:.]?(\\d{2}))? ?(am|pm)\\b") {
            var h = it.groupValues[1].toInt() % 12
            if (it.groupValues[3].lowercase() == "pm") h += 12
            safeTime(h, it.groupValues[2].toIntOrNull() ?: 0)?.let { t -> time = t }
        } || take("\\b(?:at |@ ?)(\\d{1,2})[:.](\\d{2})\\b") {
            safeTime(it.groupValues[1].toInt(), it.groupValues[2].toInt())?.let { t -> time = t }
        } || take("\\b(?:at |@ ?)(\\d{1,2})\\b") {
            var h = it.groupValues[1].toInt()
            if (h in 1..7) h += 12
            safeTime(h, 0)?.let { t -> time = t }
        } || take("\\b(\\d{1,2}):(\\d{2})\\b") {
            safeTime(it.groupValues[1].toInt(), it.groupValues[2].toInt())?.let { t -> time = t }
        }
        if (!gotTime && time == null) {
            take("\\b(?:in the )?morning\\b") { time = LocalTime.of(9, 0) } ||
                take("\\b(?:in the )?afternoon\\b") { time = LocalTime.of(14, 0) } ||
                take("\\b(?:in the )?evening\\b") { time = LocalTime.of(18, 0) } ||
                take("\\bat night\\b") { time = LocalTime.of(21, 0) }
        }

        var title = s.replace(Regex("\\s+"), " ").trim()
        title = title.replace(Regex("^(remind me to|remind me|to)\\s+", RegexOption.IGNORE_CASE), "")
        title = title.replace(Regex("\\s+(on|at|by|this|in|for)$", RegexOption.IGNORE_CASE), "")
        title = title.trim(' ', ',', '-', '.')
        if (title.isNotEmpty()) title = title.replaceFirstChar { it.uppercase() }

        return Result(title, date, time, repeat, urgent, dated)
    }
}
