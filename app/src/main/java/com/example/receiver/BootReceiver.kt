package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.security.SecureStorage
import com.example.service.PhotoUploadForegroundService
import com.example.worker.PhotoUploadWorker

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            val secureStorage = SecureStorage.getInstance(context)
            if (secureStorage.isAutoUploadEnabled()) {
                // Restart foreground service to restore MediaStore ContentObserver
                try {
                    PhotoUploadForegroundService.start(context)
                } catch (_: Exception) {
                }

                // Enqueue WorkManager to process any pending uploads in the queue
                PhotoUploadWorker.enqueueOneTimeWork(context)
                PhotoUploadWorker.enqueuePeriodicSync(context)
            }
        }
    }
}
