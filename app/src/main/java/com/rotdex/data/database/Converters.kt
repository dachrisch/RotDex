package com.rotdex.data.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rotdex.data.models.SpinRewardType
import com.rotdex.data.models.StreakRewardType

/**
 * Type converters for Room database
 */
class Converters {

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromSpinRewardType(value: SpinRewardType): String {
        return value.name
    }

    @TypeConverter
    fun toSpinRewardType(value: String): SpinRewardType {
        return SpinRewardType.valueOf(value)
    }

    @TypeConverter
    fun fromStreakRewardType(value: StreakRewardType): String {
        return value.name
    }

    @TypeConverter
    fun toStreakRewardType(value: String): StreakRewardType {
        return StreakRewardType.valueOf(value)
    }
}
