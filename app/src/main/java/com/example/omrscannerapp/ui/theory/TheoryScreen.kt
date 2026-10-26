package com.example.omrscannerapp.ui.theory

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.omrscannerapp.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

enum class EvaluatorState {
    INPUT_MODE,
    EDIT_QUESTIONS,
    UPLOAD_ANSWERS,
    EVALUATING,
    REPORT
}

fun uriToFile(context: Context, uri: Uri): File? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val file = File.createTempFile("upload", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(file)
        inputStream?.copyTo(outputStream)
        inputStream?.close()
        outputStream.close()
        file
    } catch (e: Exception) {
        null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheoryScreen(onBack: () -> Unit) {
    var currentState by remember { mutableStateOf(EvaluatorState.INPUT_MODE) }
    var questions by remember { mutableStateOf(mutableListOf<ExtractedQuestion>()) }
    var studentImages by remember { mutableStateOf(listOf<Uri>()) }
    var evaluationReport by remember { mutableStateOf<EvaluationReport?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val questionPaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                currentState = EvaluatorState.EDIT_QUESTIONS
                isProcessing = true
                coroutineScope.launch {
                    try {
                        val file = withContext(Dispatchers.IO) { uriToFile(context, uri) }
                        if (file != null) {
                            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
                            val response = TheoryApiClient.apiService.extractQuestions(body)
                            questions = response.questions.toMutableList()
                        } else {
                            errorMessage = "Failed to load image."
                        }
                    } catch (e: Exception) {
                        errorMessage = "Failed to extract questions: ${e.message}"
                    } finally {
                        isProcessing = false
                    }
                }
            }
        }
    )

    val studentAnswersLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            if (uris.isNotEmpty()) {
                studentImages = uris
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Answer Evaluator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            if (errorMessage.isNotEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(errorMessage, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }

            when (currentState) {
                EvaluatorState.INPUT_MODE -> {
                    Text("Step 1: Enter Exam Questions", style = MaterialTheme.typography.titleLarge)
                    Button(
                        onClick = {
                            questions = mutableListOf()
                            currentState = EvaluatorState.EDIT_QUESTIONS
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Write Questions Manually")
                    }
                    Button(
                        onClick = { questionPaperLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Extract from Question Paper Image")
                    }
                }

                EvaluatorState.EDIT_QUESTIONS -> {
                    Text("Step 1: Configure Questions & Marks", style = MaterialTheme.typography.titleLarge)

                    if (isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("Extracting questions with Gemini...", modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        questions.forEachIndexed { index, q ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Q${q.question_number}", style = MaterialTheme.typography.titleMedium)
                                        IconButton(onClick = {
                                            val newList = questions.toMutableList()
                                            newList.removeAt(index)
                                            questions = newList
                                        }) {
                                            Icon(Icons.Default.Delete, "Delete")
                                        }
                                    }
                                    OutlinedTextField(
                                        value = q.question_text,
                                        onValueChange = {
                                            val newList = questions.toMutableList()
                                            newList[index] = q.copy(question_text = it)
                                            questions = newList
                                        },
                                        label = { Text("Question Text") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = q.max_marks.toString(),
                                        onValueChange = {
                                            val newList = questions.toMutableList()
                                            newList[index] = q.copy(max_marks = it.toFloatOrNull() ?: 0f)
                                            questions = newList
                                        },
                                        label = { Text("Max Marks") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val newList = questions.toMutableList()
                                newList.add(ExtractedQuestion(newList.size + 1, "", 0f))
                                questions = newList
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, "Add")
                            Spacer(Modifier.width(8.dp))
                            Text("Add Question")
                        }

                        Button(
                            onClick = { currentState = EvaluatorState.UPLOAD_ANSWERS },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = questions.isNotEmpty()
                        ) {
                            Text("Next: Upload Student Answers")
                        }
                    }
                }

                EvaluatorState.UPLOAD_ANSWERS -> {
                    Text("Step 2: Upload Student Answers", style = MaterialTheme.typography.titleLarge)
                    Text("Selected ${studentImages.size} pages.")

                    Button(
                        onClick = { studentAnswersLauncher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Select Answer Sheet Images")
                    }

                    Button(
                        onClick = {
                            errorMessage = ""
                            currentState = EvaluatorState.EVALUATING
                            coroutineScope.launch {
                                try {
                                    val parts = studentImages.mapNotNull { uri ->
                                        val f = withContext(Dispatchers.IO) { uriToFile(context, uri) }
                                        if (f != null) {
                                            val reqFile = f.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                            MultipartBody.Part.createFormData("files", f.name, reqFile)
                                        } else null
                                    }
                                    if (parts.isEmpty()) throw Exception("No valid images selected.")

                                    val qJson = Json.encodeToString(questions)
                                    val qReq = qJson.toRequestBody("application/json".toMediaTypeOrNull())

                                    val report = TheoryApiClient.apiService.evaluateAnswers(qReq, parts)
                                    evaluationReport = report
                                    currentState = EvaluatorState.REPORT
                                } catch (e: Exception) {
                                    errorMessage = "Evaluation failed: ${e.message}"
                                    currentState = EvaluatorState.UPLOAD_ANSWERS
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = studentImages.isNotEmpty()
                    ) {
                        Text("Submit for Evaluation")
                    }
                }

                EvaluatorState.EVALUATING -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Evaluating student answers...")
                        Text("This may take a moment.")
                    }
                }

                EvaluatorState.REPORT -> {
                    val report = evaluationReport
                    if (report != null) {
                        Text("Evaluation Report", style = MaterialTheme.typography.headlineMedium)
                        
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                Text("Score: ${report.total_marks_obtained} / ${report.max_possible_marks}", style = MaterialTheme.typography.titleLarge)
                                Text("Percentage: ${report.percentage}%")
                                if (report.requires_manual_review) {
                                    Text("⚠️ Requires Manual Review", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Text("Strengths", style = MaterialTheme.typography.titleMedium)
                        Text(report.strengths)
                        
                        Text("Weaknesses", style = MaterialTheme.typography.titleMedium)
                        Text(report.weaknesses)

                        Text("Recommendations", style = MaterialTheme.typography.titleMedium)
                        Text(report.recommendations)

                        Spacer(Modifier.height(16.dp))
                        Text("Question Breakdown", style = MaterialTheme.typography.titleLarge)

                        report.question_evaluations.forEachIndexed { index, q ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Text("Q${q.question_number}: ${q.question_text}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                        Text("${q.marks_awarded} / ${q.max_marks}", style = MaterialTheme.typography.titleMedium)
                                    }
                                    if (q.needs_manual_review) {
                                        Text("⚠️ Unreadable/Ambiguous", color = MaterialTheme.colorScheme.error)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text("Student Wrote:", style = MaterialTheme.typography.labelLarge)
                                    Text(q.student_answer_transcription)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Feedback:", style = MaterialTheme.typography.labelLarge)
                                    Text(q.feedback)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Model Answer:", style = MaterialTheme.typography.labelLarge)
                                    Text(q.model_answer)
                                }
                            }
                        }

                        Button(onClick = { currentState = EvaluatorState.INPUT_MODE }, modifier = Modifier.fillMaxWidth()) {
                            Text("Start New Evaluation")
                        }
                    }
                }
            }
        }
    }
}
