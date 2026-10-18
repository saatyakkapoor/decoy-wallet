package com.example.decoywallet

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.example.decoywallet.databinding.ActivityScanBinding

/** QR Scan & Pay — detects a merchant and opens the payment screen. */
class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private var done = false

    private val merchants = listOf(
        Triple("Chai Point", "Koramangala, Bengaluru", 180.0),
        Triple("More Supermarket", "HSR Layout", 0.0),
        Triple("Namma Metro", "QR Ticket", 60.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        animateScanLine()

        // Simulate detecting a QR after ~1.8s.
        binding.root.postDelayed({
            if (isFinishing || done) return@postDelayed
            done = true
            val (name, area, prefill) = merchants.random()
            binding.scanStatus.text = "QR detected ✓"
            startActivity(
                Intent(this, PayActivity::class.java)
                    .putExtra(PayActivity.EXTRA_TITLE, "Scan & Pay")
                    .putExtra(PayActivity.EXTRA_PAYEE, name)
                    .putExtra(PayActivity.EXTRA_SUB, "$area · Verified merchant")
                    .putExtra(PayActivity.EXTRA_ICON, R.drawable.ic_scan)
                    .putExtra(PayActivity.EXTRA_PREFILL, prefill)
                    .putExtra(PayActivity.EXTRA_KIND, "merchant")
            )
            finish()
        }, 1800)
    }

    private fun animateScanLine() {
        val anim = ObjectAnimator.ofFloat(binding.scanLine, "translationY", -110f, 110f)
        anim.duration = 900
        anim.repeatMode = ObjectAnimator.REVERSE
        anim.repeatCount = ObjectAnimator.INFINITE
        anim.interpolator = LinearInterpolator()
        anim.start()
    }
}
