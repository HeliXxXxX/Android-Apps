package com.helix.budget.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Constants ──

const val TYPE_EXPENSE = "EXPENSE"
const val TYPE_INCOME = "INCOME"

// ── Entity ──

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Always positive, stored in minor units (cents). */
    val amountMinor: Long,
    /** [TYPE_EXPENSE] or [TYPE_INCOME]. */
    val type: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// ── DAO ──

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<Transaction>>

    /** Net balance = income − expense, in minor units. */
    @Query(
        "SELECT COALESCE(SUM(CASE WHEN type = '$TYPE_INCOME' THEN amountMinor ELSE -amountMinor END), 0) " +
            "FROM transactions"
    )
    fun balanceMinor(): Flow<Long>

    /** Total spent (expenses only) within a time range, in minor units. */
    @Query(
        "SELECT COALESCE(SUM(amountMinor), 0) FROM transactions " +
            "WHERE type = '$TYPE_EXPENSE' AND createdAt >= :startMillis AND createdAt < :endMillis"
    )
    fun spentBetween(startMillis: Long, endMillis: Long): Flow<Long>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Insert
    suspend fun insert(tx: Transaction): Long

    @Update
    suspend fun update(tx: Transaction)

    @Delete
    suspend fun delete(tx: Transaction)
}

// ── Database ──

@Database(entities = [Transaction::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget.db"
                ).build().also { INSTANCE = it }
            }
    }
}
