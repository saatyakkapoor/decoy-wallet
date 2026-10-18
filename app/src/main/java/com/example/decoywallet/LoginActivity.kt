package com.example.decoywallet

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.decoywallet.capture.IntruderCapture
import com.example.decoywallet.capture.LocationHelper
import com.example.decoywallet.data.AlertRepository
import com.example.decoywallet.databinding.ActivityLoginBinding
import com.example.decoywallet.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val entered = StringBuilder()
    private lateinit var dots: List<View>
    private val settings by lazy { SettingsStore(this) }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // First launch after install: send the owner to MPIN setup, then the admin panel.
        if (!settings.mpinSet) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dots = listOf(binding.dot0, binding.dot1, binding.dot2, binding.dot3)
        wireKeypad()

        binding.keyBackspace.setOnClickListener { onBackspace() }
        binding.forgotPin.setOnClickListener {
            Toast.makeText(this, "Contact AU Bank customer care to reset MPIN", Toast.LENGTH_SHORT).show()
        }

        requestProtectionPermissionsOnce()
    }

    private fun requestProtectionPermissionsOnce() {
        val needed = mutableListOf<String>()
        fun need(p: String) {
            if (!hasPermission(p)) needed.add(p)
        }
        need(Manifest.permission.CAMERA)
        need(Manifest.permission.ACCESS_FINE_LOCATION)
        need(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (needed.isNotEmpty()) {
            settings.permissionsAsked = true
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun wireKeypad() {
        attachDigitListeners(binding.keypad)
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
        binding.errorText.visibility = View.GONE
        entered.append(d)
        refreshDots()
        if (entered.length == 4) {
            binding.root.postDelayed({ submit() }, 100)
        }
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
                if (i < entered.length) R.drawable.pin_dot_filled
                else R.drawable.pin_dot_empty
            )
        }
    }

    private fun submit() {
        val pin = entered.toString()
        if (pin == settings.ownerMpin) {
            AlertRepository.resetAttempts()
            startActivity(Intent(this, OwnerDashboardActivity::class.java))
            finish()
        } else {
            captureIntruder()
            startActivity(Intent(this, DecoyWalletActivity::class.java))
            finish()
        }
    }

    private fun captureIntruder() {
        val wantPhoto = settings.capturePhoto && hasPermission(Manifest.permission.CAMERA)
        val wantLocation = settings.trackLocation &&
            (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION))
        val appContext = applicationContext
        val deviceId = settings.deviceId
        val lifecycleOwner = ProcessLifecycleOwner.get()

        DecoyApp.appScope.launch {
            val cameras = if (wantPhoto) {
                runCatching { IntruderCapture.captureBoth(appContext, lifecycleOwner) }.getOrNull()
            } else null

            val location = if (wantLocation) {
                withContext(Dispatchers.IO) {
                    runCatching { LocationHelper.current(appContext) }.getOrNull()
                }
            } else null

            // Email cameras + GPS immediately (From: Decoy Wallet <decoy@paperpool.in>)
            AlertRepository.recordIntruder(appContext, deviceId, cameras, location)
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
