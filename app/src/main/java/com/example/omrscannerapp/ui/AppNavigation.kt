package com.example.omrscannerapp.ui

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.omrscannerapp.ui.camera.CameraScreen
import com.example.omrscannerapp.ui.preview.PreviewScreen
import com.example.omrscannerapp.ui.keys.KeysScreen
import com.example.omrscannerapp.ui.keys.CreateKeyScreen
import com.example.omrscannerapp.ui.review.ReviewScreen
import com.example.omrscannerapp.data.AnswerKey
import com.example.omrscannerapp.ui.result.ScanProcessingScreen
import com.example.omrscannerapp.ui.result.ResultScreen
import com.example.omrscannerapp.ui.theory.TheoryScreen
import com.example.omrscannerapp.data.ScanResult
import com.example.omrscannerapp.data.QuestionData
import com.example.omrscannerapp.data.ScanResultRepository
import java.util.UUID
import androidx.compose.ui.platform.LocalContext

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val scanResultRepository = remember { ScanResultRepository(context) }
    var currentScanResult by remember { mutableStateOf<ScanResult?>(null) }
    var currentKey by remember { mutableStateOf<AnswerKey?>(null) }
    var currentDetectedAnswers by remember { mutableStateOf<List<Int>?>(null) }

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            MainScreen(
                onScanClick = {
                    navController.navigate("camera")
                },
                onGalleryImageSelected = { uri ->
                    navController.navigate("preview/${Uri.encode(uri.toString())}")
                },
                onCreateKeyClick = {
                    navController.navigate("create_key")
                },
                onTheoryClick = {
                    navController.navigate("theory")
                }
            )
        }
        composable("theory") {
            TheoryScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("keys") {
            KeysScreen(
                onCreateKeyClick = { navController.navigate("create_key") }
            )
        }
        composable("create_key") {
            CreateKeyScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("camera") {
            CameraScreen(
                onBack = { navController.popBackStack() },
                onImageCaptured = { uri ->
                    navController.navigate("preview/${Uri.encode(uri.toString())}") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = "preview/{imageUri}",
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("imageUri")
            val uri = uriString?.let { Uri.parse(it) }
            PreviewScreen(
                imageUri = uri,
                onBack = { navController.popBackStack() },
                onConfirm = { answerKey ->
                    navController.navigate("processing/${Uri.encode(uri.toString())}/${answerKey.id}") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                }
            )
        }
        composable(
            route = "processing/{imageUri}/{keyId}",
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType },
                navArgument("keyId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("imageUri")
            val keyId = backStackEntry.arguments?.getString("keyId") ?: ""
            val uri = uriString?.let { Uri.parse(it) } ?: Uri.EMPTY

            ScanProcessingScreen(
                imageUri = uri,
                answerKeyId = keyId,
                onSuccess = { detectedAnswers, answerKey ->
                    currentDetectedAnswers = detectedAnswers
                    currentKey = answerKey
                    navController.navigate("review") {
                        popUpTo("dashboard") { inclusive = false }
                    }
                },
                onError = { errorMsg ->
                    // For now, just go back to dashboard
                    navController.popBackStack("dashboard", inclusive = false)
                }
            )
        }
        composable("review") {
            if (currentDetectedAnswers != null && currentKey != null) {
                ReviewScreen(
                    initialAnswers = currentDetectedAnswers!!,
                    answerKey = currentKey!!,
                    onConfirm = { scanResult ->
                        scanResultRepository.saveResult(scanResult)
                        currentScanResult = scanResult
                        navController.navigate("result") {
                            popUpTo("dashboard") { inclusive = false }
                        }
                    },
                    onBack = { navController.popBackStack("dashboard", inclusive = false) }
                )
            }
        }
        composable("result") {
            currentScanResult?.let { result ->
                ResultScreen(
                    result = result,
                    onBackToHome = {
                        navController.popBackStack("dashboard", inclusive = false)
                    }
                )
            }
        }
    }
}
