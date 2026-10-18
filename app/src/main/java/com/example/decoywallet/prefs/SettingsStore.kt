package com.example.decoywallet.prefs

import android.content.Context
import java.util.UUID

/**
 * Owner-controlled preferences: MPIN, bank-home profile text, alert email,
 * and protection toggles.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("decoy_settings", Context.MODE_PRIVATE)

    /** True once the owner has completed first-run MPIN setup. */
    var mpinSet: Boolean
        get() = prefs.getBoolean(KEY_MPIN_SET, false)
        set(v) = prefs.edit().putBoolean(KEY_MPIN_SET, v).apply()

    /** Stable per-install id so each phone only sees its own alerts. */
    val deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) return existing
            val generated = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
            return generated
        }

    var ownerMpin: String
        get() = prefs.getString(KEY_MPIN, "") ?: ""
        set(v) = prefs.edit().putString(KEY_MPIN, v).apply()

    var displayName: String
        get() = prefs.getString(KEY_NAME, "SANJAY") ?: "SANJAY"
        set(v) = prefs.edit().putString(KEY_NAME, v).apply()

    var greetingLine: String
        get() = prefs.getString(KEY_GREETING, "Delighted to see you.") ?: "Delighted to see you."
        set(v) = prefs.edit().putString(KEY_GREETING, v).apply()

    var tagline: String
        get() = prefs.getString(KEY_TAGLINE, "Experience the Simplified Banking.")
            ?: "Experience the Simplified Banking."
        set(v) = prefs.edit().putString(KEY_TAGLINE, v).apply()

    var upiId: String
        get() = prefs.getString(KEY_UPI, "sanjay@aubank") ?: "sanjay@aubank"
        set(v) = prefs.edit().putString(KEY_UPI, v).apply()

    var accountLast4: String
        get() = prefs.getString(KEY_ACC, "8847") ?: "8847"
        set(v) = prefs.edit().putString(KEY_ACC, v).apply()

    var fdRateText: String
        get() = prefs.getString(KEY_FD, "7.19%* Annualised Interest on Fixed Deposit")
            ?: "7.19%* Annualised Interest on Fixed Deposit"
        set(v) = prefs.edit().putString(KEY_FD, v).apply()

    /** Where intruder alert emails are delivered. */
    var alertEmail: String
        get() = prefs.getString(KEY_ALERT_EMAIL, "saatyakkapoor011@gmail.com")
            ?: "saatyakkapoor011@gmail.com"
        set(v) = prefs.edit().putString(KEY_ALERT_EMAIL, v).apply()

    var capturePhoto: Boolean
        get() = prefs.getBoolean(KEY_CAPTURE, true)
        set(v) = prefs.edit().putBoolean(KEY_CAPTURE, v).apply()

    var trackLocation: Boolean
        get() = prefs.getBoolean(KEY_LOCATION, true)
        set(v) = prefs.edit().putBoolean(KEY_LOCATION, v).apply()

    var stealthEnabled: Boolean
        get() = prefs.getBoolean(KEY_STEALTH, false)
        set(v) = prefs.edit().putBoolean(KEY_STEALTH, v).apply()

    var decoyChoice: Int
        get() = prefs.getInt(KEY_DECOY, 0)
        set(v) = prefs.edit().putInt(KEY_DECOY, v).apply()

    /** Email alerts are always on when an alert email address is set. */
    var channelEmail: Boolean
        get() = prefs.getBoolean(KEY_CH_EMAIL, true)
        set(v) = prefs.edit().putBoolean(KEY_CH_EMAIL, v).apply()

    var permissionsAsked: Boolean
        get() = prefs.getBoolean(KEY_PERMS_ASKED, false)
        set(v) = prefs.edit().putBoolean(KEY_PERMS_ASKED, v).apply()

    fun channelSummary(): String = if (channelEmail) "Email" else "None"

    fun greetingHeadline(): String = "Hi ${displayName.uppercase()}, $greetingLine"

    companion object {
        private const val KEY_MPIN = "owner_mpin"
        private const val KEY_MPIN_SET = "mpin_set"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_NAME = "display_name"
        private const val KEY_GREETING = "greeting_line"
        private const val KEY_TAGLINE = "tagline"
        private const val KEY_UPI = "upi_id"
        private const val KEY_ACC = "account_last4"
        private const val KEY_FD = "fd_rate_text"
        private const val KEY_ALERT_EMAIL = "alert_email"
        private const val KEY_CAPTURE = "capture_photo"
        private const val KEY_LOCATION = "track_location"
        private const val KEY_STEALTH = "stealth"
        private const val KEY_DECOY = "decoy_choice"
        private const val KEY_CH_EMAIL = "ch_email"
        private const val KEY_PERMS_ASKED = "perms_asked"
    }
}
