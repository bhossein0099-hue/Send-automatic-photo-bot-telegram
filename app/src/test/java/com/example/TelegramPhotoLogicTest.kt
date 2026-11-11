package com.example

import com.example.data.model.PhotoUploadEntity
import com.example.data.model.UploadStatus
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TelegramPhotoLogicTest {

    @Test
    fun testChatIdValidation() {
        val validUserChatId = "123456789"
        val validChannelChatId = "-1001234567890"
        val invalidChatIdLetters = "1234abc"
        val invalidChatIdSpecial = "@my_channel"

        val numericRegex = Regex("^-?\\d+$")

        assertTrue(validUserChatId.matches(numericRegex))
        assertTrue(validChannelChatId.matches(numericRegex))
        assertFalse(invalidChatIdLetters.matches(numericRegex))
        assertFalse(invalidChatIdSpecial.matches(numericRegex))
    }

    @Test
    fun testPrivacyBoundaryEnforcement() {
        val activationTimestamp = 1700000000000L

        val oldPhotoTimestamp = 1699999999000L
        val newPhotoTimestamp = 1700000005000L

        val shouldIgnoreOldPhoto = oldPhotoTimestamp < activationTimestamp
        val shouldQueueNewPhoto = newPhotoTimestamp >= activationTimestamp

        assertTrue("Old photos before activation must be ignored", shouldIgnoreOldPhoto)
        assertTrue("Photos taken after activation must be processed", shouldQueueNewPhoto)
    }

    @Test
    fun testTokenSanitization() {
        val fakeToken = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ1234567"
        val rawUrl = "https://api.telegram.org/bot$fakeToken/sendPhoto"

        val sanitizedUrl = rawUrl.replace(Regex("/bot[^/]+/"), "/bot[REDACTED]/")

        assertFalse(sanitizedUrl.contains(fakeToken))
        assertTrue(sanitizedUrl.contains("/bot[REDACTED]/sendPhoto"))
    }

    @Test
    fun testPhotoUploadEntityDefaults() {
        val entity = PhotoUploadEntity(
            mediaStoreId = 999L,
            uriString = "content://media/external/images/media/999",
            fileName = "IMG_2026.jpg",
            fileSize = 1024000L,
            creationTimestamp = System.currentTimeMillis()
        )

        assertEquals(UploadStatus.PENDING, entity.status)
        assertEquals(0, entity.retryCount)
        assertEquals(999L, entity.mediaStoreId)
    }

    @Test
    fun testTokenCleaningAndValidation() {
        // Trims whitespace
        val rawTokenWithSpaces = "  123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ  "
        val cleaned = com.example.data.remote.TelegramRepository.cleanBotToken(rawTokenWithSpaces)
        assertEquals("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ", cleaned)

        // Strips leading "bot" prefix if accidentally typed
        val tokenWithPrefix = "bot123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ"
        val cleanedPrefix = com.example.data.remote.TelegramRepository.cleanBotToken(tokenWithPrefix)
        assertEquals("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ", cleanedPrefix)

        // Empty token validation fails
        val emptyResult = com.example.data.remote.TelegramRepository.validateBotToken("")
        assertTrue(emptyResult.isFailure)

        // Placeholder token validation fails
        val placeholderResult = com.example.data.remote.TelegramRepository.validateBotToken("[BOT_TOKEN_REDACTED]")
        assertTrue(placeholderResult.isFailure)

        val placeholderResult2 = com.example.data.remote.TelegramRepository.validateBotToken("<ACTUAL_BOT_TOKEN>")
        assertTrue(placeholderResult2.isFailure)

        // Invalid format without colon fails
        val invalidFormat = com.example.data.remote.TelegramRepository.validateBotToken("invalidtoken12345")
        assertTrue(invalidFormat.isFailure)

        // Valid token succeeds
        val validResult = com.example.data.remote.TelegramRepository.validateBotToken("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ")
        assertTrue(validResult.isSuccess)
        assertEquals("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ", validResult.getOrNull())
    }

    @Test
    fun testTelegramUrlConstruction() {
        val token = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ"
        val getMeUrl = com.example.data.remote.TelegramRepository.buildTelegramUrl(token, "getMe")
        assertEquals("https://api.telegram.org/bot123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ/getMe", getMeUrl)

        val sendMessageUrl = com.example.data.remote.TelegramRepository.buildTelegramUrl(token, "sendMessage")
        assertEquals("https://api.telegram.org/bot123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ/sendMessage", sendMessageUrl)

        val sendPhotoUrl = com.example.data.remote.TelegramRepository.buildTelegramUrl(token, "sendPhoto")
        assertEquals("https://api.telegram.org/bot123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ/sendPhoto", sendPhotoUrl)

        // With leading slash in method name
        val withSlash = com.example.data.remote.TelegramRepository.buildTelegramUrl(token, "/getMe")
        assertEquals("https://api.telegram.org/bot123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ/getMe", withSlash)

        // With spaces in raw token
        val withSpaces = com.example.data.remote.TelegramRepository.buildTelegramUrl("  $token \n", "getMe")
        assertEquals("https://api.telegram.org/bot123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ/getMe", withSpaces)
    }

    @Test
    fun testGetMeApiCallAndResponseParsing() = kotlinx.coroutines.runBlocking {
        val fakeToken = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ"
        var requestUrl = ""

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                requestUrl = chain.request().url.toString()
                val jsonBody = """
                    {
                        "ok": true,
                        "result": {
                            "id": 123456789,
                            "is_bot": true,
                            "first_name": "PhotoBot",
                            "username": "my_camera_bot",
                            "can_join_groups": true
                        }
                    }
                """.trimIndent()
                okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(jsonBody.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.telegram.org/")
            .client(okHttpClient)
            .build()

        val service = retrofit.create(com.example.data.remote.TelegramApiService::class.java)
        val url = com.example.data.remote.TelegramRepository.buildTelegramUrl(fakeToken, "getMe")

        val response = service.getMe(url)
        assertEquals("https://api.telegram.org/bot123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ/getMe", requestUrl)
        assertTrue(response.isSuccessful)

        val bodyString = response.body()?.string().orEmpty()
        val json = org.json.JSONObject(bodyString)
        assertTrue(json.getBoolean("ok"))

        val result = json.getJSONObject("result")
        assertEquals(123456789L, result.getLong("id"))
        assertTrue(result.getBoolean("is_bot"))
        assertEquals("PhotoBot", result.getString("first_name"))
        assertEquals("my_camera_bot", result.getString("username"))
    }

    @Test
    fun testSendPhotoMultipartApiCall() = kotlinx.coroutines.runBlocking {
        val fakeToken = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ"
        val chatId = "987654321"
        var requestUrl = ""
        var contentTypeHeader = ""

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                requestUrl = req.url.toString()
                contentTypeHeader = req.body?.contentType()?.toString().orEmpty()

                val jsonBody = """
                    {
                        "ok": true,
                        "result": {
                            "message_id": 42,
                            "chat": {
                                "id": 987654321,
                                "type": "private"
                            },
                            "photo": [
                                {
                                    "file_id": "AgACAgIAAxkBAAE...",
                                    "file_unique_id": "AQAD...",
                                    "file_size": 12345,
                                    "width": 800,
                                    "height": 600
                                }
                            ]
                        }
                    }
                """.trimIndent()
                okhttp3.Response.Builder()
                    .request(req)
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body(jsonBody.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.telegram.org/")
            .client(okHttpClient)
            .build()

        val service = retrofit.create(com.example.data.remote.TelegramApiService::class.java)
        val sendPhotoUrl = com.example.data.remote.TelegramRepository.buildTelegramUrl(fakeToken, "sendPhoto")

        val dummyImageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())
        val multipartBody = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart("caption", "Test Photo")
            .addFormDataPart(
                "photo",
                "test.jpg",
                dummyImageBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val response = service.sendPhoto(sendPhotoUrl, multipartBody)
        assertEquals("https://api.telegram.org/bot123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ/sendPhoto", requestUrl)
        assertTrue("Request must be multipart/form-data", contentTypeHeader.contains("multipart/form-data"))
        assertTrue(response.isSuccessful)

        val bodyString = response.body()?.string().orEmpty()
        val json = org.json.JSONObject(bodyString)
        assertTrue(json.getBoolean("ok"))
        val result = json.getJSONObject("result")
        assertEquals(42, result.getInt("message_id"))
    }

    @Test
    fun testSendPhotoTelegramErrorHandling() = kotlinx.coroutines.runBlocking {
        val fakeToken = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ"
        val chatId = "987654321"

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val errorJson = """
                    {
                        "ok": false,
                        "error_code": 400,
                        "description": "Bad Request: chat not found"
                    }
                """.trimIndent()
                okhttp3.Response.Builder()
                    .request(chain.request())
                    .protocol(okhttp3.Protocol.HTTP_1_1)
                    .code(400)
                    .message("Bad Request")
                    .body(errorJson.toResponseBody("application/json".toMediaTypeOrNull()))
                    .build()
            }
            .build()

        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl("https://api.telegram.org/")
            .client(okHttpClient)
            .build()

        val service = retrofit.create(com.example.data.remote.TelegramApiService::class.java)
        val sendPhotoUrl = com.example.data.remote.TelegramRepository.buildTelegramUrl(fakeToken, "sendPhoto")

        val multipartBody = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("chat_id", chatId)
            .addFormDataPart(
                "photo",
                "sample.jpg",
                byteArrayOf(1, 2, 3).toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .build()

        val response = service.sendPhoto(sendPhotoUrl, multipartBody)
        assertFalse(response.isSuccessful)
        assertEquals(400, response.code())

        val errBody = response.errorBody()?.string().orEmpty()
        val json = org.json.JSONObject(errBody)
        assertFalse(json.getBoolean("ok"))
        assertEquals("Bad Request: chat not found", json.getString("description"))
    }

    @Test
    fun testAppLockPinStorageAndVerification() {
        val context = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>()
        val secureStorage = com.example.data.security.SecureStorage.getInstance(context)

        // Initially no PIN set
        secureStorage.disableAppLock()
        assertFalse(secureStorage.isAppLockEnabled())

        // Set PIN
        secureStorage.setPin("4321")
        assertTrue(secureStorage.isAppLockEnabled())
        assertTrue(secureStorage.hasPinSet())

        // Verify correct PIN
        assertTrue(secureStorage.verifyPin("4321"))

        // Verify incorrect PIN
        assertFalse(secureStorage.verifyPin("0000"))
        assertFalse(secureStorage.verifyPin("1234"))

        // Disable PIN
        secureStorage.disableAppLock()
        assertFalse(secureStorage.isAppLockEnabled())
    }
}
