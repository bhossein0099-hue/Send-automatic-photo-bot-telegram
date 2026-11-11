package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SecureStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private var fallbackKey: SecretKey? = null

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // Fallback for JVM test environments where AndroidKeyStore hardware provider is unavailable
            if (fallbackKey == null) {
                try {
                    val kgen = KeyGenerator.getInstance("AES")
                    kgen.init(256)
                    fallbackKey = kgen.generateKey()
                } catch (_: Exception) {}
            }
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: fallbackKey
        } catch (_: Exception) {
            fallbackKey
        }
    }

    private fun encrypt(plainText: String): String? {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = getSecretKey() ?: return null
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    private fun decrypt(encryptedBase64: String?): String? {
        if (encryptedBase64.isNullOrEmpty()) return ""
        return try {
            val secretKey = getSecretKey() ?: return null
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return null
            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decrypted = cipher.doFinal(cipherText)
            String(decrypted, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    fun saveBotToken(token: String) {
        val trimmed = token.trim()
        val encrypted = encrypt(trimmed)
        prefs.edit().putString(KEY_ENCRYPTED_TOKEN, encrypted).apply()
    }

    fun getBotToken(): String {
        val encrypted = prefs.getString(KEY_ENCRYPTED_TOKEN, null) ?: return ""
        return decrypt(encrypted) ?: ""
    }

    fun saveChatId(chatId: String) {
        prefs.edit().putString(KEY_CHAT_ID, chatId.trim()).apply()
    }

    fun getChatId(): String {
        return prefs.getString(KEY_CHAT_ID, "") ?: ""
    }

    fun setAutoUploadEnabled(enabled: Boolean) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_AUTO_UPLOAD, enabled)
        if (enabled) {
            // Set activation timestamp boundary if not set or freshly enabled
            val currentActivation = prefs.getLong(KEY_ACTIVATION_TIMESTAMP, 0L)
            if (currentActivation == 0L) {
                editor.putLong(KEY_ACTIVATION_TIMESTAMP, System.currentTimeMillis())
            }
        }
        editor.apply()
    }

    fun isAutoUploadEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_UPLOAD, false)
    }

    fun setActivationTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_ACTIVATION_TIMESTAMP, timestamp).apply()
    }

    fun getActivationTimestamp(): Long {
        return prefs.getLong(KEY_ACTIVATION_TIMESTAMP, 0L)
    }

    fun setLastUploadInfo(status: String, timestamp: Long) {
        prefs.edit()
            .putString(KEY_LAST_UPLOAD_STATUS, status)
            .putLong(KEY_LAST_UPLOAD_TIMESTAMP, timestamp)
            .apply()
    }

    fun getLastUploadStatus(): String {
        return prefs.getString(KEY_LAST_UPLOAD_STATUS, "") ?: ""
    }

    fun getLastUploadTimestamp(): Long {
        return prefs.getLong(KEY_LAST_UPLOAD_TIMESTAMP, 0L)
    }

    fun isAppLockEnabled(): Boolean {
        return prefs.getBoolean(KEY_APP_LOCK_ENABLED, false) && hasPinSet()
    }

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun setPin(pin: String) {
        val encrypted = encrypt(pin.trim())
        prefs.edit()
            .putString(KEY_ENCRYPTED_PIN, encrypted)
            .putBoolean(KEY_APP_LOCK_ENABLED, true)
            .apply()
    }

    fun hasPinSet(): Boolean {
        val encrypted = prefs.getString(KEY_ENCRYPTED_PIN, null)
        return !encrypted.isNullOrBlank()
    }

    fun verifyPin(inputPin: String): Boolean {
        val encrypted = prefs.getString(KEY_ENCRYPTED_PIN, null) ?: return false
        val savedPin = decrypt(encrypted) ?: return false
        return savedPin == inputPin.trim()
    }

    fun disableAppLock() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_PIN)
            .putBoolean(KEY_APP_LOCK_ENABLED, false)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "secure_telegram_prefs"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "telegram_bot_credentials_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12

        private const val KEY_ENCRYPTED_TOKEN = "enc_bot_token"
        private const val KEY_CHAT_ID = "telegram_chat_id"
        private const val KEY_AUTO_UPLOAD = "auto_upload_enabled"
        private const val KEY_ACTIVATION_TIMESTAMP = "activation_boundary_timestamp"
        private const val KEY_LAST_UPLOAD_STATUS = "last_upload_status"
        private const val KEY_LAST_UPLOAD_TIMESTAMP = "last_upload_timestamp"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
        private const val KEY_ENCRYPTED_PIN = "enc_app_lock_pin"

        @Volatile
        private var INSTANCE: SecureStorage? = null

        fun getInstance(context: Context): SecureStorage {
            return INSTANCE ?: synchronized(this) {
                val instance = SecureStorage(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
