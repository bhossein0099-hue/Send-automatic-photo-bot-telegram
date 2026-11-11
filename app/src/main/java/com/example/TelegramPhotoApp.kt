package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.remote.TelegramRepository
import com.example.data.repository.PhotoRepository
import com.example.data.security.SecureStorage
import com.example.service.NotificationHelper
import com.example.util.NetworkMonitor

class TelegramPhotoApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var secureStorage: SecureStorage
        private set

    lateinit var photoRepository: PhotoRepository
        private set

    lateinit var telegramRepository: TelegramRepository
        private set

    lateinit var networkMonitor: NetworkMonitor
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        secureStorage = SecureStorage.getInstance(this)
        photoRepository = PhotoRepository(database.photoUploadDao(), secureStorage)
        telegramRepository = TelegramRepository(this)
        networkMonitor = NetworkMonitor(this)

        NotificationHelper.createNotificationChannels(this)
    }

    companion object {
        lateinit var instance: TelegramPhotoApp
            private set
    }
}
