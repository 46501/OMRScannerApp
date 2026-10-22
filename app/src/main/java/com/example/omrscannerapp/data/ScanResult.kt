package com.example.omrscannerapp.data

import com.example.omrscannerapp.processing.GradingEngine
import kotlinx.serialization.Serializable

@Serializable
data class ScanResult(
    val id: String,
    val examName: String,
    val dateMillis: Long,
    val finalScore: Float,
    val maxPossibleScore: Float,
    val correctCount: Int,
    val incorrectCount: Int,
    val unansweredCount: Int,
    val questionResults: List<QuestionData>
)

@Serializable
data class QuestionData(
    val questionNumber: Int,
    val studentAnswer: Int,
    val correctAnswer: Int,
    val status: String,
    val marksAwarded: Float
)
