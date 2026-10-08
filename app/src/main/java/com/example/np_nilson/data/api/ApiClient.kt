package com.example.np_nilson.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // Default base URL for Android Emulator pointing to host localhost (10.0.2.2)
    private var baseUrl: String = "http://10.0.2.2:8080/"

    fun setBaseUrl(newUrl: String) {
        val formattedUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        baseUrl = formattedUrl
        authApiInstance = createAuthApi()
    }

    fun getBaseUrl(): String = baseUrl

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private var authApiInstance: AuthApi = createAuthApi()

    fun getAuthApi(): AuthApi = authApiInstance

    private fun createAuthApi(): AuthApi {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }
}
