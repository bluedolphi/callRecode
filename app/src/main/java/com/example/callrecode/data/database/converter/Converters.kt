package com.example.callrecode.data.database.converter

import androidx.room.TypeConverter
import java.util.Date

/**
 * Type converters for Room database.
 * Handles conversion between complex data types and primitive types that Room can store.
 */
class Converters {
    
    /**
     * Convert timestamp (Long) to Date
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    /**
     * Convert Date to timestamp (Long)
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    /**
     * Convert comma-separated string to List<String>
     */
    @TypeConverter
    fun fromString(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }
    
    /**
     * Convert List<String> to comma-separated string
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }
    
    /**
     * Convert comma-separated string to List<Long>
     */
    @TypeConverter
    fun fromLongString(value: String?): List<Long>? {
        return value?.split(",")?.mapNotNull { 
            try { 
                it.trim().toLong() 
            } catch (e: NumberFormatException) { 
                null 
            } 
        }
    }
    
    /**
     * Convert List<Long> to comma-separated string
     */
    @TypeConverter
    fun fromLongList(list: List<Long>?): String? {
        return list?.joinToString(",")
    }
}