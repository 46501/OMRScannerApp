package com.example.omrscannerapp.ui.result

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.omrscannerapp.data.AnswerKey
import com.example.omrscannerapp.data.AnswerKeyRepository
import com.example.omrscannerapp.processing.GradingEngine
import com.example.omrscannerapp.processing.OMRProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ScanProcessingScreen(
    imageUri: Uri,
    answerKeyId: String,
    onSuccess: (List<Int>, AnswerKey) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AnswerKeyRepository(context) }
    var statusText by remember { mutableStateOf("Analyzing OMR sheet...") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val key = repository.getAllKeys().find { it.id == answerKeyId }
                if (key == null) {
                    withContext(Dispatchers.Main) { onError("Answer key not found") }
                    return@withContext
                }

                statusText = "Extracting answers..."
                val detectionResult = OMRProcessor.processSheet(context, imageUri, key.numberOfQuestions, key.optionsPerQuestion)

                if (!detectionResult.success) {
                    withContext(Dispatchers.Main) { onError(detectionResult.errorMessage ?: "Failed to detect OMR") }
                    return@withContext
                }

                withContext(Dispatchers.Main) {
                    statusText = "Please review..."
                    onSuccess(detectionResult.answers, key)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onError(e.message ?: "Unknown error occurred") }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Processing...",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray
            )
        }
    }
}
