package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.TelegramPhotoApp
import com.example.data.model.PhotoUploadEntity
import com.example.data.remote.TelegramRepository
import com.example.data.security.SecureStorage
import com.example.service.PhotoUploadForegroundService
import com.example.util.BatteryOptimizationHelper
import com.example.worker.PhotoUploadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TestConnectionState {
    data object Idle : TestConnectionState
    data object Loading : TestConnectionState
    data class Success(val message: String) : TestConnectionState
    data class Error(val message: String) : TestConnectionState
}

sealed interface TestUploadState {
    data object Idle : TestUploadState
    data object Uploading : TestUploadState
    data class Success(val message: String) : TestUploadState
    data class Error(val message: String) : TestUploadState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as TelegramPhotoApp
    private val secureStorage = app.secureStorage
    private val photoRepository = app.photoRepository
    private val telegramRepository = app.telegramRepository
    private val networkMonitor = app.networkMonitor

    val isConnectedToInternet: StateFlow<Boolean> = networkMonitor.isConnected

    private val _isAutoUploadEnabled = MutableStateFlow(secureStorage.isAutoUploadEnabled())
    val isAutoUploadEnabled: StateFlow<Boolean> = _isAutoUploadEnabled.asStateFlow()

    private val _lastUploadStatus = MutableStateFlow(secureStorage.getLastUploadStatus())
    val lastUploadStatus: StateFlow<String> = _lastUploadStatus.asStateFlow()

    private val _lastUploadTime = MutableStateFlow(secureStorage.getLastUploadTimestamp())
    val lastUploadTime: StateFlow<Long> = _lastUploadTime.asStateFlow()

    private val _isBatteryRestricted = MutableStateFlow(!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(application))
    val isBatteryRestricted: StateFlow<Boolean> = _isBatteryRestricted.asStateFlow()

    val sentPhotosCount: StateFlow<Int> = photoRepository.sentPhotosCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pendingPhotosCount: StateFlow<Int> = photoRepository.pendingPhotosCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allPhotos: StateFlow<List<PhotoUploadEntity>> = photoRepository.allPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingPhotos: StateFlow<List<PhotoUploadEntity>> = photoRepository.pendingAndFailedPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _botToken = MutableStateFlow(secureStorage.getBotToken())
    val botToken: StateFlow<String> = _botToken.asStateFlow()

    private val _chatId = MutableStateFlow(secureStorage.getChatId())
    val chatId: StateFlow<String> = _chatId.asStateFlow()

    // App Lock State
    private val _isAppLockEnabled = MutableStateFlow(secureStorage.isAppLockEnabled())
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(!secureStorage.isAppLockEnabled())
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    fun unlockWithPin(pin: String): Boolean {
        val success = secureStorage.verifyPin(pin)
        if (success) {
            _isAuthenticated.value = true
        }
        return success
    }

    fun lockApp() {
        if (_isAppLockEnabled.value) {
            _isAuthenticated.value = false
        }
    }

