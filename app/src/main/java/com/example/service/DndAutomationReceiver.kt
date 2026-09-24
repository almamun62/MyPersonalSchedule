package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.UserPreferencesManager

class DndAutomationReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_DND_TURN_ON = "com.example.ACTION_DND_TURN_ON"
        const val ACTION_DND_TURN_OFF = "com.example.ACTION_DND_TURN_OFF"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i("DndAutomationReceiver", "Received action: $action")
        
        val prefs = UserPreferencesManager.getInstance(context)
        val isAutoDndEnabled = prefs.isAutoDndEnabled.value
        val isDndAllowed = DndManager.checkDndPermission(context)
        
        if (isAutoDndEnabled && isDndAllowed) {
            when (action) {
                ACTION_DND_TURN_ON -> {
                    Log.i("DndAutomationReceiver", "Automating DND: ON")
                    DndManager.setDnd(context, true)
                }
                ACTION_DND_TURN_OFF -> {
                    Log.i("DndAutomationReceiver", "Automating DND: OFF")
                    DndManager.setDnd(context, false)
                }
            }
        } else {
            Log.i("DndAutomationReceiver", "DND Automation skipped. Enabled: $isAutoDndEnabled, Permitted: $isDndAllowed")
        }
    }
}
