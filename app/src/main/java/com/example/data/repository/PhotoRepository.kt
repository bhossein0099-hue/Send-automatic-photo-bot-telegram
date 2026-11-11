package com.example.data.repository

import com.example.data.local.PhotoUploadDao
import com.example.data.model.PhotoUploadEntity
import com.example.data.model.UploadStatus
import com.example.data.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PhotoRepository(
    private val dao: PhotoUploadDao,
    private val secureStorage: SecureStorage
) {

    val allPhotos: Flow<List<PhotoUploadEntity>> = dao.getAllPhotos()
    val pendingAndFailedPhotos: Flow<List<PhotoUploadEntity>> = dao.getPendingAndFailedPhotosFlow()
    val sentPhotosCount: Flow<Int> = dao.getSentCount()
    val pendingPhotosCount: Flow<Int> = dao.getPendingCount()
    val lastSentPhoto: Flow<PhotoUploadEntity?> = dao.getLastSentPhoto()

    suspend fun isAlreadyQueued(mediaStoreId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.existsByMediaStoreId(mediaStoreId)
    }

    suspend fun isUriAlreadyQueued(uriString: String): Boolean = withContext(Dispatchers.IO) {
        dao.existsByUri(uriString)
    }

    suspend fun queueNewPhoto(
        mediaStoreId: Long,
        uriString: String,
        fileName: String,
        fileSize: Long,
        creationTimestamp: Long
    ): Long = withContext(Dispatchers.IO) {
        // Enforce privacy rule: only accept if created after or at activation boundary
        val activationTime = secureStorage.getActivationTimestamp()
        if (activationTime > 0 && creationTimestamp < activationTime) {
            return@withContext -1L
        }

        // Duplicate check
        if (dao.existsByMediaStoreId(mediaStoreId)) {
            return@withContext -1L
        }

        val entity = PhotoUploadEntity(
            mediaStoreId = mediaStoreId,
            uriString = uriString,
            fileName = fileName,
            fileSize = fileSize,
            creationTimestamp = creationTimestamp,
            status = UploadStatus.PENDING,
            retryCount = 0
        )
        dao.insert(entity)
    }

    suspend fun getPendingList(): List<PhotoUploadEntity> = withContext(Dispatchers.IO) {
        dao.getPendingAndFailedPhotosList()
    }

    suspend fun markUploading(id: Long) = withContext(Dispatchers.IO) {
        dao.updateStatus(id, UploadStatus.UPLOADING, null, null)
    }

    suspend fun markSent(id: Long) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        dao.updateStatus(id, UploadStatus.SENT, null, now)
        secureStorage.setLastUploadInfo("موفق", now)
    }

    suspend fun markFailed(id: Long, error: String?) = withContext(Dispatchers.IO) {
        dao.updateStatus(id, UploadStatus.FAILED, error, null)
        secureStorage.setLastUploadInfo("خطا: $error", System.currentTimeMillis())
    }

    suspend fun markForRetry(id: Long, error: String? = null) = withContext(Dispatchers.IO) {
        dao.markForRetry(id, error)
    }

    suspend fun deletePhoto(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun clearSent() = withContext(Dispatchers.IO) {
        dao.clearSent()
    }
}
