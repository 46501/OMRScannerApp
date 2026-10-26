package com.example.omrscannerapp.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
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

@Serializable
data class ExtractedQuestion(
    val question_number: Int,
    val question_text: String,
    var max_marks: Float
)

@Serializable
data class ExtractedQuestionsResponse(
    val questions: List<ExtractedQuestion>
)

@Serializable
data class QuestionEvaluation(
    val question_number: Int,
    val question_text: String,
    val max_marks: Float,
    var marks_awarded: Float,
    val student_answer_transcription: String,
    val feedback: String,
    val correct_concepts: String,
    val missing_concepts: String,
    val suggested_improvements: String,
    val model_answer: String,
    val needs_manual_review: Boolean
)

@Serializable
data class EvaluationReport(
    var total_marks_obtained: Float,
    var max_possible_marks: Float,
    var percentage: Float,
    val strengths: String,
    val weaknesses: String,
    val common_mistakes: String,
    val recommendations: String,
    var requires_manual_review: Boolean,
    var question_evaluations: List<QuestionEvaluation>
)

interface TheoryApiService {
    @POST("api/theory/ask")
    suspend fun askTheory(@Body request: TheoryRequest): TheoryResponse

    @Multipart
    @POST("api/theory/extract_questions")
    suspend fun extractQuestions(@Part file: MultipartBody.Part): ExtractedQuestionsResponse

    @Multipart
    @POST("api/theory/evaluate")
    suspend fun evaluateAnswers(
        @Part("questions_json") questionsJson: RequestBody,
        @Part files: List<MultipartBody.Part>
    ): EvaluationReport
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
