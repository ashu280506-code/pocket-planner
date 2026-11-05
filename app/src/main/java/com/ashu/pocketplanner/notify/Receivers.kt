package com.ashu.pocketplanner.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.ashu.pocketplanner.data.Repo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

private fun BroadcastReceiver.work(block: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            block()
        } catch (_: Throwable) {
        } finally {
            pending.finish()
        }
    }
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        work { Alarms.runDue(app) }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        work {
            Alarms.ensureChannels(app)
            Repo.get(app).commit()
        }
    }
}

class ActionReceiver : BroadcastReceiver() {
    companion object {
        const val DONE = "com.ashu.pocketplanner.DONE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DONE) return
        val app = context.applicationContext
        val id = intent.getStringExtra("id") ?: return
        val day = intent.getStringExtra("date")?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: LocalDate.now()
        val nid = intent.getIntExtra("nid", 0)
        NotificationManagerCompat.from(app).cancel(nid)
        work { Repo.get(app).tickNow(id, day) }
    }
}
