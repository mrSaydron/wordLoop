package com.mrsaydron.wordloop.domain

import com.mrsaydron.wordloop.data.WordEntity
import kotlin.math.max
import kotlin.math.roundToLong

object Sm2Scheduler {
    private const val MIN_EF = 1.3
    private const val DEFAULT_INTERVAL_MINUTES = 1440L
    private const val RESET_REVIEW_DELAY_MINUTES = 10L

    fun updateCard(word: WordEntity, isCorrect: Boolean, nowMillis: Long): WordEntity {
        val q = if (isCorrect) 5 else 0
        return if (!isCorrect) {
            val newEf = if (word.status == CardStatus.PROGRESS_RESET) {
                word.ef
            } else {
                calculateEf(word.ef, q).coerceAtLeast(MIN_EF)
            }
            word.copy(
                status = CardStatus.PROGRESS_RESET,
                ef = newEf,
                intervalMinutes = DEFAULT_INTERVAL_MINUTES,
                nextReviewDate = nowMillis + minutesToMillis(RESET_REVIEW_DELAY_MINUTES)
            )
        } else if (word.status == CardStatus.NEW || word.status == CardStatus.PROGRESS_RESET) {
            word.copy(
                status = CardStatus.IN_PROGRESS,
                intervalMinutes = DEFAULT_INTERVAL_MINUTES,
                nextReviewDate = nowMillis + minutesToMillis(DEFAULT_INTERVAL_MINUTES)
            )
        } else {
            val newEf = calculateEf(word.ef, q).coerceAtLeast(MIN_EF)
            val newInterval = max(1, (word.intervalMinutes * newEf).roundToLong())
            word.copy(
                status = CardStatus.IN_PROGRESS,
                ef = newEf,
                intervalMinutes = newInterval,
                nextReviewDate = nowMillis + minutesToMillis(newInterval)
            )
        }
    }

    private fun calculateEf(ef: Double, q: Int): Double {
        val delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
        return ef + delta
    }

    private fun minutesToMillis(minutes: Long): Long = minutes * 60_000L
}
