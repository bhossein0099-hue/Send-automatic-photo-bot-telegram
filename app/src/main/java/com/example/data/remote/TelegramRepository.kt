package com.example.data.remote

import android.content.Context
import android.net.Uri
import com.example.data.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import retrofit2.Retrofit
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

class TelegramRepository(private val context: Context) {

    private val secureStorage = SecureStorage.getInstance(context)

    companion object {
        const val BASE_URL = "https://api.telegram.org/"

        /**
         * Cleans and normalizes a Telegram bot token.
         * Trims whitespace, strips accidental leading "bot" prefix, and filters internal whitespace.
         */
        fun cleanBotToken(rawToken: String?): String {
            if (rawToken.isNullOrBlank()) return ""
            var token = rawToken.trim()
            // If user copied "bot123456:ABC..." from instructions or URL, remove leading "bot"
            if (token.startsWith("bot", ignoreCase = true) && token.length > 3 && token[3].isDigit()) {
                token = token.substring(3)
            }
            // Filter out internal whitespace or hidden chars
            return token.filter { !it.isWhitespace() }
        }

        /**
         * Validates that the token is non-empty and follows standard Telegram bot token pattern:
         * Format: <bot_id>:<secret_hash> (e.g., 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ)
         */
        fun validateBotToken(rawToken: String?): Result<String> {
            val cleanToken = cleanBotToken(rawToken)
            if (cleanToken.isBlank()) {
                return Result.failure(IllegalArgumentException("توکن ربات تلگرام خالی است"))
            }
            if (cleanToken.contains("[BOT_TOKEN") || cleanToken.contains("<ACTUAL") || cleanToken.contains("REDACTED")) {
                return Result.failure(IllegalArgumentException("لطفاً توکن واقعی ربات تلگرام خود را وارد کنید"))
            }
            val tokenRegex = Regex("^\\d{6,15}:[A-Za-z0-9_-]+$")
            if (!cleanToken.matches(tokenRegex)) {
                return Result.failure(IllegalArgumentException("فرمت توکن ربات نامعتبر است. نمونه صحیح: 123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ"))
            }
            return Result.success(cleanToken)
        }

        /**
         * Constructs the Telegram API endpoint URL in the required format:
         * https://api.telegram.org/bot<ACTUAL_BOT_TOKEN>/<method>
         * Properly URL-encodes token components if necessary while preserving the required colon.
         */
        fun buildTelegramUrl(rawToken: String, method: String): String {
            val cleanToken = cleanBotToken(rawToken)
            val cleanMethod = method.trim().removePrefix("/")

            // Properly URL encode token components if necessary while keeping ':'
            val tokenPart = if (cleanToken.contains(':')) {
                val parts = cleanToken.split(':', limit = 2)
                val botId = parts[0]
                val secret = parts[1]
                val safeSecret = java.net.URLEncoder.encode(secret, "UTF-8").replace("+", "%20")
                "$botId:$safeSecret"
            } else {
                java.net.URLEncoder.encode(cleanToken, "UTF-8").replace("+", "%20")
            }

            return "${BASE_URL}bot$tokenPart/$cleanMethod"
        }
    }

