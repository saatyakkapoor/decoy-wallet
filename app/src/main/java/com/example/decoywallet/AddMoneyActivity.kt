package com.example.decoywallet

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.decoywallet.databinding.ActivityAddMoneyBinding
import com.example.decoywallet.model.WalletStore
import com.example.decoywallet.prefs.SettingsStore

class AddMoneyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddMoneyBinding
    private var source = "UPI"

    private val quick = listOf(500, 1000, 2000, 5000)
    private data class Src(val name: String, val icon: Int, val sub: String)
    private lateinit var sources: List<Src>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddMoneyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = SettingsStore(this)
        sources = listOf(
            Src("UPI", R.drawable.ic_scan, prefs.upiId),
            Src("Debit Card", R.drawable.ic_card, "AU Bank •••• ${prefs.accountLast4}"),
            Src("Net Banking", R.drawable.ic_rupee, "AU Small Finance Bank")
        )

        binding.btnBack.setOnClickListener { finish() }
        buildQuickAmounts()
        buildSources()
        binding.btnAdd.setOnClickListener { add() }
    }

    private fun buildQuickAmounts() {
        val dp = resources.displayMetrics.density
        quick.forEach { amt ->
            val chip = TextView(this).apply {
                text = "₹$amt"
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(this@AddMoneyActivity, R.color.text_primary))
                textSize = 14f
                setBackgroundResource(R.drawable.bg_chip)
                val ph = (16 * dp).toInt(); val pv = (10 * dp).toInt()
                setPadding(ph, pv, ph, pv)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .also { it.marginEnd = (8 * dp).toInt() }
                setOnClickListener { binding.inAmount.setText(amt.toString()) }
            }
            binding.quickAmounts.addView(chip)
        }
    }

    private fun buildSources() {
        val dp = resources.displayMetrics.density
        sources.forEachIndexed { i, s ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val p = (14 * dp).toInt(); setPadding(p, p, p, p)
                setBackgroundResource(R.drawable.bg_tile)
                setOnClickListener { source = s.name; refreshSources() }
                tag = s.name
            }
            row.addView(ImageView(this).apply {
                setImageResource(s.icon)
                setBackgroundResource(R.drawable.bg_circle_action)
                val sz = (38 * dp).toInt()
                setPadding((9*dp).toInt(),(9*dp).toInt(),(9*dp).toInt(),(9*dp).toInt())
                layoutParams = LinearLayout.LayoutParams(sz, sz)
            })
            val col = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setPadding((14*dp).toInt(),0,0,0)
            }
            col.addView(TextView(this).apply {
                text = s.name; textSize = 15f
                setTextColor(ContextCompat.getColor(this@AddMoneyActivity, R.color.text_primary))
            })
            col.addView(TextView(this).apply {
                text = s.sub; textSize = 12f
                setTextColor(ContextCompat.getColor(this@AddMoneyActivity, R.color.text_muted))
            })
            row.addView(col)
            val radio = ImageView(this).apply {
                id = View.generateViewId()
                val sz = (22 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(sz, sz)
                tag = "radio_${s.name}"
            }
            row.addView(radio)
            binding.sourceGroup.addView(row)
            if (i < sources.size - 1) binding.sourceGroup.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,(1*dp).toInt())
                    .also { it.marginStart = (66*dp).toInt() }
                setBackgroundColor(ContextCompat.getColor(this@AddMoneyActivity, R.color.card_stroke))
            })
        }
        refreshSources()
    }

    private fun refreshSources() {
        for (i in 0 until binding.sourceGroup.childCount) {
            val row = binding.sourceGroup.getChildAt(i)
            if (row is LinearLayout && row.tag == source) {
                (row.getChildAt(2) as ImageView).setImageResource(R.drawable.ic_check)
            } else if (row is LinearLayout && row.tag != null) {
                (row.getChildAt(2) as ImageView).setImageDrawable(null)
            }
        }
    }

    private fun add() {
        val amount = binding.inAmount.text.toString().trim().toDoubleOrNull() ?: 0.0
        if (amount <= 0) { Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show(); return }
        val txn = WalletStore.addMoney(amount, source)
        startActivity(
            Intent(this, PaymentSuccessActivity::class.java)
                .putExtra(PaymentSuccessActivity.EXTRA_TITLE, "Money Added")
                .putExtra(PaymentSuccessActivity.EXTRA_AMOUNT, amount)
                .putExtra(PaymentSuccessActivity.EXTRA_TO, "Wallet · via $source")
                .putExtra(PaymentSuccessActivity.EXTRA_TXN, txn.id)
                .putExtra(PaymentSuccessActivity.EXTRA_CREDIT, true)
        )
        finish()
    }
}
