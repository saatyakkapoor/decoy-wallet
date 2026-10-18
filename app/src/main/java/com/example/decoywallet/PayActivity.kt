package com.example.decoywallet

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.decoywallet.databinding.ActivityPayBinding
import com.example.decoywallet.model.WalletStore

/** Generic "confirm & pay" screen with a preset payee. Used by Scan and Bills. */
class PayActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPayBinding

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_PAYEE = "payee"
        const val EXTRA_SUB = "sub"
        const val EXTRA_ICON = "icon"
        const val EXTRA_PREFILL = "prefill"
        const val EXTRA_KIND = "kind"   // "bill" or "merchant"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Payment"
        val payee = intent.getStringExtra(EXTRA_PAYEE) ?: "Merchant"
        val sub = intent.getStringExtra(EXTRA_SUB) ?: "Verified payee"
        val icon = intent.getIntExtra(EXTRA_ICON, R.drawable.ic_scan)
        val prefill = intent.getDoubleExtra(EXTRA_PREFILL, 0.0)

        binding.screenTitle.text = title
        binding.payeeName.text = payee
        binding.payeeSub.text = sub
        binding.payeeIcon.setImageResource(icon)
        if (prefill > 0) binding.inAmount.setText(prefill.toInt().toString())

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPay.setOnClickListener { pay(payee, sub) }
    }

    private fun pay(payee: String, sub: String) {
        val amount = binding.inAmount.text.toString().trim().toDoubleOrNull() ?: 0.0
        if (amount <= 0) { Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show(); return }

        val kind = intent.getStringExtra(EXTRA_KIND)
        val txn = if (kind == "bill") WalletStore.payBill(payee, amount)
                  else WalletStore.payMerchant(payee, sub, amount)

        startActivity(
            Intent(this, PaymentSuccessActivity::class.java)
                .putExtra(PaymentSuccessActivity.EXTRA_TITLE, "Payment Successful")
                .putExtra(PaymentSuccessActivity.EXTRA_AMOUNT, amount)
                .putExtra(PaymentSuccessActivity.EXTRA_TO, payee)
                .putExtra(PaymentSuccessActivity.EXTRA_TXN, txn.id)
        )
        finish()
    }
}
