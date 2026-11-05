package com.ashu.pocketplanner.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as SysSettings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.ashu.pocketplanner.data.AppState
import com.ashu.pocketplanner.data.FontChoice
import com.ashu.pocketplanner.data.Repo
import com.ashu.pocketplanner.data.ThemePreset
import com.ashu.pocketplanner.data.WidgetSkin
import com.ashu.pocketplanner.fx.GlyphFx
import com.ashu.pocketplanner.fx.Photos
import com.ashu.pocketplanner.fx.Sound
import com.ashu.pocketplanner.logic.Fmt
import com.ashu.pocketplanner.notify.Alarms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(s: AppState, repo: Repo, photo: ImageBitmap?, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val p = LocalPalette.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st = s.settings
    var appFog by remember { mutableFloatStateOf(st.photoFog) }
    var widgetFog by remember { mutableFloatStateOf(st.widgetFog) }
    var busy by remember { mutableStateOf(false) }
    val imageOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    val pickApp = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) { Photos.importApp(ctx, uri, appFog) }
            busy = false
            if (ok) repo.settings { it.copy(photoVersion = System.currentTimeMillis(), photoFog = appFog, theme = ThemePreset.PHOTO) }
        }
    }
    val pickWidget = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val ok = withContext(Dispatchers.IO) { Photos.importWidget(ctx, uri, widgetFog) }
            busy = false
            if (ok) repo.settings { it.copy(widgetPhotoVersion = System.currentTimeMillis(), widgetFog = widgetFog, widgetSkin = WidgetSkin.PHOTO) }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AppBackground(st, photo)
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = p.text)
                    }
                    Text("Settings", style = MaterialTheme.typography.headlineMedium, color = p.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = p.accent, strokeWidth = 2.dp)
                }
            }

            item {
                Card("Look") {
                    ToggleRow("Dark mode", "On by default. Turn off for the light Paper look.", st.dark) { v -> repo.settings { it.copy(dark = v) } }
                    Label("Theme")
                    ChipRowPadded {
                        ThemePreset.entries.forEach { t ->
                            ThemeSwatch(t, st.theme == t, photo) {
                                if (t == ThemePreset.PHOTO && st.photoVersion == 0L) pickApp.launch(imageOnly)
                                else repo.settings { it.copy(theme = t) }
                            }
                        }
                    }
                    Label("Font")
                    ChipRowPadded {
                        FontChoice.entries.forEach { f -> FontSample(f, st.font == f) { repo.settings { it.copy(font = f) } } }
                    }
                    Label("Background photo")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { pickApp.launch(imageOnly) },
                            colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
                        ) { Text(if (st.photoVersion == 0L) "Choose photo" else "Change photo") }
                        if (st.photoVersion != 0L) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                Photos.removeApp(ctx)
                                repo.settings { it.copy(photoVersion = 0L, theme = if (it.theme == ThemePreset.PHOTO) ThemePreset.MIDNIGHT else it.theme) }
                            }) { Text("Remove", color = p.danger) }
                        }
                    }
                    if (st.photoVersion != 0L) {
                        FogSlider("Fog", appFog, { appFog = it }) {
                            scope.launch {
                                busy = true
                                withContext(Dispatchers.IO) { Photos.renderApp(ctx, appFog) }
                                busy = false
                                repo.settings { it.copy(photoFog = appFog, photoVersion = System.currentTimeMillis()) }
                            }
                        }
                    }
                }
            }

            item {
                Card("Fun") {
                    ToggleRow("Pop sound", "Plays when you tick something off", st.sound) { v ->
                        repo.settings { it.copy(sound = v) }
                        if (v) Sound.pop()
                    }
                    ToggleRow("Confetti", "A little burst for every finished task", st.confetti) { v -> repo.settings { it.copy(confetti = v) } }
                    if (GlyphFx.supported()) {
                        ToggleRow("Glyph lights", "The back lights up as you finish tasks (while the app is open)", st.glyph) { v ->
                            repo.settings { it.copy(glyph = v) }
                            if (v) GlyphFx.start(ctx) else GlyphFx.stop()
                        }
                    }
                }
            }

            item {
                val notifOn = NotificationManagerCompat.from(ctx).areNotificationsEnabled()
                val exactOn = Alarms.canExact(ctx)
                Card("Reminders") {
                    if (!notifOn) {
                        Warn("Notifications are off, so reminders can't alert you.", "Turn on") {
                            ctx.startActivity(
                                Intent(SysSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(SysSettings.EXTRA_APP_PACKAGE, ctx.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                    if (!exactOn && Build.VERSION.SDK_INT >= 31) {
                        Warn("Exact alarms are off, so alerts may arrive a few minutes late.", "Allow") {
                            ctx.startActivity(
                                Intent(SysSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                    ToggleRow("Morning summary", "One note each morning with what's due today", st.digest) { v -> repo.settings { it.copy(digest = v) } }
                    if (st.digest) {
                        ChipRowPadded {
                            (6..10).forEach { h -> GlassChip(Fmt.hour(h), st.digestHour == h) { repo.settings { it.copy(digestHour = h) } } }
                        }
                    }
                }
            }

            item {
                Card("Widget") {
                    Text(
                        "Long-press your home screen, tap Widgets, find Pocket Planner and drag it out. Resize it to 2×2, 4×1, 4×2 or 4×4.",
                        style = MaterialTheme.typography.bodySmall, color = p.sub,
                    )
                    Label("Skin")
                    ChipRowPadded {
                        WidgetSkin.entries.forEach { k ->
                            GlassChip(k.label, st.widgetSkin == k) {
                                if (k == WidgetSkin.PHOTO && st.widgetPhotoVersion == 0L) pickWidget.launch(imageOnly)
                                else repo.settings { it.copy(widgetSkin = k) }
                            }
                        }
                    }
                    Label("Widget font")
                    ChipRowPadded {
                        FontChoice.entries.forEach { f -> GlassChip(widgetFontName(f), st.widgetFont == f) { repo.settings { it.copy(widgetFont = f) } } }
                    }
                    Label("Widget photo")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { pickWidget.launch(imageOnly) },
                            colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
                        ) { Text(if (st.widgetPhotoVersion == 0L) "Choose photo" else "Change photo") }
                        if (st.widgetPhotoVersion != 0L) {
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = {
                                Photos.removeWidget(ctx)
                                repo.settings { it.copy(widgetPhotoVersion = 0L, widgetSkin = if (it.widgetSkin == WidgetSkin.PHOTO) WidgetSkin.GLASS else it.widgetSkin) }
                            }) { Text("Remove", color = p.danger) }
                        }
                    }
                    if (st.widgetPhotoVersion != 0L) {
                        FogSlider("Widget fog", widgetFog, { widgetFog = it }) {
                            scope.launch {
                                busy = true
                                withContext(Dispatchers.IO) { Photos.renderWidget(ctx, widgetFog) }
                                busy = false
                                repo.settings { it.copy(widgetFog = widgetFog, widgetPhotoVersion = System.currentTimeMillis()) }
                            }
                        }
                    }
                    Text(
                        "Widgets can only use your phone's built-in font styles, so these are the closest matches to the app's fonts.",
                        style = MaterialTheme.typography.bodySmall, color = p.sub,
                    )
                }
            }

            if (GlyphFx.supported()) {
                item {
                    Card("Glyph alerts") {
                        Text(
                            "To make the back light up when a reminder goes off, open your phone's Settings, then Glyph Interface, then Notifications, and pick a light pattern for Pocket Planner.",
                            style = MaterialTheme.typography.bodyMedium, color = p.text,
                        )
                    }
                }
            }

            item {
                val version = runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: ""
                Text(
                    "Pocket Planner $version · Your data stays on this phone.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.sub,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}

private fun widgetFontName(f: FontChoice) = when (f) {
    FontChoice.CLEAN -> "Clean"
    FontChoice.ROUNDED -> "Rounded"
    FontChoice.HAND -> "Handwritten"
    FontChoice.DOT -> "Mono"
    FontChoice.RETRO -> "Serif"
}

@Composable
private fun Card(title: String, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    Glass(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = p.text, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun ChipRowPadded(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) = ChipRow(content)

@Composable
private fun Warn(text: String, action: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.danger.copy(alpha = 0.14f)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = p.text, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) { Text(action, color = p.danger, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun FogSlider(label: String, value: Float, onChange: (Float) -> Unit, onDone: () -> Unit) {
    val p = LocalPalette.current
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = p.text, modifier = Modifier.weight(1f))
            Text(if (value < 0.2f) "Clear" else if (value < 0.6f) "Misty" else "Foggy", style = MaterialTheme.typography.bodySmall, color = p.sub)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            onValueChangeFinished = onDone,
            colors = SliderDefaults.colors(thumbColor = p.accent, activeTrackColor = p.accent, inactiveTrackColor = p.glassHi),
        )
    }
}

@Composable
private fun ThemeSwatch(t: ThemePreset, selected: Boolean, photo: ImageBitmap?, onClick: () -> Unit) {
    val p = LocalPalette.current
    val preview = paletteFor(com.ashu.pocketplanner.data.Settings(theme = t, dark = p.dark))
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(preview.base) + preview.blobs.take(3)))
                .border(if (selected) 3.dp else 1.dp, if (selected) p.accent else p.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (t == ThemePreset.PHOTO) {
                if (photo != null) Image(photo, null, Modifier.size(56.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                else Text("🖼️", fontSize = 22.sp)
            }
        }
        Text(t.label, style = MaterialTheme.typography.labelSmall, color = if (selected) p.accent else p.sub)
    }
}

@Composable
private fun FontSample(f: FontChoice, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    val fonts = remember(f) { fontsFor(f) }
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) p.accent.copy(alpha = 0.2f) else p.glassHi)
            .border(1.dp, if (selected) p.accent else p.border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Aa", fontFamily = fonts.display, fontSize = (24 * fonts.scale).sp, color = p.text, fontWeight = FontWeight.Bold)
        Text(f.label, style = MaterialTheme.typography.labelSmall, color = p.sub)
    }
}
