package com.example.decoywallet

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.decoywallet.databinding.ActivityPaymentSuccessBinding
import com.example.decoywallet.model.WalletStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Shared "payment done" confirmation. All callers finish() after launching it. */
class PaymentSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentSuccessBinding

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_AMOUNT = "amount"
        const val EXTRA_TO = "to"
        const val EXTRA_TXN = "txn"
        const val EXTRA_CREDIT = "credit"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
        val credit = intent.getBooleanExtra(EXTRA_CREDIT, false)

        binding.successTitle.text = intent.getStringExtra(EXTRA_TITLE) ?: "Payment Successful"
        binding.successAmount.text = (if (credit) "+ " else "") + WalletStore.formatInr(amount)
        binding.rowTo.text = intent.getStringExtra(EXTRA_TO) ?: "—"
        binding.rowTxn.text = intent.getStringExtra(EXTRA_TXN) ?: "—"
        binding.rowDate.text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH).format(Date())

        binding.btnDone.setOnClickListener { finish() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = finish()
}
