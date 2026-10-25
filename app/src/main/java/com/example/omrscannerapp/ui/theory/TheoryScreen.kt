package com.example.omrscannerapp.ui.theory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.omrscannerapp.network.TheoryApiClient
import com.example.omrscannerapp.network.TheoryRequest
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheoryScreen(
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var question by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("General") }
    var detailLevel by remember { mutableStateOf("medium") }
    var detailExpanded by remember { mutableStateOf(false) }

    var responseText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theory Assistant") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Ask a question") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Subject (e.g. Physics)") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = detailExpanded,
                onExpandedChange = { detailExpanded = !detailExpanded }
            ) {
                OutlinedTextField(
                    value = detailLevel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Detail Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = detailExpanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = detailExpanded,
                    onDismissRequest = { detailExpanded = false }
                ) {
                    listOf("short", "medium", "detailed").forEach { level ->
                        DropdownMenuItem(
                            text = { Text(level.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                detailLevel = level
                                detailExpanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    if (question.isBlank()) {
                        errorMessage = "Please enter a question"
                        return@Button
                    }
                    errorMessage = ""
                    responseText = ""
                    isLoading = true

                    coroutineScope.launch {
                        try {
                            val req = TheoryRequest(question.trim(), subject.trim(), detailLevel)
                            val res = TheoryApiClient.apiService.askTheory(req)
                            responseText = res.answer
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            errorMessage = "Failed to connect to backend or Gemini API: ${e.localizedMessage}"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Ask Gemini")
                }
            }

            if (errorMessage.isNotEmpty()) {
                Text(errorMessage, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { errorMessage = "" }) {
                    Text("Clear Error")
                }
            }

            if (responseText.isNotEmpty()) {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Answer", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(responseText, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                TextButton(onClick = {
                    responseText = ""
                    question = ""
                }) {
                    Text("Clear Question & Answer")
                }
            }
        }
    }
}
