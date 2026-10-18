package com.example.decoywallet.model

import android.content.Context
import com.example.decoywallet.data.MetaEntity
import com.example.decoywallet.data.TxnEntity
import com.example.decoywallet.data.WalletDao
import com.example.decoywallet.data.WalletDatabase
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.random.Random

/** One wallet transaction. */
data class Transaction(
    val id: String,
    val name: String,
    val sub: String,
    val amount: Double,          // signed: negative = debit, positive = credit
) {
    val positive: Boolean get() = amount >= 0
    val letter: String get() = name.trim().take(1).uppercase()
}

/**
 * Bank home state, persisted with Room so balance and history survive restarts.
 * All payment flows update this store so the banking experience stays consistent.
 */
object WalletStore {

    private lateinit var dao: WalletDao

    const val STARTING_BALANCE = 124805.56
    val userName = "Aarav Sharma"
    val upiId = "aarav@aubank"
    val cardLast4 = "8847"

    /** Must be called once (from the Application) before any screen reads state. */
    fun init(context: Context) {
        dao = WalletDatabase.get(context).walletDao()
        if (dao.getBalanceOrNull() == null) {
            dao.setMeta(MetaEntity(id = 0, balance = STARTING_BALANCE))
        }
        if (dao.txnCount() == 0) seed()
    }

    /** Seed the believable starting history (inserted bottom-up so order holds). */
    private fun seed() {
        listOf(
            TxnEntity(txnId = "T100238", name = "BESCOM", sub = "Electricity Bill · 27 Sep", amount = -1240.0),
            TxnEntity(txnId = "T100239", name = "Amazon.in", sub = "Shopping · 28 Sep", amount = -1899.0),
            TxnEntity(txnId = "T100240", name = "Rahul Sharma", sub = "Received via UPI · 29 Sep", amount = 2500.0),
            TxnEntity(txnId = "T100241", name = "Jio Recharge", sub = "Mobile · 30 Sep", amount = -299.0),
            TxnEntity(txnId = "T100242", name = "Salary", sub = "Infosys Ltd · 1 Oct", amount = 85000.0),
            TxnEntity(txnId = "T100243", name = "Zomato", sub = "Food & Dining · Today", amount = -428.0)
        ).forEach { dao.insert(it) }
    }

    val balance: Double
        get() = dao.getBalanceOrNull() ?: STARTING_BALANCE

    val transactions: List<Transaction>
        get() = dao.getTransactions().map { Transaction(it.txnId, it.name, it.sub, it.amount) }

    private fun newTxnId(): String = "T" + Random.nextLong(100000000L, 999999999L)

    private fun record(name: String, sub: String, amount: Double): Transaction {
        val newBalance = balance + amount
        dao.setMeta(MetaEntity(id = 0, balance = newBalance))
        val txn = Transaction(newTxnId(), name, sub, amount)
        dao.insert(TxnEntity(txnId = txn.id, name = name, sub = sub, amount = amount))
        return txn
    }

    fun send(name: String, upiOrPhone: String, amount: Double): Transaction =
        record(name, "Sent via UPI · $upiOrPhone", -abs(amount))

    fun addMoney(amount: Double, source: String): Transaction =
        record("Added to Wallet", "via $source", abs(amount))

    fun payMerchant(name: String, sub: String, amount: Double): Transaction =
        record(name, sub, -abs(amount))

    fun payBill(biller: String, amount: Double): Transaction =
        record(biller, "Bill payment", -abs(amount))

    // ---- Indian-style currency formatting (₹1,24,805.56) ----

    private val withPaise = DecimalFormat("##,##,##0.00")
    private val noPaise = DecimalFormat("##,##,##0")

    fun formatInr(amount: Double, paise: Boolean = true): String {
        val f = if (paise) withPaise else noPaise
        return "₹" + f.format(abs(amount))
    }

    /** Signed, no paise — for transaction rows, e.g. "+₹2,500" / "-₹428". */
    fun formatSigned(amount: Double): String {
        val sign = if (amount >= 0) "+" else "-"
        return sign + formatInr(amount, paise = false)
    }
}
