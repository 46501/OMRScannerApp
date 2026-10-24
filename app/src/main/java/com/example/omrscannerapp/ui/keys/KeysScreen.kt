package com.example.omrscannerapp.ui.keys

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.omrscannerapp.data.AnswerKey
import com.example.omrscannerapp.data.AnswerKeyRepository
import com.example.omrscannerapp.theme.PrimaryColor
import java.text.SimpleDateFormat
import java.util.*
import com.example.omrscannerapp.export.PdfGenerator
import androidx.core.content.FileProvider
import android.content.Intent

@Composable
fun KeysScreen(
    modifier: Modifier = Modifier,
    onCreateKeyClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AnswerKeyRepository(context) }
    var keys by remember { mutableStateOf(repository.getAllKeys()) }
    var showDeleteConfirm by remember { mutableStateOf<AnswerKey?>(null) }

    fun printKey(key: AnswerKey) {
        val file = PdfGenerator.generateOmrSheet(context, key)
        if (file != null) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Open OMR Sheet"))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Answer Key",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Button(
                onClick = onCreateKeyClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create New")
                Spacer(Modifier.width(4.dp))
                Text("Create New")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (keys.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No answer keys found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(keys) { key ->
                    KeyItemCard(
                        key = key,
                        onDeleteClick = { showDeleteConfirm = key },
                        onPrintClick = { printKey(key) }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Answer Key") },
            text = { Text("Are you sure you want to delete '${showDeleteConfirm?.name}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm?.let {
                            repository.deleteKey(it.id)
                            keys = repository.getAllKeys()
                        }
                        showDeleteConfirm = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun KeyItemCard(key: AnswerKey, onDeleteClick: () -> Unit, onPrintClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF10B981).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF10B981)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = key.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(key.dateMillis))
                Text(
                    text = "${key.numberOfQuestions} Questions • $dateStr",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                )
            }
            IconButton(onClick = onPrintClick) {
                Icon(
                    imageVector = Icons.Default.Print,
                    contentDescription = "Print",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
