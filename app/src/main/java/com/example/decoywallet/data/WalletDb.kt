package com.example.decoywallet.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/** A persisted decoy transaction. Newest row (highest [seq]) shows at the top. */
@Entity(tableName = "transactions")
data class TxnEntity(
    @PrimaryKey(autoGenerate = true) val seq: Long = 0,
    val txnId: String,
    val name: String,
    val sub: String,
    val amount: Double
)

/** Single-row table holding the current decoy balance. */
@Entity(tableName = "wallet_meta")
data class MetaEntity(
    @PrimaryKey val id: Int = 0,
    val balance: Double
)

@Dao
interface WalletDao {
    @Query("SELECT COUNT(*) FROM transactions")
    fun txnCount(): Int

    @Query("SELECT * FROM transactions ORDER BY seq DESC")
    fun getTransactions(): List<TxnEntity>

    @Insert
    fun insert(txn: TxnEntity): Long

    @Query("SELECT balance FROM wallet_meta WHERE id = 0")
    fun getBalanceOrNull(): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setMeta(meta: MetaEntity)
}

@Database(entities = [TxnEntity::class, MetaEntity::class], version = 1, exportSchema = false)
abstract class WalletDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile private var instance: WalletDatabase? = null

        fun get(context: Context): WalletDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                WalletDatabase::class.java,
                "decoy_wallet.db"
            )
                // Small data set; main-thread access keeps the existing
                // synchronous WalletStore API simple and is fine here.
                .allowMainThreadQueries()
                .build()
                .also { instance = it }
        }
    }
}
