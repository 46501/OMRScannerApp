package com.example.omrscannerapp.data

import kotlinx.serialization.Serializable

@Serializable
data class AnswerKey(
    val id: String,
    val name: String,
    val dateMillis: Long,
    val numberOfQuestions: Int,
    val optionsPerQuestion: Int,
    val correctMarks: Float,
    val negativeMarks: Float,
    val answers: List<Int> // index = question index, value = correct option index (0 to optionsPerQuestion-1), -1 if not set
)
