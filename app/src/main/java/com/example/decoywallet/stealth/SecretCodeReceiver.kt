package com.example.decoywallet.stealth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.decoywallet.LoginActivity

/**
 * Lets the owner re-open the app after stealth mode has hidden the launcher
 * icon. Dialing *#*#426837#*#* fires the SECRET_CODE broadcast, which opens the
 * decoy login screen directly.
 */
class SecretCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SECRET_CODE") {
            val launch = Intent(context, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launch)
        }
    }
}
