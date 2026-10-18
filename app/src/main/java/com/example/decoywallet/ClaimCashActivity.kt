package com.example.decoywallet

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.decoywallet.databinding.ActivityClaimCashBinding

/** Flashy cash-reward bait — keeps the intruder in the payment flow. */
class ClaimCashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClaimCashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClaimCashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnClaimNow.setOnClickListener {
            // Lead them into Add Money so it feels like withdrawing/claiming cash
            startActivity(
                Intent(this, AddMoneyActivity::class.java)
                    .putExtra("from_claim", true)
            )
            finish()
        }
        binding.btnLater.setOnClickListener { finish() }
    }
}
