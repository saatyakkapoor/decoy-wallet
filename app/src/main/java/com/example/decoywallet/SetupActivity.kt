package com.example.decoywallet

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.decoywallet.databinding.ActivitySetupBinding
import com.example.decoywallet.prefs.SettingsStore

/**
 * First-run screen. The owner picks a 4-digit MPIN (entered twice), then lands
 * straight in the Security Center. Shown only until [SettingsStore.mpinSet] is true.
 */
class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private val settings by lazy { SettingsStore(this) }
    private val entered = StringBuilder()
    private lateinit var dots: List<View>

    /** First PIN captured; null until the owner has entered it once. */
    private var firstPin: String? = null

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dots = listOf(binding.dot0, binding.dot1, binding.dot2, binding.dot3)
        attachDigitListeners(binding.keypad)
        binding.keyBackspace.setOnClickListener { onBackspace() }

        requestProtectionPermissionsOnce()
    }

    private fun attachDigitListeners(group: android.view.ViewGroup) {
        for (i in 0 until group.childCount) {
            when (val child = group.getChildAt(i)) {
                is android.view.ViewGroup -> attachDigitListeners(child)
                is TextView -> child.setOnClickListener { onDigit(child.text.toString()) }
            }
        }
    }

    private fun onDigit(d: String) {
        if (entered.length >= 4) return
        binding.setupError.visibility = View.GONE
        entered.append(d)
        refreshDots()
        if (entered.length == 4) binding.root.postDelayed({ onFourEntered() }, 120)
    }

    private fun onBackspace() {
        if (entered.isNotEmpty()) {
            entered.deleteCharAt(entered.length - 1)
            refreshDots()
        }
    }

    private fun refreshDots() {
        dots.forEachIndexed { i, dot ->
            dot.setBackgroundResource(
                if (i < entered.length) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty
            )
        }
    }

    private fun onFourEntered() {
        val pin = entered.toString()
        val first = firstPin
        if (first == null) {
            firstPin = pin
            clearEntry()
            binding.setupSubtitle.setText(R.string.setup_confirm_pin)
            return
        }
        if (pin != first) {
            firstPin = null
            clearEntry()
            binding.setupSubtitle.setText(R.string.setup_choose_pin)
            showError(getString(R.string.setup_mismatch))
            return
        }
        settings.ownerMpin = pin
        settings.mpinSet = true
        startActivity(
            Intent(this, OwnerDashboardActivity::class.java)
                .putExtra(OwnerDashboardActivity.EXTRA_FIRST_RUN, true)
        )
        finish()
    }

    private fun showError(message: String) {
        binding.setupError.text = message
        binding.setupError.visibility = View.VISIBLE
    }

    private fun clearEntry() {
        entered.clear()
        refreshDots()
    }

    private fun requestProtectionPermissionsOnce() {
        val needed = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        settings.permissionsAsked = true
        permissionLauncher.launch(needed)
    }
}
