package com.example.decoywallet

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.decoywallet.databinding.ActivityDecoyWalletBinding
import com.example.decoywallet.model.Transaction
import com.example.decoywallet.model.WalletStore
import com.example.decoywallet.prefs.SettingsStore

/** AU-style bank home (Explore / Quick Access) with cash-reward bait. */
class DecoyWalletActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDecoyWalletBinding
    private val settings by lazy { SettingsStore(this) }
    private var exploreSelected = true

    private data class ExploreItem(val label: String, val icon: Int, val action: () -> Unit)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDecoyWalletBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnMenu.setOnClickListener { openClaim() }
        binding.btnSearch.setOnClickListener {
            Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show()
        }
        binding.btnNotify.setOnClickListener {
            Toast.makeText(this, "1 new: ₹25,000 cash reward waiting!", Toast.LENGTH_LONG).show()
            openClaim()
        }
        binding.btnApplyFd.setOnClickListener {
            Toast.makeText(this, "FD application started", Toast.LENGTH_SHORT).show()
        }

        val openReward = View.OnClickListener { openClaim() }
        binding.cashBait.setOnClickListener(openReward)
        binding.btnClaimBait.setOnClickListener(openReward)
        pulseBait()

        binding.tabExplore.setOnClickListener { showExplore() }
        binding.tabQuick.setOnClickListener { showQuick() }

        binding.actionAdd.setOnClickListener { startActivity(Intent(this, AddMoneyActivity::class.java)) }
        binding.actionSend.setOnClickListener { startActivity(Intent(this, SendActivity::class.java)) }
        binding.actionScan.setOnClickListener { startActivity(Intent(this, ScanActivity::class.java)) }
        binding.actionBills.setOnClickListener { startActivity(Intent(this, BillsActivity::class.java)) }

        buildExploreGrid()
        showExplore()
    }

    private fun openClaim() {
        startActivity(Intent(this, ClaimCashActivity::class.java))
    }

    private fun pulseBait() {
        ObjectAnimator.ofFloat(binding.cashBait, View.SCALE_X, 1f, 1.03f, 1f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(binding.cashBait, View.SCALE_Y, 1f, 1.03f, 1f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.greetingHi.text = settings.greetingHeadline()
        binding.greetingSub.text = settings.tagline
        binding.fdRateText.text = settings.fdRateText
        binding.balanceText.text = WalletStore.formatInr(WalletStore.balance)
        binding.upiIdText.text = settings.upiId
        renderTransactions()
    }

    private fun showExplore() {
        exploreSelected = true
        binding.exploreGrid.visibility = View.VISIBLE
        binding.quickPanel.visibility = View.GONE
        binding.fdBanner.visibility = View.VISIBLE
        binding.cashBait.visibility = View.VISIBLE
        binding.tabExploreText.setTextColor(ContextCompat.getColor(this, R.color.brand_primary))
        binding.tabQuickText.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
        binding.tabExploreLine.visibility = View.VISIBLE
        binding.tabQuickLine.visibility = View.INVISIBLE
    }

    private fun showQuick() {
        exploreSelected = false
        binding.exploreGrid.visibility = View.GONE
        binding.quickPanel.visibility = View.VISIBLE
        binding.fdBanner.visibility = View.GONE
        binding.cashBait.visibility = View.VISIBLE
        binding.tabExploreText.setTextColor(ContextCompat.getColor(this, R.color.text_muted))
        binding.tabQuickText.setTextColor(ContextCompat.getColor(this, R.color.brand_primary))
        binding.tabExploreLine.visibility = View.INVISIBLE
        binding.tabQuickLine.visibility = View.VISIBLE
        binding.balanceText.text = WalletStore.formatInr(WalletStore.balance)
        renderTransactions()
    }

    private fun buildExploreGrid() {
        val items = listOf(
            ExploreItem("Savings Account", R.drawable.ic_card) {
                Toast.makeText(this, "Balance ${WalletStore.formatInr(WalletStore.balance)}", Toast.LENGTH_SHORT).show()
            },
            ExploreItem("FD", R.drawable.ic_rupee) {
                Toast.makeText(this, settings.fdRateText, Toast.LENGTH_SHORT).show()
            },
            ExploreItem("Auto Loan", R.drawable.ic_send) {
                Toast.makeText(this, "Auto Loan", Toast.LENGTH_SHORT).show()
            },
            ExploreItem("Bill Pay", R.drawable.ic_bills) {
                startActivity(Intent(this, BillsActivity::class.java))
            },
            ExploreItem("Cash Reward", R.drawable.ic_rupee) { openClaim() },
            ExploreItem("Recharge", R.drawable.ic_phone_android) {
                startActivity(Intent(this, BillsActivity::class.java))
            },
            ExploreItem("Bus", R.drawable.ic_map) {
                Toast.makeText(this, "Bus booking", Toast.LENGTH_SHORT).show()
            },
            ExploreItem("Cab", R.drawable.ic_location) {
                Toast.makeText(this, "Cab booking", Toast.LENGTH_SHORT).show()
            },
            ExploreItem("Hotels", R.drawable.ic_bills) {
                Toast.makeText(this, "Hotels", Toast.LENGTH_SHORT).show()
            },
            ExploreItem("Flights", R.drawable.ic_send) {
                Toast.makeText(this, "Flights", Toast.LENGTH_SHORT).show()
            },
            ExploreItem("Gift Cards", R.drawable.ic_card) {
                Toast.makeText(this, "Gift Cards", Toast.LENGTH_SHORT).show()
            },
            ExploreItem("Apply Now", R.drawable.ic_check) {
                Toast.makeText(this, "Apply Now", Toast.LENGTH_SHORT).show()
            }
        )

        binding.exploreGrid.removeAllViews()
        val dp = resources.displayMetrics.density
        items.forEach { item ->
            val cell = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins((4 * dp).toInt(), (10 * dp).toInt(), (4 * dp).toInt(), (10 * dp).toInt())
                }
                layoutParams = params
                setOnClickListener { item.action() }
            }
            cell.addView(ImageView(this).apply {
                setImageResource(item.icon)
                setBackgroundResource(R.drawable.bg_explore_icon)
                setColorFilter(ContextCompat.getColor(this@DecoyWalletActivity, R.color.brand_primary))
                val s = (56 * dp).toInt()
                val pad = (14 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(s, s)
                setPadding(pad, pad, pad, pad)
            })
            cell.addView(TextView(this).apply {
                text = item.label
                gravity = Gravity.CENTER
                textSize = 11f
                setTextColor(ContextCompat.getColor(this@DecoyWalletActivity, R.color.text_primary))
                setPadding(0, (8 * dp).toInt(), 0, 0)
                maxLines = 2
            })
            binding.exploreGrid.addView(cell)
        }
    }

    private fun renderTransactions() {
        binding.txContainer.removeAllViews()
        val dp = resources.displayMetrics.density
        val list = WalletStore.transactions
        list.forEachIndexed { index, tx ->
            binding.txContainer.addView(row(tx, dp))
            if (index < list.size - 1) binding.txContainer.addView(divider(dp))
        }
    }

    private fun row(tx: Transaction, dp: Float): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val p = (14 * dp).toInt()
            setPadding(p, p, p, p)
        }
        val avatar = TextView(this).apply {
            text = tx.letter
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(this@DecoyWalletActivity, R.color.brand_primary))
            textSize = 16f
            setBackgroundResource(R.drawable.bg_explore_icon)
            val s = (42 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(s, s)
        }
        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding((12 * dp).toInt(), 0, (12 * dp).toInt(), 0)
        }
        textCol.addView(TextView(this).apply {
            text = tx.name
            setTextColor(ContextCompat.getColor(this@DecoyWalletActivity, R.color.text_primary))
            textSize = 15f
        })
        textCol.addView(TextView(this).apply {
            text = tx.sub
            setTextColor(ContextCompat.getColor(this@DecoyWalletActivity, R.color.text_muted))
            textSize = 12f
        })
        val amount = TextView(this).apply {
            text = WalletStore.formatSigned(tx.amount)
            textSize = 15f
            setTextColor(
                ContextCompat.getColor(
                    this@DecoyWalletActivity,
                    if (tx.positive) R.color.success else R.color.text_primary
                )
            )
        }
        row.addView(avatar)
        row.addView(textCol)
        row.addView(amount)
        return row
    }

    private fun divider(dp: Float): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, (1 * dp).toInt()
        ).also { it.marginStart = (66 * dp).toInt() }
        setBackgroundColor(ContextCompat.getColor(this@DecoyWalletActivity, R.color.card_stroke))
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}
