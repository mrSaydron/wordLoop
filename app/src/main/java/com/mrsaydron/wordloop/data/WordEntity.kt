package com.mrsaydron.wordloop.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mrsaydron.wordloop.domain.CardStatus

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val word: String,
    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long,
    @ColumnInfo(name = "interval_minutes")
    val intervalMinutes: Long,
    val ef: Double,
    val status: CardStatus
)
