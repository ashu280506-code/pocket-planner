package com.ashu.pocketplanner.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.ashu.pocketplanner.data.Repo
import com.ashu.pocketplanner.fx.GlyphFx
import com.ashu.pocketplanner.notify.Alarms

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ADD = "open_add"
    }

    private var addKey by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = Repo.get(this)
        Alarms.ensureChannels(this)
        runCatching { Alarms.reschedule(this, repo.state.value) }
        if (savedInstanceState == null) handleIntent(intent)
        setContent { PlannerApp(repo, addKey) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_ADD, false) == true) {
            intent.removeExtra(EXTRA_ADD)
            addKey += 1
        }
    }

    override fun onStart() {
        super.onStart()
        if (Repo.get(this).state.value.settings.glyph) GlyphFx.start(this)
    }

    override fun onStop() {
        GlyphFx.stop()
        super.onStop()
    }
}
