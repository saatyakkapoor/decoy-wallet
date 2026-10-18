package com.example.decoywallet.stealth

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Stealth mode: show or hide the app's launcher icon by enabling / disabling
 * the LauncherAlias component. When hidden, the app is re-opened by dialing the
 * secret code handled by [SecretCodeReceiver].
 */
object Stealth {

    private fun alias(context: Context) =
        ComponentName(context.packageName, "com.example.decoywallet.LauncherAlias")

    /** @param hidden true to hide the launcher icon, false to show it. */
    fun setHidden(context: Context, hidden: Boolean) {
        val state = if (hidden) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        context.packageManager.setComponentEnabledSetting(
            alias(context), state, PackageManager.DONT_KILL_APP
        )
    }
}
