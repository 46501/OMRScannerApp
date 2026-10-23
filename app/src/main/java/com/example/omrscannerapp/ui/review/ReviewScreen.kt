package com.example.omrscannerapp.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omrscannerapp.data.AnswerKey
import com.example.omrscannerapp.data.QuestionData
import com.example.omrscannerapp.data.ScanResult
import com.example.omrscannerapp.processing.GradingEngine
import com.example.omrscannerapp.theme.PrimaryColor
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    initialAnswers: List<Int>,
    answerKey: AnswerKey,
    onConfirm: (ScanResult) -> Unit,
    onBack: () -> Unit
) {
    var currentAnswers by remember { mutableStateOf(initialAnswers) }
    val evaluationResult = remember(currentAnswers) { GradingEngine.evaluate(currentAnswers, answerKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Answers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        val questionDataList = evaluationResult.questionResults.map { qr ->
                            QuestionData(
                                questionNumber = qr.questionNumber,
                                studentAnswer = qr.studentAnswer,
                                correctAnswer = qr.correctAnswer,
                                status = qr.status.name,
                                marksAwarded = qr.marksAwarded
                            )
                        }
                        val scanResult = ScanResult(
                            id = UUID.randomUUID().toString(),
                            examName = answerKey.name,
                            dateMillis = System.currentTimeMillis(),
                            finalScore = evaluationResult.finalScore,
                            maxPossibleScore = evaluationResult.maxPossibleScore,
                            correctCount = evaluationResult.correctCount,
                            incorrectCount = evaluationResult.incorrectCount,
                            unansweredCount = evaluationResult.unansweredCount,
                            questionResults = questionDataList
                        )
                        onConfirm(scanResult)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text(
                        text = "Confirm & Save Results",
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
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Score: ${evaluationResult.finalScore.toInt()} / ${evaluationResult.maxPossibleScore.toInt()}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Correct: ${evaluationResult.correctCount}", color = Color(0xFF10B981))
                        Text("Wrong: ${evaluationResult.incorrectCount}", color = Color(0xFFEF4444))
                        Text("Unanswered: ${evaluationResult.unansweredCount}", color = Color.Gray)
                    }
                    if (evaluationResult.multipleMarkedCount > 0) {
                        Spacer(Modifier.height(8.dp))
                        Text("Needs Review (Multiple): ${evaluationResult.multipleMarkedCount}", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Text("Edit Detected Answers", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(vertical = 8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(evaluationResult.questionResults.size) { index ->
                    val qr = evaluationResult.questionResults[index]
                    ReviewQuestionRow(
                        questionResult = qr,
                        optionsCount = answerKey.optionsPerQuestion,
                        onAnswerChange = { newAnswer ->
                            val updated = currentAnswers.toMutableList()
                            updated[index] = newAnswer
                            currentAnswers = updated
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun ReviewQuestionRow(
    questionResult: GradingEngine.QuestionResult,
    optionsCount: Int,
    onAnswerChange: (Int) -> Unit
) {
    val isCorrect = questionResult.status == GradingEngine.AnswerStatus.CORRECT
    val isUnanswered = questionResult.status == GradingEngine.AnswerStatus.UNANSWERED
    val isMultiple = questionResult.status == GradingEngine.AnswerStatus.MULTIPLE_MARKED

    val bgColor = when {
        isCorrect -> Color(0xFF10B981).copy(alpha = 0.05f)
        isUnanswered -> Color.Gray.copy(alpha = 0.05f)
        isMultiple -> Color(0xFFF59E0B).copy(alpha = 0.1f)
        else -> Color(0xFFEF4444).copy(alpha = 0.05f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Q${questionResult.questionNumber}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.width(48.dp)
                )
                
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val options = if (optionsCount == 4) listOf("A", "B", "C", "D") else listOf("A", "B", "C", "D", "E")
                    options.forEachIndexed { optIndex, label ->
                        val selected = questionResult.studentAnswer == optIndex
                        val isActuallyCorrectOption = questionResult.correctAnswer == optIndex
                        
                        // Green if correct, Red if selected and wrong, Gray otherwise
                        val containerColor = when {
                            selected && isCorrect -> Color(0xFF10B981)
                            selected && !isCorrect -> Color(0xFFEF4444)
                            isActuallyCorrectOption -> Color(0xFF10B981).copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                        
                        val contentColor = if (selected) Color.White else if (isActuallyCorrectOption) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                        val borderColor = if (selected) Color.Transparent else if (isActuallyCorrectOption) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable { onAnswerChange(optIndex) }
                                .border(1.dp, borderColor, CircleShape)
                                .background(containerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, color = contentColor, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = { onAnswerChange(-1) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Text("Clear (Unanswered)", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}
