package com.example.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.ServiceCompat
import com.example.TelegramPhotoApp
import com.example.data.model.PhotoUploadEntity
import com.example.data.repository.PhotoRepository
import com.example.data.security.SecureStorage
import com.example.worker.PhotoUploadWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PhotoUploadForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private lateinit var photoRepository: PhotoRepository
    private lateinit var secureStorage: SecureStorage
    private lateinit var observer: MediaStorePhotoObserver
    private var uploadJob: Job? = null
    private val uploadMutex = Mutex()

    private var currentPendingCount = 0
    private var currentSentCount = 0

    override fun onCreate() {
        super.onCreate()
        val app = application as TelegramPhotoApp
        photoRepository = app.photoRepository
        secureStorage = app.secureStorage

        // Start Foreground Notification
        val initialNotification = NotificationHelper.buildServiceNotification(
            this,
            pendingCount = 0,
            sentCount = 0
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            ServiceCompat.startForeground(
                this,
                NotificationHelper.SERVICE_NOTIFICATION_ID,
                initialNotification,
                foregroundServiceType
            )
        } else {
            startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, initialNotification)
        }

        // Register MediaStore Observer
        observer = MediaStorePhotoObserver(
            context = applicationContext,
            photoRepository = photoRepository,
            secureStorage = secureStorage,
            onNewPhotoQueued = {
                triggerUploadQueue()
            }
        )
        try {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (_: Exception) {
        }

        // Monitor pending and sent counts to update notification
        serviceScope.launch {
            combine(
                photoRepository.pendingPhotosCount,
                photoRepository.sentPhotosCount
            ) { pending, sent ->
                Pair(pending, sent)
            }.collect { (pending, sent) ->
                currentPendingCount = pending
                currentSentCount = sent
                updateNotification()
            }
        }

        // Monitor internet status and trigger upload when back online
        serviceScope.launch {
            app.networkMonitor.isConnected.collect { isConnected ->
                if (isConnected) {
                    triggerUploadQueue()
                }
            }
        }

        // Do initial scan in case photos arrived while service was starting
        observer.scanForNewPhotos()
        triggerUploadQueue()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_SERVICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_TRIGGER_UPLOAD -> {
                triggerUploadQueue()
            }
            else -> {
                triggerUploadQueue()
            }
        }
        return START_STICKY
    }

    private fun updateNotification(statusText: String? = null) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val notification = NotificationHelper.buildServiceNotification(
            context = this,
            pendingCount = currentPendingCount,
            sentCount = currentSentCount,
            currentStatus = statusText
        )
        notificationManager?.notify(NotificationHelper.SERVICE_NOTIFICATION_ID, notification)
    }

    fun triggerUploadQueue() {
        if (uploadJob?.isActive == true) {
            return
        }

        uploadJob = serviceScope.launch {
            uploadMutex.withLock {
                processQueue()
            }
        }
    }

    private suspend fun processQueue() {
        val app = application as TelegramPhotoApp
        val networkMonitor = app.networkMonitor
        val telegramRepository = app.telegramRepository

        while (true) {
            // Check internet
            if (!networkMonitor.isCurrentlyConnected()) {
                updateNotification("در انتظار اینترنت")
                // Schedule WorkManager retry with network constraint
                PhotoUploadWorker.enqueueOneTimeWork(applicationContext)
                break
            }

            val pendingList = photoRepository.getPendingList()
            if (pendingList.isEmpty()) {
                updateNotification("همه عکس‌ها ارسال شدند")
                break
            }

            for (photo in pendingList) {
                if (!networkMonitor.isCurrentlyConnected()) {
                    updateNotification("اینترنت قطع شد")
                    PhotoUploadWorker.enqueueOneTimeWork(applicationContext)
                    return
                }

                uploadSinglePhoto(photo, telegramRepository)
                // Small courteous delay between Telegram uploads
                delay(800)
            }
        }
    }

    private suspend fun uploadSinglePhoto(
        photo: PhotoUploadEntity,
        telegramRepository: com.example.data.remote.TelegramRepository
    ) {
        photoRepository.markUploading(photo.id)
        updateNotification("در حال ارسال: ${photo.fileName}")

        val dateFormatted = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
            .format(Date(photo.creationTimestamp))
        val caption = "📷 عکس دوربین جدید\nنام فایل: ${photo.fileName}\nزمان ثبت: $dateFormatted"

        val result = telegramRepository.uploadPhoto(
            uri = Uri.parse(photo.uriString),
            fileName = photo.fileName,
            mimeType = "image/jpeg",
            captionText = caption
        )

        result.fold(
            onSuccess = {
                photoRepository.markSent(photo.id)
                updateNotification()
            },
            onFailure = { error ->
                val errorMsg = error.localizedMessage ?: "خطای ارسال"
                photoRepository.markFailed(photo.id, errorMsg)
                updateNotification("خطا در ارسال: $errorMsg")
                // Enqueue WorkManager with backoff for offline / retry guarantee
                PhotoUploadWorker.enqueueOneTimeWork(applicationContext)
            }
        )
    }

    override fun onDestroy() {
        try {
            contentResolver.unregisterContentObserver(observer)
        } catch (_: Exception) {
        }
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START_SERVICE = "com.example.service.action.START"
        const val ACTION_STOP_SERVICE = "com.example.service.action.STOP"
        const val ACTION_TRIGGER_UPLOAD = "com.example.service.action.TRIGGER_UPLOAD"

        fun start(context: Context) {
            val intent = Intent(context, PhotoUploadForegroundService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PhotoUploadForegroundService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.stopService(intent)
        }

        fun triggerUpload(context: Context) {
            val intent = Intent(context, PhotoUploadForegroundService::class.java).apply {
                action = ACTION_TRIGGER_UPLOAD
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
