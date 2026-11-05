package com.ashu.pocketplanner.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.ashu.pocketplanner.R
import com.ashu.pocketplanner.data.FontChoice
import com.ashu.pocketplanner.data.Settings
import com.ashu.pocketplanner.data.ThemePreset

data class Palette(
    val dark: Boolean,
    val base: Color,
    val blobs: List<Color>,
    val blobAlpha: Float,
    val accent: Color,
    val onAccent: Color,
    val warm: Color,
    val text: Color,
    val sub: Color,
    val glass: Color,
    val glassHi: Color,
    val border: Color,
    val sheet: Color,
    val danger: Color,
    val dots: Boolean,
)

private fun c(v: Long) = Color(v)

private fun hsv(h: Float, sat: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(((h % 360f) + 360f) % 360f, sat.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))

/** Your own background colour: the colour itself is the background, everything else is matched to it. */
private fun customPalette(argb: Long): Palette {
    val base = Color(argb)
    val hsvArr = FloatArray(3)
    android.graphics.Color.colorToHSV(argb.toInt(), hsvArr)
    val (h, sat, v) = Triple(hsvArr[0], hsvArr[1], hsvArr[2])
    val dark = base.luminance() < 0.42f
    val blobs = listOf(
        hsv(h - 35f, sat * 0.9f + 0.1f, if (dark) v + 0.35f else v - 0.2f),
        hsv(h + 40f, sat * 0.9f + 0.1f, if (dark) v + 0.3f else v - 0.15f),
        hsv(h + 180f, 0.45f, if (dark) 0.85f else 0.9f),
    )
    return if (dark) Palette(
        dark = true, base = base, blobs = blobs, blobAlpha = 0.45f,
        accent = hsv(h + 160f, 0.45f, 1f), onAccent = Color(0xFF15112E), warm = Color(0xFFFFB547),
        text = Color(0xFFF7F6FF), sub = Color(0xC2F7F6FF),
        glass = Color.White.copy(alpha = 0.08f), glassHi = Color.White.copy(alpha = 0.14f),
        border = Color.White.copy(alpha = 0.18f), sheet = hsv(h, sat, (v * 0.55f).coerceAtLeast(0.08f)),
        danger = Color(0xFFFF7A8A), dots = false,
    ) else Palette(
        dark = false, base = base, blobs = blobs, blobAlpha = 0.35f,
        accent = hsv(h + 160f, 0.8f, 0.5f), onAccent = Color.White, warm = Color(0xFFC46A00),
        text = Color(0xFF16142A), sub = Color(0xFF16142A).copy(alpha = 0.68f),
        glass = Color.White.copy(alpha = 0.42f), glassHi = Color.White.copy(alpha = 0.62f),
        border = Color.White.copy(alpha = 0.8f), sheet = hsv(h, sat * 0.25f, 0.98f),
        danger = Color(0xFFC62839), dots = false,
    )
}

fun paletteFor(s: Settings): Palette {
    if (s.theme == ThemePreset.CUSTOM) return customPalette(s.customColor)
    val dark = s.dark
    val blobs = when (s.theme) {
        ThemePreset.SUNSET -> listOf(c(0xFFFF6B3D), c(0xFFFF2E7E), c(0xFFFFB23E), c(0xFF8A3CFF))
        ThemePreset.AURORA -> listOf(c(0xFF1DE9B6), c(0xFF7C4DFF), c(0xFF00B0FF), c(0xFF64FFDA))
        ThemePreset.DOT -> listOf(c(0xFF3A3A3A), c(0xFF262626))
        else -> listOf(c(0xFF6B4CFF), c(0xFF2E6BFF), c(0xFFFF4FA3), c(0xFFFF9A3C))
    }
    val accentDark = when (s.theme) {
        ThemePreset.SUNSET -> c(0xFFFFA07A)
        ThemePreset.AURORA -> c(0xFF5EF2C9)
        ThemePreset.DOT -> c(0xFFFF3B30)
        else -> c(0xFFB3A6FF)
    }
    val accentLight = when (s.theme) {
        ThemePreset.SUNSET -> c(0xFFD9481F)
        ThemePreset.AURORA -> c(0xFF0B8F72)
        ThemePreset.DOT -> c(0xFFD7261C)
        else -> c(0xFF5A45DB)
    }
    return if (dark) {
        Palette(
            dark = true,
            base = when (s.theme) {
                ThemePreset.SUNSET -> c(0xFF1A0B12)
                ThemePreset.AURORA -> c(0xFF04121A)
                ThemePreset.DOT -> c(0xFF000000)
                else -> c(0xFF0B0A1A)
            },
            blobs = blobs,
            blobAlpha = if (s.theme == ThemePreset.DOT) 0.6f else 0.55f,
            accent = accentDark,
            onAccent = if (s.theme == ThemePreset.DOT) Color.White else c(0xFF15112E),
            warm = c(0xFFFFB547),
            text = c(0xFFF4F2FF),
            sub = c(0xB8F4F2FF),
            glass = Color.White.copy(alpha = 0.07f),
            glassHi = Color.White.copy(alpha = 0.13f),
            border = Color.White.copy(alpha = 0.16f),
            sheet = when (s.theme) {
                ThemePreset.SUNSET -> c(0xFF22121A)
                ThemePreset.AURORA -> c(0xFF0A1C24)
                ThemePreset.DOT -> c(0xFF101010)
                else -> c(0xFF17142B)
            },
            danger = c(0xFFFF7A8A),
            dots = s.theme == ThemePreset.DOT,
        )
    } else {
        Palette(
            dark = false,
            base = if (s.theme == ThemePreset.DOT) c(0xFFF3F3F3) else c(0xFFEEF0F8),
            blobs = if (s.theme == ThemePreset.DOT) listOf(c(0xFFDDDDDD), c(0xFFCFCFCF)) else blobs,
            blobAlpha = 0.32f,
            accent = accentLight,
            onAccent = Color.White,
            warm = c(0xFFE08A00),
            text = c(0xFF17142B),
            sub = c(0xFF5D5A78),
            glass = Color.White.copy(alpha = 0.55f),
            glassHi = Color.White.copy(alpha = 0.75f),
            border = if (s.theme == ThemePreset.DOT) Color.Black.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.9f),
            sheet = c(0xFFF7F7FC),
            danger = c(0xFFD32F45),
            dots = s.theme == ThemePreset.DOT,
        )
    }
}

