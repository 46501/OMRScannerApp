package com.example.omrscannerapp.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AnswerKeyRepository(private val context: Context) {
    private val keysFile = File(context.filesDir, "answer_keys.json")

    fun getAllKeys(): List<AnswerKey> {
        if (!keysFile.exists()) return emptyList()
        return try {
            Json.decodeFromString(keysFile.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveKey(key: AnswerKey) {
        val keys = getAllKeys().toMutableList()
        val existingIndex = keys.indexOfFirst { it.id == key.id }
        if (existingIndex >= 0) {
            keys[existingIndex] = key
        } else {
            keys.add(0, key) // Add to top
        }
        keysFile.writeText(Json.encodeToString(keys))
    }

    fun deleteKey(id: String) {
        val keys = getAllKeys().filter { it.id != id }
        keysFile.writeText(Json.encodeToString(keys))
    }
}
