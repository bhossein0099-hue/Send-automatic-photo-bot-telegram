package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "photo_uploads",
    indices = [
        Index(value = ["mediaStoreId"], unique = true),
        Index(value = ["status"])
    ]
)
data class PhotoUploadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaStoreId: Long,
    val uriString: String,
    val fileName: String,
    val fileSize: Long,
    val fileHash: String? = null,
    val creationTimestamp: Long,
    val status: UploadStatus = UploadStatus.PENDING,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val sentTimestamp: Long? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)
