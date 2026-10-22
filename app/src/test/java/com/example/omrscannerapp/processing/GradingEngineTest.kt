package com.example.omrscannerapp.processing

import com.example.omrscannerapp.data.AnswerKey
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class GradingEngineTest {

    private fun createKey(
        numQuestions: Int,
        opts: Int,
        correctMarks: Float,
        negative: Float,
        answers: List<Int>
    ): AnswerKey {
        return AnswerKey(
            id = UUID.randomUUID().toString(),
            name = "Test Key",
            dateMillis = 0L,
            numberOfQuestions = numQuestions,
            optionsPerQuestion = opts,
            correctMarks = correctMarks,
            negativeMarks = negative,
            answers = answers
        )
    }

    @Test
    fun testAllCorrectAnswers() {
        val key = createKey(5, 4, 1.0f, 0.25f, listOf(0, 1, 2, 3, 0))
        val student = listOf(0, 1, 2, 3, 0)

        val result = GradingEngine.evaluate(student, key)

        assertEquals(5, result.correctCount)
        assertEquals(0, result.incorrectCount)
        assertEquals(0, result.unansweredCount)
        assertEquals(0, result.multipleMarkedCount)
        assertEquals(5.0f, result.finalScore, 0.001f)
    }

    @Test
    fun testMixedAnswersWithNegativeMarking() {
        // Q1: Correct (1.0), Q2: Incorrect (-0.5), Q3: Unanswered (0), Q4: Multiple (0)
        val key = createKey(4, 4, 1.0f, 0.5f, listOf(0, 1, 2, 3))
        val student = listOf(0, 2, -1, -2)

        val result = GradingEngine.evaluate(student, key)

        assertEquals(1, result.correctCount)
        assertEquals(1, result.incorrectCount)
        assertEquals(1, result.unansweredCount)
        assertEquals(1, result.multipleMarkedCount)
        
        // Expected score: 1.0 - 0.5 = 0.5
        assertEquals(0.5f, result.finalScore, 0.001f)
    }

    @Test
    fun testScoreFlooring() {
        // 3 incorrect answers with -1 penalty each -> -3, but floor should be 0
        val key = createKey(3, 4, 1.0f, 1.0f, listOf(0, 0, 0))
        val student = listOf(1, 1, 1)

        val result = GradingEngine.evaluate(student, key)

        assertEquals(0, result.correctCount)
        assertEquals(3, result.incorrectCount)
        assertEquals(0f, result.finalScore, 0.001f)
    }

    @Test
    fun testIncompleteStudentAnswersArray() {
        // Student array is smaller than the key (e.g. processing failed for last question)
        val key = createKey(3, 4, 1.0f, 0.0f, listOf(0, 1, 2))
        val student = listOf(0, 1) // Missing Q3

        val result = GradingEngine.evaluate(student, key)

        assertEquals(2, result.correctCount)
        assertEquals(1, result.unansweredCount)
        assertEquals(2.0f, result.finalScore, 0.001f)
    }
}
