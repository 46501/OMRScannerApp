package com.example.omrscannerapp.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class ScanResultRepository(private val context: Context) {
    private val resultsFile = File(context.filesDir, "scan_results.json")

    @Synchronized
    fun getAllResults(): List<ScanResult> {
        if (!resultsFile.exists()) return emptyList()
        return try {
            Json.decodeFromString(resultsFile.readText())
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveResult(result: ScanResult) {
        val results = getAllResults().toMutableList()
        val existingIndex = results.indexOfFirst { it.id == result.id }
        if (existingIndex >= 0) {
            results[existingIndex] = result
        } else {
            results.add(0, result) // Add to top
        }
        resultsFile.writeText(Json.encodeToString(results))
    }

    @Synchronized
    fun deleteResult(id: String) {
        val results = getAllResults().filter { it.id != id }
        resultsFile.writeText(Json.encodeToString(results))
    }
}
