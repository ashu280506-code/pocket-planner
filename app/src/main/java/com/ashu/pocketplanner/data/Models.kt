package com.ashu.pocketplanner.data

import java.time.LocalDate
import java.time.LocalTime

enum class Repeat(val label: String) {
    NONE("Once"), DAILY("Every day"), WEEKLY("Every week"), MONTHLY("Every month"), YEARLY("Every year")
}

enum class ThemePreset(val label: String) {
    MIDNIGHT("Midnight Fog"), SUNSET("Sunset Glass"), AURORA("Aurora"), DOT("Dot Matrix"), PHOTO("Your Photo")
}

enum class FontChoice(val label: String) {
    CLEAN("Clean"), ROUNDED("Rounded"), HAND("Handwritten"), DOT("Dot matrix"), RETRO("Typewriter")
}

enum class WidgetSkin(val label: String) {
    DARK("Dark"), GLASS("Foggy glass"), PHOTO("Your photo")
}

data class Reminder(
    val id: String,
    val title: String,
    val note: String = "",
    val emoji: String = "",
    /** null = a note with no date. For repeating items this is the next pending occurrence. */
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val repeat: Repeat = Repeat.NONE,
    val anchorDay: Int = 1,
    val urgent: Boolean = false,
    val countdown: Boolean = false,
    val done: Boolean = false,
    val doneAt: Long = 0L,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

/** One finished task, kept for the History and weekly report (6 months). */
data class Completion(
    val id: String,
    val reminderId: String,
    val title: String,
    val emoji: String,
    val date: LocalDate,
    val at: Long,
)

data class Settings(
    val theme: ThemePreset = ThemePreset.MIDNIGHT,
    val dark: Boolean = true,
    val font: FontChoice = FontChoice.CLEAN,
    val sound: Boolean = true,
    val confetti: Boolean = true,
    val glyph: Boolean = true,
    val photoFog: Float = 0.5f,
    /** 0 = no photo chosen; otherwise changes whenever the photo is re-rendered. */
    val photoVersion: Long = 0L,
    val widgetSkin: WidgetSkin = WidgetSkin.GLASS,
    val widgetFont: FontChoice = FontChoice.CLEAN,
    val widgetFog: Float = 0.6f,
    val widgetPhotoVersion: Long = 0L,
    val digest: Boolean = true,
    val digestHour: Int = 8,
    val askedNotif: Boolean = false,
)

data class AppState(
    val reminders: List<Reminder> = emptyList(),
    val history: List<Completion> = emptyList(),
    val moods: Map<LocalDate, Int> = emptyMap(),
    val settings: Settings = Settings(),
    val lastCheck: Long = 0L,
)

object Moods {
    val emojis = listOf("😄", "🙂", "😐", "😕", "😢")
    val labels = listOf("Great", "Good", "Okay", "Low", "Rough")
    val colors = listOf(0xFF4ADE80, 0xFF2DD4BF, 0xFF94A3B8, 0xFFFBBF24, 0xFF818CF8)
}
