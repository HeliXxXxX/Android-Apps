package com.helix.flashcards.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ── Entities ──

@Entity(tableName = "decks")
data class Deck(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "cards",
    foreignKeys = [ForeignKey(
        entity = Deck::class,
        parentColumns = ["id"],
        childColumns = ["deckId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("deckId")]
)
data class Card(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deckId: Long,
    val frontText: String = "",
    val frontImageUri: String? = null,
    val backText: String = "",
    val backImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// ── DAOs ──

@Dao
interface DeckDao {
    @Query("SELECT * FROM decks ORDER BY createdAt DESC")
    fun getAllDecks(): Flow<List<Deck>>

    @Query("SELECT * FROM decks WHERE id = :id")
    suspend fun getDeckById(id: Long): Deck?

    @Insert
    suspend fun insert(deck: Deck): Long

    @Update
    suspend fun update(deck: Deck)

    @Delete
    suspend fun delete(deck: Deck)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY createdAt ASC")
    fun getCardsByDeck(deckId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Long): Card?

    @Query("SELECT * FROM cards WHERE id = :id")
    fun getCardByIdFlow(id: Long): Flow<Card?>

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    fun getCardCount(deckId: Long): Flow<Int>

    @Insert
    suspend fun insert(card: Card): Long

    @Update
    suspend fun update(card: Card)

    @Delete
    suspend fun delete(card: Card)
}

// ── Database ──

@Database(entities = [Deck::class, Card::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "flashcards.db"
                ).build().also { INSTANCE = it }
            }
    }
}