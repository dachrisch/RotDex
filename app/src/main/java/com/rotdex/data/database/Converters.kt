package com.rotdex.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rotdex.data.models.*

/**
 * Type converters for Room database
 */
class Converters {

    // String List converter
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    // Long List converter (for fusion input card IDs)
    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").map { it.toLong() }
    }

    // CardRarity converter
    @TypeConverter
    fun fromCardRarity(value: CardRarity): String {
        return value.name
    }

    @TypeConverter
    fun toCardRarity(value: String): CardRarity {
        return CardRarity.valueOf(value)
    }

    // CardRarity List converter (for fusion input rarities)
    @TypeConverter
    fun fromCardRarityList(value: List<CardRarity>): String {
        return value.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toCardRarityList(value: String): List<CardRarity> {
        if (value.isEmpty()) return emptyList()
        return value.split(",").map { CardRarity.valueOf(it) }
    }

    // FusionType converter
    @TypeConverter
    fun fromFusionType(value: FusionType): String {
        return value.name
    }

    @TypeConverter
    fun toFusionType(value: String): FusionType {
        return FusionType.valueOf(value)
    }

    // SpinRewardType converter
    @TypeConverter
    fun fromSpinRewardType(value: SpinRewardType): String {
        return value.name
    }

    @TypeConverter
    fun toSpinRewardType(value: String): SpinRewardType {
        return SpinRewardType.valueOf(value)
    }

    // StreakRewardType converter
    @TypeConverter
    fun fromStreakRewardType(value: StreakRewardType): String {
        return value.name
    }

    @TypeConverter
    fun toStreakRewardType(value: String): StreakRewardType {
        return StreakRewardType.valueOf(value)
    }
}
