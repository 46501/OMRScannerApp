package com.example.omrscannerapp.ui

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.omrscannerapp.ui.camera.CameraScreen
import com.example.omrscannerapp.ui.preview.PreviewScreen

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            MainScreen(
                onScanClick = {
                    navController.navigate("camera")
                },
                onGalleryImageSelected = { uri ->
                    navController.navigate("preview/${Uri.encode(uri.toString())}")
                }
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
                onConfirm = {
                    // Placeholder for actual OpenCV processing
                    navController.popBackStack("dashboard", inclusive = false)
                }
            )
        }
    }
}
