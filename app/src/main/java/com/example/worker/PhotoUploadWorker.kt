package com.example.worker

import android.content.Context
import android.net.Uri
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.TelegramPhotoApp
import com.example.data.local.AppDatabase
import com.example.data.remote.TelegramRepository
import com.example.data.repository.PhotoRepository
import com.example.data.security.SecureStorage
import com.example.service.PhotoUploadForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PhotoUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val secureStorage = SecureStorage.getInstance(applicationContext)
        if (!secureStorage.isAutoUploadEnabled()) {
            return@withContext Result.success()
        }

        // Make sure foreground service is running if auto-upload is enabled
        try {
            PhotoUploadForegroundService.start(applicationContext)
        } catch (_: Exception) {
        }

        val database = AppDatabase.getInstance(applicationContext)
        val photoRepository = PhotoRepository(database.photoUploadDao(), secureStorage)
        val telegramRepository = TelegramRepository(applicationContext)

        val pendingList = photoRepository.getPendingList()
        if (pendingList.isEmpty()) {
            return@withContext Result.success()
        }

        var anyFailed = false

        for (photo in pendingList) {
            photoRepository.markUploading(photo.id)

            val dateFormatted = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
                .format(Date(photo.creationTimestamp))
            val caption = "📷 عکس دوربین جدید (ارسال مجدد هوشمند)\nنام فایل: ${photo.fileName}\nزمان ثبت: $dateFormatted"

            val result = telegramRepository.uploadPhoto(
                uri = Uri.parse(photo.uriString),
                fileName = photo.fileName,
                mimeType = "image/jpeg",
                captionText = caption
            )

            result.fold(
                onSuccess = {
                    photoRepository.markSent(photo.id)
                },
                onFailure = { error ->
                    anyFailed = true
                    photoRepository.markFailed(photo.id, error.localizedMessage ?: "خطای ارسال مجدد")
                }
            )
        }

        if (anyFailed) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        private const val UNIQUE_ONE_TIME_WORK = "telegram_photo_upload_work"
        private const val UNIQUE_PERIODIC_WORK = "telegram_photo_upload_periodic_work"

        fun enqueueOneTimeWork(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<PhotoUploadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,
                    TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_TIME_WORK,
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        fun enqueuePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = PeriodicWorkRequestBuilder<PhotoUploadWorker>(
                15,
                TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
        }
    }
}
