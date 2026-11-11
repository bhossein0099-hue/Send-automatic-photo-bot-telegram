package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PhotoUploadEntity
import com.example.data.model.UploadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoUploadDao {

    @Query("SELECT * FROM photo_uploads ORDER BY creationTimestamp DESC")
    fun getAllPhotos(): Flow<List<PhotoUploadEntity>>

    @Query("SELECT * FROM photo_uploads WHERE status = :status ORDER BY creationTimestamp ASC")
    fun getPhotosByStatus(status: UploadStatus): Flow<List<PhotoUploadEntity>>

    @Query("SELECT * FROM photo_uploads WHERE status IN ('PENDING', 'FAILED') ORDER BY creationTimestamp ASC")
    fun getPendingAndFailedPhotosFlow(): Flow<List<PhotoUploadEntity>>

    @Query("SELECT * FROM photo_uploads WHERE status IN ('PENDING', 'FAILED') ORDER BY creationTimestamp ASC")
    suspend fun getPendingAndFailedPhotosList(): List<PhotoUploadEntity>

    @Query("SELECT COUNT(*) FROM photo_uploads WHERE status = 'SENT'")
    fun getSentCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM photo_uploads WHERE status IN ('PENDING', 'UPLOADING', 'FAILED')")
    fun getPendingCount(): Flow<Int>

    @Query("SELECT * FROM photo_uploads WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: Long): PhotoUploadEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM photo_uploads WHERE mediaStoreId = :mediaStoreId LIMIT 1)")
    suspend fun existsByMediaStoreId(mediaStoreId: Long): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM photo_uploads WHERE uriString = :uriString LIMIT 1)")
    suspend fun existsByUri(uriString: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(photo: PhotoUploadEntity): Long

    @Update
    suspend fun update(photo: PhotoUploadEntity)

    @Query("UPDATE photo_uploads SET status = :status, lastError = :error, sentTimestamp = :sentTimestamp WHERE id = :id")
    suspend fun updateStatus(id: Long, status: UploadStatus, error: String?, sentTimestamp: Long?)

    @Query("UPDATE photo_uploads SET status = 'PENDING', retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun markForRetry(id: Long, error: String?)

    @Query("DELETE FROM photo_uploads WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM photo_uploads WHERE status = 'SENT'")
    suspend fun clearSent()

    @Query("SELECT * FROM photo_uploads WHERE status = 'SENT' ORDER BY sentTimestamp DESC LIMIT 1")
    fun getLastSentPhoto(): Flow<PhotoUploadEntity?>
}