    fun enableAppLock(newPin: String): Boolean {
        val trimmed = newPin.trim()
        if (trimmed.length < 4) return false
        secureStorage.setPin(trimmed)
        _isAppLockEnabled.value = true
        _isAuthenticated.value = true
        return true
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!secureStorage.verifyPin(currentPin)) return false
        val trimmed = newPin.trim()
        if (trimmed.length < 4) return false
        secureStorage.setPin(trimmed)
        return true
    }

    fun disableAppLock(currentPin: String): Boolean {
        if (!secureStorage.verifyPin(currentPin)) return false
        secureStorage.disableAppLock()
        _isAppLockEnabled.value = false
        _isAuthenticated.value = true
        return true
    }

    private val _testConnectionState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testConnectionState: StateFlow<TestConnectionState> = _testConnectionState.asStateFlow()

    private val _testUploadState = MutableStateFlow<TestUploadState>(TestUploadState.Idle)
    val testUploadState: StateFlow<TestUploadState> = _testUploadState.asStateFlow()

    fun refreshBatteryOptimizationStatus() {
        _isBatteryRestricted.value = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(getApplication())
    }

    fun toggleAutoUpload(enabled: Boolean, context: Context) {
        secureStorage.setAutoUploadEnabled(enabled)
        _isAutoUploadEnabled.value = enabled

        if (enabled) {
            PhotoUploadForegroundService.start(context)
            PhotoUploadWorker.enqueuePeriodicSync(context)
        } else {
            PhotoUploadForegroundService.stop(context)
        }
    }

    fun saveSettings(token: String, newChatId: String): Boolean {
        if (_isAppLockEnabled.value && !_isAuthenticated.value) {
            return false
        }
        val trimmedChatId = newChatId.trim()
        val isNumeric = trimmedChatId.matches(Regex("^-?\\d+$"))
        if (!isNumeric) {
            return false
        }
        val cleanToken = TelegramRepository.cleanBotToken(token)
        if (cleanToken.isBlank()) {
            return false
        }
        secureStorage.saveBotToken(cleanToken)
        secureStorage.saveChatId(trimmedChatId)
        _botToken.value = cleanToken
        _chatId.value = trimmedChatId
        return true
    }

    fun testConnection(token: String, targetChatId: String) {
        val cleanToken = TelegramRepository.cleanBotToken(token)
        val validation = TelegramRepository.validateBotToken(cleanToken)
        if (validation.isFailure) {
            _testConnectionState.value = TestConnectionState.Error(
                validation.exceptionOrNull()?.message ?: "فرمت توکن ربات تلگرام نامعتبر است"
            )
            return
        }

        val cleanChatId = targetChatId.trim()
        if (cleanChatId.isBlank() || !cleanChatId.matches(Regex("^-?\\d+$"))) {
            _testConnectionState.value = TestConnectionState.Error("شناسه چت (Chat ID) باید فقط عددی باشد (مثلاً 123456789 یا 1001234567890-)")
            return
        }

        viewModelScope.launch {
            _testConnectionState.value = TestConnectionState.Loading
            val result = telegramRepository.testConnection(cleanToken, cleanChatId)
            result.fold(
                onSuccess = { msg ->
                    _testConnectionState.value = TestConnectionState.Success(msg)
                },
                onFailure = { err ->
                    _testConnectionState.value = TestConnectionState.Error(err.localizedMessage ?: "خطای اتصال")
                }
            )
        }
    }

    fun resetTestConnectionState() {
        _testConnectionState.value = TestConnectionState.Idle
    }

    fun sendTestPhoto(uri: Uri, context: Context) {
        viewModelScope.launch {
            _testUploadState.value = TestUploadState.Uploading

            val rawToken = secureStorage.getBotToken()
            val chatId = secureStorage.getChatId()
            val tokenCheck = TelegramRepository.validateBotToken(rawToken)
            if (tokenCheck.isFailure) {
                _testUploadState.value = TestUploadState.Error(
                    tokenCheck.exceptionOrNull()?.message ?: "توکن ربات تلگرام نامعتبر یا تنظیم نشده است"
                )
                return@launch
            }
            if (chatId.isBlank() || !chatId.matches(Regex("^-?\\d+$"))) {
                _testUploadState.value = TestUploadState.Error("شناسه چت (Chat ID) نامعتبر یا تنظیم نشده است")
                return@launch
            }

            val fileName = "photo_${System.currentTimeMillis()}.jpg"
            val result = telegramRepository.uploadPhoto(
                uri = uri,
                fileName = fileName,
                mimeType = "image/jpeg",
                captionText = "🧪 ارسال آزمایشی عکس از برنامه ارسال خودکار عکس به تلگرام"
            )
            result.fold(
                onSuccess = {
                    _testUploadState.value = TestUploadState.Success("عکس با موفقیت به تلگرام فرستاده شد!")
                    updateLastUploadInfo("موفق (ارسال دستی)")
                },
                onFailure = { err ->
                    val errorMsg = err.localizedMessage ?: "خطا در ارسال عکس"
                    _testUploadState.value = TestUploadState.Error(errorMsg)
                    updateLastUploadInfo("خطا در ارسال: $errorMsg")
                }
            )
        }
    }

    fun resetTestUploadState() {
        _testUploadState.value = TestUploadState.Idle
    }

    fun retryPhoto(id: Long, context: Context) {
        viewModelScope.launch {
            photoRepository.markForRetry(id)
            PhotoUploadForegroundService.triggerUpload(context)
            PhotoUploadWorker.enqueueOneTimeWork(context)
        }
    }

    fun deletePhoto(id: Long) {
        viewModelScope.launch {
            photoRepository.deletePhoto(id)
        }
    }

    fun clearSentPhotos() {
        viewModelScope.launch {
            photoRepository.clearSent()
        }
    }

    private fun updateLastUploadInfo(status: String) {
        val now = System.currentTimeMillis()
        _lastUploadStatus.value = status
        _lastUploadTime.value = now
        secureStorage.setLastUploadInfo(status, now)
    }
}
