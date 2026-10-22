package com.example.omrscannerapp.processing

import com.example.omrscannerapp.data.AnswerKey

object GradingEngine {

    data class EvaluationResult(
        val totalQuestions: Int,
        val correctCount: Int,
        val incorrectCount: Int,
        val unansweredCount: Int,
        val multipleMarkedCount: Int,
        val totalMarksEarned: Float,
        val totalNegativeMarks: Float,
        val finalScore: Float,
        val maxPossibleScore: Float,
        val questionResults: List<QuestionResult>
    )

    data class QuestionResult(
        val questionNumber: Int,
        val studentAnswer: Int,
        val correctAnswer: Int,
        val status: AnswerStatus,
        val marksAwarded: Float
    )

    enum class AnswerStatus {
        CORRECT, INCORRECT, UNANSWERED, MULTIPLE_MARKED
    }

    fun evaluate(studentAnswers: List<Int>, key: AnswerKey): EvaluationResult {
        var correctCount = 0
        var incorrectCount = 0
        var unansweredCount = 0
        var multipleMarkedCount = 0
        var totalMarksEarned = 0f
        var totalNegativeMarks = 0f
        
        val questionResults = mutableListOf<QuestionResult>()

        val expectedQuestions = key.numberOfQuestions
        
        for (i in 0 until expectedQuestions) {
            val studentAns = if (i < studentAnswers.size) studentAnswers[i] else -1
            val correctAns = if (i < key.answers.size) key.answers[i] else -1
            
            val status: AnswerStatus
            var marks = 0f

            if (studentAns == correctAns && correctAns != -1) {
                status = AnswerStatus.CORRECT
                correctCount++
                marks = key.correctMarks
                totalMarksEarned += marks
            } else if (studentAns == -1) {
                status = AnswerStatus.UNANSWERED
                unansweredCount++
                marks = 0f
            } else if (studentAns == -2) {
                status = AnswerStatus.MULTIPLE_MARKED
                multipleMarkedCount++
                // Typically invalid questions get 0, but some might want negative
                marks = 0f
            } else {
                status = AnswerStatus.INCORRECT
                incorrectCount++
                marks = -key.negativeMarks
                totalNegativeMarks += key.negativeMarks
            }

            questionResults.add(
                QuestionResult(
                    questionNumber = i + 1,
                    studentAnswer = studentAns,
                    correctAnswer = correctAns,
                    status = status,
                    marksAwarded = marks
                )
            )
        }

        val maxPossibleScore = expectedQuestions * key.correctMarks
        val finalScore = maxOf(0f, totalMarksEarned - totalNegativeMarks) // Apply minimum score policy (0 floor)

        return EvaluationResult(
            totalQuestions = expectedQuestions,
            correctCount = correctCount,
            incorrectCount = incorrectCount,
            unansweredCount = unansweredCount,
            multipleMarkedCount = multipleMarkedCount,
            totalMarksEarned = totalMarksEarned,
            totalNegativeMarks = totalNegativeMarks,
            finalScore = finalScore,
            maxPossibleScore = maxPossibleScore,
            questionResults = questionResults
        )
    }
}
