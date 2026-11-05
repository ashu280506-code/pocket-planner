package com.ashu.pocketplanner.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** Plain JSON storage, no extra libraries. */
object Json {

    fun encode(s: AppState): String {
        val o = JSONObject()
        o.put("v", 1)
        o.put("reminders", JSONArray().apply { s.reminders.forEach { put(rem(it)) } })
        o.put("history", JSONArray().apply { s.history.forEach { put(comp(it)) } })
        o.put("moods", JSONObject().apply { s.moods.forEach { (d, m) -> put(d.toString(), m) } })
        o.put("remarks", JSONObject().apply { s.remarks.forEach { (d, t) -> put(d.toString(), t) } })
        o.put("settings", settings(s.settings))
        o.put("lastCheck", s.lastCheck)
        return o.toString()
    }

    fun decode(text: String): AppState {
        val o = JSONObject(text)
        val rems = o.optJSONArray("reminders")?.let { a -> (0 until a.length()).mapNotNull { runCatching { rem(a.getJSONObject(it)) }.getOrNull() } }.orEmpty()
        val hist = o.optJSONArray("history")?.let { a -> (0 until a.length()).mapNotNull { runCatching { comp(a.getJSONObject(it)) }.getOrNull() } }.orEmpty()
        val moods = mutableMapOf<LocalDate, Int>()
        o.optJSONObject("moods")?.let { m -> m.keys().forEach { k -> runCatching { moods[LocalDate.parse(k)] = m.getInt(k) } } }
        val remarks = mutableMapOf<LocalDate, String>()
        o.optJSONObject("remarks")?.let { m -> m.keys().forEach { k -> runCatching { remarks[LocalDate.parse(k)] = m.getString(k) } } }
        val st = o.optJSONObject("settings")?.let { settings(it) } ?: Settings()
        return AppState(rems, hist, moods, remarks, st, o.optLong("lastCheck", 0L))
    }

    private fun timeStr(t: LocalTime) = String.format(Locale.US, "%02d:%02d", t.hour, t.minute)

    private fun rem(r: Reminder) = JSONObject().apply {
        put("id", r.id); put("title", r.title); put("note", r.note); put("emoji", r.emoji)
        put("date", r.date?.toString() ?: ""); put("time", r.time?.let { timeStr(it) } ?: "")
        put("repeat", r.repeat.name); put("anchor", r.anchorDay)
        put("urgent", r.urgent); put("countdown", r.countdown)
        put("done", r.done); put("doneAt", r.doneAt); put("createdAt", r.createdAt); put("updatedAt", r.updatedAt)
    }

    private fun rem(o: JSONObject) = Reminder(
        id = o.getString("id"),
        title = o.optString("title"),
        note = o.optString("note"),
        emoji = o.optString("emoji"),
        date = o.optString("date").takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
        time = o.optString("time").takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) },
        repeat = enumOr(o.optString("repeat"), Repeat.NONE),
        anchorDay = o.optInt("anchor", 1),
        urgent = o.optBoolean("urgent"),
        countdown = o.optBoolean("countdown"),
        done = o.optBoolean("done"),
        doneAt = o.optLong("doneAt"),
        createdAt = o.optLong("createdAt"),
        updatedAt = o.optLong("updatedAt"),
    )

    private fun comp(c: Completion) = JSONObject().apply {
        put("id", c.id); put("rid", c.reminderId); put("title", c.title); put("emoji", c.emoji)
        put("date", c.date.toString()); put("at", c.at)
    }

    private fun comp(o: JSONObject) = Completion(
        id = o.getString("id"),
        reminderId = o.optString("rid"),
        title = o.optString("title"),
        emoji = o.optString("emoji"),
        date = LocalDate.parse(o.getString("date")),
        at = o.optLong("at"),
    )

    private fun settings(s: Settings) = JSONObject().apply {
        put("theme", s.theme.name); put("dark", s.dark); put("font", s.font.name)
        put("sound", s.sound); put("confetti", s.confetti); put("glyph", s.glyph)
        put("photoFog", s.photoFog.toDouble()); put("photoVersion", s.photoVersion)
        put("widgetSkin", s.widgetSkin.name); put("widgetFont", s.widgetFont.name)
        put("widgetFog", s.widgetFog.toDouble()); put("widgetPhotoVersion", s.widgetPhotoVersion)
        put("digest", s.digest); put("digestHour", s.digestHour); put("askedNotif", s.askedNotif)
        put("name", s.name); put("onboarded", s.onboarded); put("customColor", s.customColor)
        put("emojis", s.emojis); put("widgetMixFonts", s.widgetMixFonts)
    }

    private fun settings(o: JSONObject): Settings {
        val d = Settings()
        return Settings(
            theme = enumOr(o.optString("theme"), d.theme),
            dark = o.optBoolean("dark", d.dark),
            font = enumOr(o.optString("font"), d.font),
            sound = o.optBoolean("sound", d.sound),
            confetti = o.optBoolean("confetti", d.confetti),
            glyph = o.optBoolean("glyph", d.glyph),
            photoFog = o.optDouble("photoFog", d.photoFog.toDouble()).toFloat(),
            photoVersion = o.optLong("photoVersion", 0L),
            widgetSkin = enumOr(o.optString("widgetSkin"), d.widgetSkin),
            widgetFont = enumOr(o.optString("widgetFont"), d.widgetFont),
            widgetFog = o.optDouble("widgetFog", d.widgetFog.toDouble()).toFloat(),
            widgetPhotoVersion = o.optLong("widgetPhotoVersion", 0L),
            digest = o.optBoolean("digest", d.digest),
            digestHour = o.optInt("digestHour", d.digestHour),
            askedNotif = o.optBoolean("askedNotif", false),
            name = o.optString("name", ""),
            onboarded = o.optBoolean("onboarded", false),
            customColor = o.optLong("customColor", d.customColor),
            emojis = o.optBoolean("emojis", true),
            widgetMixFonts = o.optBoolean("widgetMixFonts", false),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        runCatching { enumValueOf<E>(name) }.getOrDefault(fallback)
}
