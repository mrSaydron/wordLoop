package com.mrsaydron.wordloop.data

import androidx.room.TypeConverter
import com.mrsaydron.wordloop.domain.CardStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: CardStatus): String = status.name

    @TypeConverter
    fun toStatus(raw: String): CardStatus = CardStatus.valueOf(raw)
}
