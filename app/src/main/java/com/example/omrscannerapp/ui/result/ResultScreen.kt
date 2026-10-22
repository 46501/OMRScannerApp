package com.example.omrscannerapp.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omrscannerapp.data.ScanResult
import com.example.omrscannerapp.data.QuestionData
import com.example.omrscannerapp.theme.PrimaryColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: ScanResult,
    onBackToHome: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Result") },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Home")
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
                    onClick = onBackToHome,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                ) {
                    Text(
                        text = "Save & Return to Home",
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
            // Header
            Text(
                text = result.examName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(result.dateMillis))
            Text(
                text = "Date: $dateStr",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Score Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Score
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${result.finalScore.toInt()}/${result.maxPossibleScore.toInt()}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = PrimaryColor
                            )
                        )
                        Text(
                            text = "Score",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    // Right: Breakdown
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Correct: ${result.correctCount}")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Incorrect: ${result.incorrectCount}")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Unattempted: ${result.unansweredCount}")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Detailed Results",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Question List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(result.questionResults) { q ->
                    QuestionResultRow(q)
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun QuestionResultRow(q: QuestionData) {
    val isCorrect = q.status == "CORRECT"
    val isUnanswered = q.status == "UNANSWERED"
    val isMultiple = q.status == "MULTIPLE_MARKED"
    
    val bgColor = when {
        isCorrect -> Color(0xFF10B981).copy(alpha = 0.1f)
        isUnanswered -> Color.Gray.copy(alpha = 0.1f)
        isMultiple -> Color(0xFFF59E0B).copy(alpha = 0.1f)
        else -> Color(0xFFEF4444).copy(alpha = 0.1f)
    }

    val iconColor = when {
        isCorrect -> Color(0xFF10B981)
        isUnanswered -> Color.Gray
        isMultiple -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    val icon = when {
        isCorrect -> Icons.Default.CheckCircle
        isUnanswered -> Icons.Default.Warning
        isMultiple -> Icons.Default.Warning
        else -> Icons.Default.Cancel
    }

    fun getOptionLabel(index: Int): String {
        if (index < 0) return "-"
        return ('A' + index).toString()
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Q${q.questionNumber}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(48.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Your Answer: ${if(isMultiple) "Multiple" else getOptionLabel(q.studentAnswer)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Correct: ${getOptionLabel(q.correctAnswer)}",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
            )
        }
        
        Icon(
            imageVector = icon,
            contentDescription = q.status,
            tint = iconColor
        )
    }
}
