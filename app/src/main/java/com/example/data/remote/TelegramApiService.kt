package com.example.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface TelegramApiService {

    @GET
    suspend fun getMe(
        @Url url: String
    ): Response<ResponseBody>

    @POST
    suspend fun sendMessage(
        @Url url: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST
    suspend fun sendPhoto(
        @Url url: String,
        @Body body: RequestBody
    ): Response<ResponseBody>
}


