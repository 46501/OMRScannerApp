package com.example.omrscannerapp.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

@Serializable
data class TheoryRequest(
    val question: String,
    val subject: String = "General",
    val detail_level: String = "medium"
)

@Serializable
data class TheoryResponse(
    val answer: String,
    val model: String
)

interface TheoryApiService {
    @POST("api/theory/ask")
    suspend fun askTheory(@Body request: TheoryRequest): TheoryResponse
}

object TheoryApiClient {
    // 10.0.2.2 is the special alias for your host loopback interface in the Android Emulator.
    // For a physical device, you would replace this with your machine's local IP address (e.g. 192.168.x.x) or your production domain.
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val json = Json {
        ignoreUnknownKeys = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val apiService: TheoryApiService by lazy {
        retrofit.create(TheoryApiService::class.java)
    }
}
