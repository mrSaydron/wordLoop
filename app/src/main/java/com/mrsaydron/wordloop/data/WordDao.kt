package com.mrsaydron.wordloop.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY word COLLATE NOCASE ASC")
    fun observeAllWords(): Flow<List<WordEntity>>

    @Query("SELECT * FROM words")
    suspend fun getAllWords(): List<WordEntity>

    @Query(
        "SELECT * FROM words " +
            "WHERE next_review_date <= :endMillis " +
            "ORDER BY next_review_date ASC"
    )
    fun observeDueWords(endMillis: Long): Flow<List<WordEntity>>

    @Query("SELECT * FROM words WHERE LOWER(word) = LOWER(:word) LIMIT 1")
    suspend fun getByWord(word: String): WordEntity?

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WordEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(word: WordEntity): Long

    @Update
    suspend fun update(word: WordEntity)

    @Delete
    suspend fun delete(word: WordEntity)
}