data class Fonts(val display: FontFamily, val body: FontFamily, val scale: Float)

private fun variable(id: Int) = FontFamily(
    Font(id, FontWeight.Normal),
    Font(id, FontWeight.Medium),
    Font(id, FontWeight.SemiBold),
    Font(id, FontWeight.Bold),
)

fun fontsFor(f: FontChoice): Fonts = when (f) {
    FontChoice.CLEAN -> Fonts(variable(R.font.bricolage), variable(R.font.outfit), 1f)
    FontChoice.ROUNDED -> Fonts(variable(R.font.nunito), variable(R.font.nunito), 1f)
    FontChoice.HAND -> Fonts(variable(R.font.caveat), variable(R.font.caveat), 1.22f)
    FontChoice.DOT -> Fonts(variable(R.font.doto), variable(R.font.outfit), 1f)
    FontChoice.RETRO -> FontFamily(Font(R.font.special_elite)).let { Fonts(it, it, 0.96f) }
}

private fun typographyFor(f: Fonts): Typography {
    val d = Typography()
    fun TextStyle.disp() = copy(fontFamily = f.display, fontSize = fontSize * f.scale)
    fun TextStyle.body() = copy(fontFamily = f.body, fontSize = fontSize * f.scale)
    return Typography(
        displayLarge = d.displayLarge.disp(),
        displayMedium = d.displayMedium.disp(),
        displaySmall = d.displaySmall.disp(),
        headlineLarge = d.headlineLarge.disp(),
        headlineMedium = d.headlineMedium.disp(),
        headlineSmall = d.headlineSmall.disp(),
        titleLarge = d.titleLarge.disp(),
        titleMedium = d.titleMedium.body(),
        titleSmall = d.titleSmall.body(),
        bodyLarge = d.bodyLarge.body(),
        bodyMedium = d.bodyMedium.body(),
        bodySmall = d.bodySmall.body(),
        labelLarge = d.labelLarge.body(),
        labelMedium = d.labelMedium.body(),
        labelSmall = d.labelSmall.body(),
    )
}

val LocalPalette = staticCompositionLocalOf { paletteFor(Settings()) }
val LocalFonts = staticCompositionLocalOf { Fonts(FontFamily.Default, FontFamily.Default, 1f) }

@Composable
fun PlannerTheme(settings: Settings, content: @Composable () -> Unit) {
    val p = remember(settings.theme, settings.dark, settings.customColor) { paletteFor(settings) }
    val f = remember(settings.font) { fontsFor(settings.font) }
    val typography = remember(f) { typographyFor(f) }
    val scheme = if (p.dark) {
        darkColorScheme(
            primary = p.accent, onPrimary = p.onAccent, secondary = p.warm,
            background = p.base, onBackground = p.text,
            surface = p.sheet, onSurface = p.text, surfaceVariant = p.glassHi, onSurfaceVariant = p.sub,
            surfaceContainer = p.sheet, surfaceContainerHigh = p.sheet, surfaceContainerHighest = p.sheet,
            surfaceContainerLow = p.sheet, outline = p.border, error = p.danger,
        )
    } else {
        lightColorScheme(
            primary = p.accent, onPrimary = p.onAccent, secondary = p.warm,
            background = p.base, onBackground = p.text,
            surface = p.sheet, onSurface = p.text, surfaceVariant = p.glassHi, onSurfaceVariant = p.sub,
            surfaceContainer = p.sheet, surfaceContainerHigh = p.sheet, surfaceContainerHighest = p.sheet,
            surfaceContainerLow = p.sheet, outline = p.border, error = p.danger,
        )
    }
    CompositionLocalProvider(LocalPalette provides p, LocalFonts provides f) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
