package com.example.omrscannerapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.omrscannerapp.theme.OMRScannerAppTheme
import com.example.omrscannerapp.ui.AppNavigation
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val isOpenCVInitialized = OpenCVLoader.initDebug()
    if (isOpenCVInitialized) {
        Log.d("OpenCV", "OpenCV loaded successfully!")
    } else {
        Log.e("OpenCV", "OpenCV initialization failed!")
    }

    enableEdgeToEdge()
    setContent {
      OMRScannerAppTheme { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
            AppNavigation()
        } 
      }
    }
  }
}
