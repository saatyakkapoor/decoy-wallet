package com.example.decoywallet

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.decoywallet.databinding.ActivitySendBinding
import com.example.decoywallet.model.WalletStore

class SendActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPay.setOnClickListener { pay() }
    }

    private fun pay() {
        val name = binding.inName.text.toString().trim()
        val upi = binding.inUpi.text.toString().trim()
        val amount = binding.inAmount.text.toString().trim().toDoubleOrNull() ?: 0.0

        if (name.isEmpty()) { toast("Enter a recipient name"); return }
        if (upi.isEmpty()) { toast("Enter a UPI ID or mobile number"); return }
        if (amount <= 0) { toast("Enter a valid amount"); return }

        val txn = WalletStore.send(name, upi, amount)
        startActivity(
            Intent(this, PaymentSuccessActivity::class.java)
                .putExtra(PaymentSuccessActivity.EXTRA_TITLE, "Money Sent")
                .putExtra(PaymentSuccessActivity.EXTRA_AMOUNT, amount)
                .putExtra(PaymentSuccessActivity.EXTRA_TO, "$name ($upi)")
                .putExtra(PaymentSuccessActivity.EXTRA_TXN, txn.id)
        )
        finish()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
