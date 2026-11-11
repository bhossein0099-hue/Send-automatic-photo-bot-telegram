package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.UploadStatus

class Converters {
    @TypeConverter
    fun fromUploadStatus(status: UploadStatus): String {
        return status.name
    }

    @TypeConverter
    fun toUploadStatus(value: String): UploadStatus {
        return runCatching { UploadStatus.valueOf(value) }.getOrDefault(UploadStatus.PENDING)
    }
}
