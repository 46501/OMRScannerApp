package com.example.omrscannerapp.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class AnswerKeyRepository(private val context: Context) {
    private val keysFile = File(context.filesDir, "answer_keys.json")

    @Synchronized
    fun getAllKeys(): List<AnswerKey> {
        if (!keysFile.exists()) return emptyList()
        return try {
            Json.decodeFromString(keysFile.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveKey(key: AnswerKey) {
        val keys = getAllKeys().toMutableList()
        val existingIndex = keys.indexOfFirst { it.id == key.id }
        if (existingIndex >= 0) {
            keys[existingIndex] = key
        } else {
            keys.add(0, key) // Add to top
        }
        val tempFile = File(context.filesDir, "answer_keys_tmp.json")
        try {
            tempFile.writeText(Json.encodeToString(keys))
            tempFile.renameTo(keysFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun deleteKey(id: String) {
        val keys = getAllKeys().filter { it.id != id }
        val tempFile = File(context.filesDir, "answer_keys_tmp.json")
        try {
            tempFile.writeText(Json.encodeToString(keys))
            tempFile.renameTo(keysFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
