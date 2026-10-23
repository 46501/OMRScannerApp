package com.example.omrscannerapp.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AnswerKeyRepositoryTest {

    private lateinit var repository: AnswerKeyRepository
    private lateinit var testFile: File

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        testFile = File(context.filesDir, "answer_keys.json")
        if (testFile.exists()) {
            testFile.delete() // Clean up before test
        }
        repository = AnswerKeyRepository(context)
    }

    @Test
    fun testSaveAndRetrieveAnswerKey() {
        val key = AnswerKey(
            id = UUID.randomUUID().toString(),
            name = "Math Test 101",
            dateMillis = 10000L,
            numberOfQuestions = 10,
            optionsPerQuestion = 4,
            correctMarks = 4.0f,
            negativeMarks = 1.0f,
            answers = listOf(0, 1, 2, 3, 0, 1, 2, 3, 0, 1)
        )

        repository.saveKey(key)

        val keys = repository.getAllKeys()
        assertEquals(1, keys.size)
        val retrieved = keys[0]
        assertEquals("Math Test 101", retrieved.name)
        assertEquals(10, retrieved.numberOfQuestions)
        assertEquals(4, retrieved.optionsPerQuestion)
        assertEquals(4.0f, retrieved.correctMarks)
        assertEquals(1.0f, retrieved.negativeMarks)
    }

    @Test
    fun testUpdateExistingKey() {
        val id = UUID.randomUUID().toString()
        val key = AnswerKey(
            id = id,
            name = "Old Name",
            dateMillis = 10000L,
            numberOfQuestions = 5,
            optionsPerQuestion = 4,
            correctMarks = 1.0f,
            negativeMarks = 0.0f,
            answers = listOf(0, 0, 0, 0, 0)
        )
        repository.saveKey(key)

        val updatedKey = key.copy(name = "New Name", numberOfQuestions = 10)
        repository.saveKey(updatedKey)

        val keys = repository.getAllKeys()
        assertEquals(1, keys.size) // Should not create a duplicate
        assertEquals("New Name", keys[0].name)
        assertEquals(10, keys[0].numberOfQuestions)
    }

    @Test
    fun testDeleteKey() {
        val key = AnswerKey(
            id = UUID.randomUUID().toString(),
            name = "To Be Deleted",
            dateMillis = 10000L,
            numberOfQuestions = 5,
            optionsPerQuestion = 4,
            correctMarks = 1.0f,
            negativeMarks = 0.0f,
            answers = listOf(0, 0, 0, 0, 0)
        )
        repository.saveKey(key)
        
        repository.deleteKey(key.id)
        val keys = repository.getAllKeys()
        assertTrue(keys.isEmpty())
    }
}
