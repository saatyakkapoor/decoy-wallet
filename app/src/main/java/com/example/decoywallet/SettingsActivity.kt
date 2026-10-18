package com.example.decoywallet

import android.Manifest
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.decoywallet.databinding.ActivitySettingsBinding
import com.example.decoywallet.databinding.RowEditBinding
import com.example.decoywallet.databinding.RowSwitchBinding
import com.example.decoywallet.prefs.SettingsStore
import com.example.decoywallet.stealth.Stealth

/** Owner settings — MPIN, bank-home profile, alerts, and protection toggles. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val settings by lazy { SettingsStore(this) }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        setupSwitch(
            binding.switchCapture, R.drawable.ic_camera,
            getString(R.string.capture_intruder), getString(R.string.capture_intruder_desc),
            settings.capturePhoto
        ) { on ->
            settings.capturePhoto = on
            if (on) requestPermissions(Manifest.permission.CAMERA)
        }
        setupSwitch(
            binding.switchLocation, R.drawable.ic_location,
            getString(R.string.track_location), getString(R.string.track_location_desc),
            settings.trackLocation
        ) { on ->
            settings.trackLocation = on
            if (on) requestPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        setupSwitch(
            binding.switchStealth, R.drawable.ic_shield,
            getString(R.string.stealth_mode), getString(R.string.stealth_mode_desc),
            settings.stealthEnabled
        ) { on ->
            settings.stealthEnabled = on
            Stealth.setHidden(this, on)
            if (on) {
                AlertDialog.Builder(this)
                    .setTitle("Stealth mode on")
                    .setMessage("App icon hidden. Reopen by dialing *#*#426837#*#*")
                    .setPositiveButton("Got it", null)
                    .show()
            }
        }

        buildProfileOptions()
        buildChannelOptions()
    }

    private fun buildProfileOptions() {
        binding.profileGroup.removeAllViews()
        fun add(title: String, value: String, onClick: () -> Unit) {
            if (binding.profileGroup.childCount > 0) binding.profileGroup.addView(divider())
            val row = RowEditBinding.inflate(LayoutInflater.from(this), binding.profileGroup, false)
            row.editTitle.text = title
            row.editValue.text = value.ifBlank { "Not set" }
            row.root.setOnClickListener { onClick() }
            binding.profileGroup.addView(row.root)
        }

        add("Change MPIN", "••••") { promptChangeMpin() }
        add("Display name", settings.displayName) {
            promptText("Display name", "Shown on the bank home greeting", settings.displayName) {
                settings.displayName = it.ifBlank { settings.displayName }
                buildProfileOptions(); true
            }
        }
        add("Greeting line", settings.greetingLine) {
            promptText("Greeting line", null, settings.greetingLine) {
                settings.greetingLine = it.ifBlank { settings.greetingLine }
                buildProfileOptions(); true
            }
        }
        add("Tagline", settings.tagline) {
            promptText("Tagline", null, settings.tagline) {
                settings.tagline = it.ifBlank { settings.tagline }
                buildProfileOptions(); true
            }
        }
        add("UPI ID", settings.upiId) {
            promptText("UPI ID", null, settings.upiId) {
                settings.upiId = it.ifBlank { settings.upiId }
                buildProfileOptions(); true
            }
        }
        add("Account last 4", settings.accountLast4) {
            promptText("Account last 4 digits", null, settings.accountLast4, InputType.TYPE_CLASS_NUMBER) {
                if (it.length == 4) {
                    settings.accountLast4 = it; buildProfileOptions(); true
                } else {
                    Toast.makeText(this, "Enter exactly 4 digits", Toast.LENGTH_SHORT).show(); false
                }
            }
        }
        add("FD banner text", settings.fdRateText) {
            promptText("FD banner text", null, settings.fdRateText) {
                settings.fdRateText = it.ifBlank { settings.fdRateText }
                buildProfileOptions(); true
            }
        }
        add("Alert email", settings.alertEmail.ifBlank { "Tap to set" }) {
            promptText(
                "Alert email",
                "Intruder alerts are sent here via Brevo SMTP",
                settings.alertEmail,
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            ) {
                settings.alertEmail = it.trim()
                buildProfileOptions(); true
            }
        }
    }

    private fun promptChangeMpin() {
        val density = resources.displayMetrics.density
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * density).toInt(), (8 * density).toInt(), (24 * density).toInt(), 0)
        }
        fun field(hint: String, password: Boolean = true) = EditText(this).apply {
            this.hint = hint
            inputType = if (password) {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            } else InputType.TYPE_CLASS_NUMBER
            maxLines = 1
        }
        val current = field("Current MPIN")
        val next = field("New 4-digit MPIN")
        val confirm = field("Confirm new MPIN")
        box.addView(current)
        box.addView(next)
        box.addView(confirm)

        AlertDialog.Builder(this)
            .setTitle("Change MPIN")
            .setView(box)
            .setPositiveButton("Save") { _, _ ->
                val cur = current.text.toString()
                val n = next.text.toString()
                val c = confirm.text.toString()
                when {
                    cur != settings.ownerMpin ->
                        Toast.makeText(this, "Current MPIN is incorrect", Toast.LENGTH_SHORT).show()
                    n.length != 4 || !n.all { it.isDigit() } ->
                        Toast.makeText(this, "New MPIN must be 4 digits", Toast.LENGTH_SHORT).show()
                    n != c ->
                        Toast.makeText(this, "New MPINs do not match", Toast.LENGTH_SHORT).show()
                    else -> {
                        settings.ownerMpin = n
                        Toast.makeText(this, "MPIN updated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptText(
        title: String,
        message: String?,
        current: String,
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        hint: String? = null,
        onSave: (String) -> Boolean
    ) {
        val input = EditText(this).apply {
            this.inputType = inputType
            setText(current)
            this.hint = hint
            setSelection(text.length)
        }
        val pad = (20 * resources.displayMetrics.density).toInt()
        val wrap = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .apply { if (message != null) setMessage(message) }
            .setView(wrap)
            .setPositiveButton("Save") { _, _ -> onSave(input.text.toString().trim()) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupSwitch(
        row: RowSwitchBinding, icon: Int, title: String, desc: String,
        checked: Boolean, onChange: (Boolean) -> Unit
    ) {
        row.rowIcon.setImageResource(icon)
        row.rowTitle.text = title
        row.rowDesc.text = desc
        row.rowSwitch.setOnCheckedChangeListener(null)
        row.rowSwitch.isChecked = checked
        row.rowSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean -> onChange(isChecked) }
        row.root.setOnClickListener { row.rowSwitch.toggle() }
    }

    private fun requestPermissions(vararg permissions: String) {
        permissionLauncher.launch(arrayOf(*permissions))
    }

    private fun buildChannelOptions() {
        binding.channelGroup.removeAllViews()
        val rowBinding = RowSwitchBinding.inflate(LayoutInflater.from(this), binding.channelGroup, false)
        rowBinding.rowIcon.setImageResource(R.drawable.ic_email)
        rowBinding.rowTitle.text = "Email alerts"
        rowBinding.rowDesc.text = "Send intruder photo + GPS via Brevo SMTP"
        rowBinding.rowSwitch.isChecked = settings.channelEmail
        rowBinding.rowSwitch.setOnCheckedChangeListener { _, isChecked ->
            settings.channelEmail = isChecked
            if (isChecked && settings.alertEmail.isBlank()) {
                Toast.makeText(this, "Set Alert email above", Toast.LENGTH_LONG).show()
            }
        }
        rowBinding.root.setOnClickListener { rowBinding.rowSwitch.toggle() }
        binding.channelGroup.addView(rowBinding.root)
    }

    private fun divider(): View {
        val dp = resources.displayMetrics.density
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
            ).also { it.marginStart = (16 * dp).toInt() }
            setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.card_stroke))
        }
    }
}
