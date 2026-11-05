package com.ashu.pocketplanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ashu.pocketplanner.data.Settings

/** Shown once, the very first time the app opens. */
@Composable
fun WelcomeScreen(settings: Settings, onDone: (String) -> Unit) {
    val p = LocalPalette.current
    var name by rememberSaveable { mutableStateOf("") }
    fun finish() = onDone(name.trim())

    Box(Modifier.fillMaxSize()) {
        AppBackground(settings, null)
        Column(
            Modifier.fillMaxSize().systemBarsPadding().imePadding().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("👋", fontSize = 56.sp)
            Spacer(Modifier.height(12.dp))
            Text("Hey there!", style = MaterialTheme.typography.displaySmall, color = p.text, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "This is Pocket Planner, your reminders, notes and calendar in one place. What should I call you?",
                style = MaterialTheme.typography.bodyLarge,
                color = p.sub,
            )
            Spacer(Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Your name") },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                textStyle = MaterialTheme.typography.titleLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = p.accent,
                    unfocusedBorderColor = p.border,
                    focusedTextColor = p.text,
                    unfocusedTextColor = p.text,
                    cursorColor = p.accent,
                    focusedPlaceholderColor = p.sub,
                    unfocusedPlaceholderColor = p.sub,
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (name.isNotBlank()) finish() }),
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { finish() },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.onAccent),
            ) {
                Text("Let's go", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = { onDone("") }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Skip for now", color = p.sub)
            }
        }
    }
}
