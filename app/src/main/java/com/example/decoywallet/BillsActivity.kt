package com.example.decoywallet

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.decoywallet.databinding.ActivityBillsBinding

class BillsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBillsBinding

    private data class Biller(val name: String, val sub: String, val icon: Int, val prefill: Double)

    private val recharge = listOf(
        Biller("Mobile Recharge", "Jio / Airtel / Vi", R.drawable.ic_phone_android, 299.0),
        Biller("DTH Recharge", "Tata Play / Dish TV", R.drawable.ic_tv, 350.0),
        Biller("Broadband", "ACT / JioFiber", R.drawable.ic_wifi, 799.0)
    )
    private val bills = listOf(
        Biller("Electricity", "BESCOM / Adani", R.drawable.ic_bolt, 1240.0),
        Biller("Gas Cylinder", "Indane / HP Gas", R.drawable.ic_flame, 1103.0),
        Biller("Water", "Municipal Board", R.drawable.ic_drop, 420.0),
        Biller("Credit Card", "Pay card bill", R.drawable.ic_card, 0.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        recharge.forEach { binding.gridRecharge.addView(tile(it)) }
        bills.forEach { binding.gridBills.addView(tile(it)) }
    }

    private fun tile(b: Biller): View {
        val dp = resources.displayMetrics.density
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val p = (16 * dp).toInt(); setPadding(p, p, p, p)
            setBackgroundResource(R.drawable.bg_tile)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = (10 * dp).toInt() }
            setOnClickListener { openPay(b) }
        }
        row.addView(ImageView(this).apply {
            setImageResource(b.icon)
            setBackgroundResource(R.drawable.bg_circle_action)
            val sz = (44 * dp).toInt()
            setPadding((11*dp).toInt(),(11*dp).toInt(),(11*dp).toInt(),(11*dp).toInt())
            layoutParams = LinearLayout.LayoutParams(sz, sz)
        })
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding((14*dp).toInt(),0,0,0)
        }
        col.addView(TextView(this).apply {
            text = b.name; textSize = 15f; setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(this@BillsActivity, R.color.text_primary))
        })
        col.addView(TextView(this).apply {
            text = b.sub; textSize = 12f
            setTextColor(ContextCompat.getColor(this@BillsActivity, R.color.text_muted))
        })
        row.addView(col)
        row.addView(ImageView(this).apply {
            setImageResource(R.drawable.ic_chevron)
            val sz = (24 * dp).toInt(); layoutParams = LinearLayout.LayoutParams(sz, sz)
        })
        return row
    }

    private fun openPay(b: Biller) {
        startActivity(
            Intent(this, PayActivity::class.java)
                .putExtra(PayActivity.EXTRA_TITLE, b.name)
                .putExtra(PayActivity.EXTRA_PAYEE, b.name)
                .putExtra(PayActivity.EXTRA_SUB, b.sub)
                .putExtra(PayActivity.EXTRA_ICON, b.icon)
                .putExtra(PayActivity.EXTRA_PREFILL, b.prefill)
                .putExtra(PayActivity.EXTRA_KIND, "bill")
        )
    }
}
