package com.example.service

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.example.data.repository.PhotoRepository
import com.example.data.security.SecureStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MediaStorePhotoObserver(
    private val context: Context,
    private val photoRepository: PhotoRepository,
    private val secureStorage: SecureStorage,
    private val onNewPhotoQueued: (() -> Unit)? = null
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        // Check for newly added camera photos
        scanForNewPhotos()
    }

    fun scanForNewPhotos() {
        scope.launch {
            if (!secureStorage.isAutoUploadEnabled()) {
                return@launch
            }

            val activationBoundaryMillis = secureStorage.getActivationTimestamp()
            if (activationBoundaryMillis <= 0L) {
                return@launch
            }

            // In MediaStore, DATE_ADDED is in seconds, DATE_TAKEN is in milliseconds
            val activationBoundarySeconds = activationBoundaryMillis / 1000L

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.MIME_TYPE
            )

            // Strictly filter only photos created AT or AFTER activation boundary
            // This guarantees existing gallery photos are NEVER scanned or queued.
            val selection = "(${MediaStore.Images.Media.DATE_ADDED} >= ? OR ${MediaStore.Images.Media.DATE_TAKEN} >= ?) AND ${MediaStore.Images.Media.SIZE} > 0"
            val selectionArgs = arrayOf(
                activationBoundarySeconds.toString(),
                activationBoundaryMillis.toString()
            )
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

            var anyNewPhotoAdded = false

            try {
                context.contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    sortOrder
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                    val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                    val dateTakenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)

                    while (cursor.moveToNext()) {
                        val mediaId = cursor.getLong(idCol)
                        val displayName = cursor.getString(nameCol) ?: "photo_$mediaId.jpg"
                        val dateAddedSec = cursor.getLong(dateAddedCol)
                        val dateTakenMs = cursor.getLong(dateTakenCol)
                        val sizeBytes = cursor.getLong(sizeCol)

                        val photoTimestamp = if (dateTakenMs > 0) dateTakenMs else dateAddedSec * 1000L

                        // Double check against boundary
                        if (photoTimestamp < activationBoundaryMillis) {
                            continue
                        }

                        // Duplicate check in Room
                        if (photoRepository.isAlreadyQueued(mediaId)) {
                            continue
                        }

                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            mediaId
                        )

                        val insertedId = photoRepository.queueNewPhoto(
                            mediaStoreId = mediaId,
                            uriString = contentUri.toString(),
                            fileName = displayName,
                            fileSize = sizeBytes,
                            creationTimestamp = photoTimestamp
                        )

                        if (insertedId > 0) {
                            anyNewPhotoAdded = true
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MediaStoreObserver", "Error scanning media store changes: ${e.message}")
            }

            if (anyNewPhotoAdded) {
                onNewPhotoQueued?.invoke()
            }
        }
    }
}
