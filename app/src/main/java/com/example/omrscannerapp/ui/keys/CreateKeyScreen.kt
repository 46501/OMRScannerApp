package com.example.omrscannerapp.ui.keys

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.omrscannerapp.data.AnswerKey
import com.example.omrscannerapp.data.AnswerKeyRepository
import com.example.omrscannerapp.theme.PrimaryColor
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateKeyScreen(
    onBack: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) } // 0: Details, 1: Answers
    
    // Details State
    var examName by remember { mutableStateOf("") }
    var numberOfQuestions by remember { mutableStateOf("") }
    var optionsPerQuestion by remember { mutableIntStateOf(4) }
    var correctMarks by remember { mutableStateOf("1") }
    var negativeMarks by remember { mutableStateOf("0") }
    
    // Answers State
    var answers by remember { mutableStateOf(emptyList<Int>()) }

    val context = LocalContext.current
    val repository = remember { AnswerKeyRepository(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentStep == 0) "Create Answer Key" else "Set Answers") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep == 1) currentStep = 0 else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = {
                        if (currentStep == 0) {
                            val count = numberOfQuestions.toIntOrNull() ?: 0
                            if (examName.isNotBlank() && count > 0) {
                                if (answers.size != count) {
                                    answers = List(count) { -1 }
                                }
                                currentStep = 1
                            }
                        } else {
                            val key = AnswerKey(
                                id = UUID.randomUUID().toString(),
                                name = examName,
                                dateMillis = System.currentTimeMillis(),
                                numberOfQuestions = numberOfQuestions.toIntOrNull() ?: 0,
                                optionsPerQuestion = optionsPerQuestion,
                                correctMarks = correctMarks.toFloatOrNull() ?: 1f,
                                negativeMarks = negativeMarks.toFloatOrNull() ?: 0f,
                                answers = answers
                            )
                            repository.saveKey(key)
                            onBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text(
                        text = if (currentStep == 0) "Next" else "Save Answer Key",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (currentStep == 0) {
                // Step 1: Details
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item {
                        OutlinedTextField(
                            value = examName,
                            onValueChange = { examName = it },
                            label = { Text("Exam Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = numberOfQuestions,
                            onValueChange = { numberOfQuestions = it },
                            label = { Text("Total Questions") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Text("Options Per Question", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(
                                text = "4 (A-D)",
                                selected = optionsPerQuestion == 4,
                                onClick = { optionsPerQuestion = 4 }
                            )
                            ChoiceChip(
                                text = "5 (A-E)",
                                selected = optionsPerQuestion == 5,
                                onClick = { optionsPerQuestion = 5 }
                            )
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = correctMarks,
                            onValueChange = { correctMarks = it },
                            label = { Text("Marks for Correct Answer") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = negativeMarks,
                            onValueChange = { negativeMarks = it },
                            label = { Text("Penalty for Wrong Answer") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Step 2: Set Answers
                val answeredCount = answers.count { it != -1 }
                Text(
                    text = "$answeredCount of ${answers.size} answered",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(answers.size) { index ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Q${index + 1}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.width(48.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val options = if (optionsPerQuestion == 4) listOf("A", "B", "C", "D") else listOf("A", "B", "C", "D", "E")
                                    options.forEachIndexed { optIndex, label ->
                                        BubbleOption(
                                            label = label,
                                            selected = answers[index] == optIndex,
                                            onClick = {
                                                val newAnswers = answers.toMutableList()
                                                newAnswers[index] = optIndex
                                                answers = newAnswers
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun ChoiceChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) PrimaryColor.copy(alpha = 0.1f) else Color.Transparent
    val contentColor = if (selected) PrimaryColor else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) PrimaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .clickable { onClick() }
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(containerColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = contentColor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun BubbleOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val containerColor = if (selected) PrimaryColor else Color.Transparent
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val borderColor = if (selected) PrimaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

    Box(
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() }
            .border(1.dp, borderColor, CircleShape)
            .background(containerColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = contentColor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
    }
}
