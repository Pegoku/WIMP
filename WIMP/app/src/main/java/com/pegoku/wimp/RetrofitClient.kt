package com.pegoku.wimp

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    suspend fun getApiKey(context: android.content.Context): String {
        val settingsDatabase = SettingsDatabase.getDatabase(context)
        val settingsDao = settingsDatabase.settingsDao()
        return settingsDao.getSettings()?.apiKey ?: ""
    }

    //Retrofit client
    private val BASE_URL: String by lazy { dotenv["BASE_URL"] ?: "" }

    // logging of network requests and responses
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // BASIC, HEADERS
    }
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    val instance: Ship24ApiService by lazy {

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Ship24ApiService::class.java)
    }

}