    // Token Sanitizer Interceptor ensures the bot token is never exposed in logs or exception messages
    private class TokenSanitizerInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            return try {
                chain.proceed(request)
            } catch (e: Exception) {
                // Ensure exception message does not contain bot token
                val rawMsg = e.message ?: "خطای ارتباط شبکه"
                val sanitizedMsg = rawMsg.replace(Regex("/bot[^/]+/"), "/bot[BOT_TOKEN_REDACTED]/")
                throw java.io.IOException(sanitizedMsg, e)
            }
        }
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .addInterceptor(TokenSanitizerInterceptor())
        .build()

    private val apiService: TelegramApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .build()
        .create(TelegramApiService::class.java)

    /**
     * Test connection to Telegram bot and chat_id
     */
    suspend fun testConnection(token: String, chatId: String): Result<String> = withContext(Dispatchers.IO) {
        val validationResult = validateBotToken(token)
        if (validationResult.isFailure) {
            return@withContext Result.failure(
                validationResult.exceptionOrNull() ?: Exception("توکن ربات تلگرام خالی یا نامعتبر است")
            )
        }
        val cleanToken = validationResult.getOrThrow()

        val cleanChatId = chatId.trim()
        if (cleanChatId.isBlank()) {
            return@withContext Result.failure(Exception("شناسه چت (Chat ID) خالی است"))
        }
        if (!cleanChatId.matches(Regex("^-?\\d+$"))) {
            return@withContext Result.failure(Exception("شناسه چت (Chat ID) باید فقط شامل ارقام عددی باشد"))
        }

        try {
            // First step: getMe to verify bot token and read bot info
            val getMeUrl = buildTelegramUrl(cleanToken, "getMe")
            android.util.Log.d("TelegramRepo", "Calling getMe: ${sanitizeLog(getMeUrl)}")
            val meResponse = apiService.getMe(getMeUrl)
            val meBody = meResponse.body()?.string() ?: meResponse.errorBody()?.string().orEmpty()

            val meJson = runCatching { JSONObject(meBody) }.getOrNull()
            if (!meResponse.isSuccessful || meJson?.optBoolean("ok") != true) {
                val desc = meJson?.optString("description")
                    ?: if (meResponse.code() == 401) "توکن ربات نامعتبر یا منقضی است (Unauthorized)"
                    else if (meResponse.code() == 404) "آدرس ربات تلگرام یافت نشد (Not Found). لطفاً از صحت توکن مطمئن شوید."
                    else "خطا در بررسی توکن ربات (کد ${meResponse.code()})"
                android.util.Log.e("TelegramRepo", "getMe failed: ${sanitizeError(desc)}")
                return@withContext Result.failure(Exception(sanitizeError(desc)))
            }

            val resultObj = meJson.optJSONObject("result")
            val botName = resultObj?.optString("first_name", "ربات") ?: "ربات"
            val botUsername = resultObj?.optString("username", "")

            // Second step: send test message to chat_id using multipart/form-data
            val testMsg = "✅ پیام آزمایشی اتصال\nربات «$botName»${if (!botUsername.isNullOrEmpty()) " (@$botUsername)" else ""} با موفقیت به این چت متصل شد و آماده ارسال عکس‌های جدید دوربین است."
            val sendMessageUrl = buildTelegramUrl(cleanToken, "sendMessage")

            val messageBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", cleanChatId)
                .addFormDataPart("text", testMsg)
                .build()

            android.util.Log.d("TelegramRepo", "Calling sendMessage: ${sanitizeLog(sendMessageUrl)}")
            val sendResponse = apiService.sendMessage(sendMessageUrl, messageBody)
            val sendBody = sendResponse.body()?.string() ?: sendResponse.errorBody()?.string().orEmpty()
            val sendJson = runCatching { JSONObject(sendBody) }.getOrNull()

            if (sendResponse.isSuccessful && sendJson?.optBoolean("ok") == true) {
                Result.success("اتصال موفق بود! پیام تست به تلگرام ارسال شد (ربات: $botName)")
            } else {
                val desc = sendJson?.optString("description")
                    ?: if (sendResponse.code() == 400) "شناسه چت نامعتبر است یا ربات عضو این چت نیست."
                    else "خطا در ارسال پیام به این شناسه چت (کد ${sendResponse.code()})"
                android.util.Log.e("TelegramRepo", "sendMessage failed: ${sanitizeError(desc)}")
                Result.failure(Exception(sanitizeError(desc)))
            }
        } catch (e: Exception) {
            val safeMsg = sanitizeError(e.localizedMessage ?: "عدم امکان برقراری ارتباط با سرورهای تلگرام")
            android.util.Log.e("TelegramRepo", "testConnection exception: $safeMsg")
            Result.failure(Exception(safeMsg))
        }
    }

    /**
     * Upload photo to Telegram using sendPhoto and multipart/form-data
     */
    suspend fun uploadPhoto(
        uri: Uri,
        fileName: String,
        mimeType: String?,
        captionText: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val rawToken = secureStorage.getBotToken()
        val chatId = secureStorage.getChatId().trim()

        val validationResult = validateBotToken(rawToken)
        if (validationResult.isFailure) {
            return@withContext Result.failure(Exception("توکن ربات تلگرام تنظیم نشده یا نامعتبر است"))
        }
        val cleanToken = validationResult.getOrThrow()

        if (chatId.isBlank() || !chatId.matches(Regex("^-?\\d+$"))) {
            return@withContext Result.failure(Exception("شناسه چت (Chat ID) تنظیم نشده یا نامعتبر است"))
        }

        var tempFile: File? = null
        var compressedFile: File? = null

        try {
            // Open image stream safely handling content URIs and file URIs
            val inputStream: InputStream? = try {
                if (uri.scheme == "file") {
                    File(uri.path ?: "").takeIf { it.exists() }?.inputStream()
                } else {
                    context.contentResolver.openInputStream(uri)
                }
            } catch (e: Exception) {
                android.util.Log.e("TelegramRepo", "Cannot open image stream for URI: $uri - ${e.message}")
                null
            }

            if (inputStream == null) {
                return@withContext Result.failure(Exception("امکان دسترسی یا باز کردن فایل عکس وجود ندارد"))
            }

            val ext = if (fileName.contains(".")) fileName.substringAfterLast(".").lowercase() else "jpg"
            tempFile = File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}.$ext")

            FileOutputStream(tempFile).use { out ->
                inputStream.use { input ->
                    input.copyTo(out)
                }
            }

            if (tempFile.length() == 0L) {
                return@withContext Result.failure(Exception("فایل عکس خالی یا خراب است"))
            }

            // Telegram photo upload limit is 10 MB (10 * 1024 * 1024 bytes)
            val maxSizeBytes = 10L * 1024 * 1024
            var fileToUpload = tempFile

            if (tempFile.length() > maxSizeBytes) {
                android.util.Log.w("TelegramRepo", "File exceeds 10MB (${tempFile.length()} bytes), compressing...")
                compressedFile = compressImageFile(tempFile, maxSizeBytes)
                if (compressedFile != null && compressedFile.length() <= maxSizeBytes) {
                    fileToUpload = compressedFile
                } else {
                    return@withContext Result.failure(Exception("حجم عکس بیش از سقف مجاز تلگرام (۱۰ مگابایت) است و امکان فشرده‌سازی آن وجود ندارد"))
                }
            }

            val resolvedMime = when (ext) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                else -> mimeType ?: "image/jpeg"
            }

            val safeFileName = "photo_${System.currentTimeMillis()}.$ext"
            val requestFile = fileToUpload.asRequestBody(resolvedMime.toMediaTypeOrNull())

            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("chat_id", chatId)

            if (!captionText.isNullOrBlank()) {
                multipartBuilder.addFormDataPart("caption", captionText)
            }

            multipartBuilder.addFormDataPart("photo", safeFileName, requestFile)
            val multipartBody = multipartBuilder.build()

            val sendPhotoUrl = buildTelegramUrl(cleanToken, "sendPhoto")
            android.util.Log.d("TelegramRepo", "Sending photo to: ${sanitizeLog(sendPhotoUrl)}, size=${fileToUpload.length()} bytes")

            val response = apiService.sendPhoto(sendPhotoUrl, multipartBody)
            val responseString = response.body()?.string() ?: response.errorBody()?.string().orEmpty()
            val json = runCatching { JSONObject(responseString) }.getOrNull()

            android.util.Log.d("TelegramRepo", "sendPhoto response code: ${response.code()}, ok=${json?.optBoolean("ok")}")

            if (response.isSuccessful && json?.optBoolean("ok") == true) {
                Result.success("با موفقیت ارسال شد")
            } else {
                val desc = json?.optString("description") ?: "خطای ناشناخته از تلگرام (کد ${response.code()})"
                android.util.Log.e("TelegramRepo", "sendPhoto error from Telegram: ${sanitizeError(desc)}")
                Result.failure(Exception(sanitizeError(desc)))
            }
        } catch (e: Exception) {
            val safeMsg = sanitizeError(e.localizedMessage ?: "خطای ارسال در شبکه")
            android.util.Log.e("TelegramRepo", "uploadPhoto exception: $safeMsg")
            Result.failure(Exception(safeMsg))
        } finally {
            try { tempFile?.delete() } catch (_: Exception) {}
            try { compressedFile?.delete() } catch (_: Exception) {}
        }
    }

    private fun compressImageFile(original: File, targetMaxBytes: Long): File? {
        return try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(original.absolutePath, options)

            var sampleSize = 1
            while ((options.outWidth / sampleSize) > 2560 || (options.outHeight / sampleSize) > 2560) {
                sampleSize *= 2
            }

            options.inJustDecodeBounds = false
            options.inSampleSize = sampleSize

            val bitmap = android.graphics.BitmapFactory.decodeFile(original.absolutePath, options) ?: return null
            val outFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")

            var quality = 85
            var compressed: Boolean
            do {
                outFile.delete()
                FileOutputStream(outFile).use { fos ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, fos)
                }
                compressed = outFile.length() <= targetMaxBytes
                quality -= 15
            } while (!compressed && quality >= 40)

            bitmap.recycle()
            outFile
        } catch (e: Exception) {
            android.util.Log.e("TelegramRepo", "Error compressing large photo: ${e.message}")
            null
        }
    }

    private fun sanitizeError(msg: String): String {
        return msg.replace(Regex("\\b[0-9]{6,15}:[a-zA-Z0-9_-]{30,50}\\b"), "[BOT_TOKEN_REDACTED]")
            .replace(Regex("/bot[^/]+/"), "/bot[BOT_TOKEN_REDACTED]/")
            .replace("Unauthorized", "توکن ربات نامعتبر یا منقضی است (Unauthorized)")
            .replace("chat not found", "چت تلگرام یافت نشد. آیا در ربات دکمه Start را زده‌اید؟")
            .replace("bot was blocked by the user", "ربات توسط کاربر مسدود شده است")
            .replace("bot is not a member of the channel chat", "ربات عضو یا ادمین کانال/گروه نیست")
    }

    private fun sanitizeLog(url: String): String {
        return url.replace(Regex("/bot[^/]+/"), "/bot[BOT_TOKEN_REDACTED]/")
    }
}

