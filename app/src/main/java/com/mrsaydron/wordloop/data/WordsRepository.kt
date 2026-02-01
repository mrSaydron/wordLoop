package com.mrsaydron.wordloop.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

class WordsRepository(private val wordDao: WordDao) {
    fun observeAllWords(): Flow<List<WordEntity>> = wordDao.observeAllWords()

    fun observeDueWordsForToday(): Flow<List<WordEntity>> {
        val endMillis = todayRangeMillis().second
        return wordDao.observeDueWords(endMillis)
    }

    suspend fun insertWord(word: WordEntity): Long = wordDao.insert(word)

    suspend fun updateWord(word: WordEntity) = wordDao.update(word)

    suspend fun deleteWord(word: WordEntity) = wordDao.delete(word)

    suspend fun getByWord(word: String): WordEntity? = wordDao.getByWord(word)

    suspend fun getById(id: Long): WordEntity? = wordDao.getById(id)

    suspend fun getAllWordsOnce(): List<WordEntity> = wordDao.getAllWords()

    private fun todayRangeMillis(): Pair<Long, Long> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        return start to end
    }
}
