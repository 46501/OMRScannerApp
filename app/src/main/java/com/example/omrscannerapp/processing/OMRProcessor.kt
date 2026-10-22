package com.example.omrscannerapp.processing

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.io.InputStream
import kotlin.math.max

object OMRProcessor {

    data class DetectionResult(
        val answers: List<Int>, // index = question index, value = selected option index, -1 for unanswered, -2 for multiple
        val success: Boolean,
        val errorMessage: String? = null
    )

    fun processSheet(context: Context, imageUri: Uri, expectedQuestions: Int, optionsPerQuestion: Int): DetectionResult {
        try {
            val bitmap = uriToBitmap(context, imageUri) ?: return DetectionResult(emptyList(), false, "Failed to load image")
            
            val mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            
            // 1. Preprocessing
            val gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            val edged = Mat()
            Imgproc.Canny(blurred, edged, 75.0, 200.0)

            // 2. Find paper contour
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(edged, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            
            if (contours.isEmpty()) {
                return DetectionResult(emptyList(), false, "No contours found")
            }

            contours.sortByDescending { Imgproc.contourArea(it) }
            
            var paperContour: MatOfPoint2f? = null
            for (c in contours) {
                val c2f = MatOfPoint2f(*c.toArray())
                val peri = Imgproc.arcLength(c2f, true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(c2f, approx, 0.02 * peri, true)

                if (approx.total() == 4L) {
                    paperContour = approx
                    break
                }
            }

            var warped = gray // Fallback to original if paper boundary not found
            if (paperContour != null) {
                warped = warpPerspective(gray, paperContour)
            } else {
                // If we can't find a 4-point contour, try to process the whole image anyway
            }

            // 3. Thresholding for bubbles
            val thresh = Mat()
            Imgproc.threshold(warped, thresh, 0.0, 255.0, Imgproc.THRESH_BINARY_INV or Imgproc.THRESH_OTSU)

            // 4. Find bubbles
            val bubbleContours = ArrayList<MatOfPoint>()
            Imgproc.findContours(thresh, bubbleContours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            val questions = ArrayList<MatOfPoint>()
            for (c in bubbleContours) {
                val rect = Imgproc.boundingRect(c)
                val ar = rect.width / rect.height.toFloat()
                // A typical bubble is roughly square
                if (rect.width >= 20 && rect.height >= 20 && ar >= 0.8 && ar <= 1.2) {
                    questions.add(c)
                }
            }

            // If we don't have exactly expectedQuestions * optionsPerQuestion bubbles, it's a heuristic failure.
            // For a robust system, we would sort bubbles into rows and columns.
            // As a general fallback for this phase, we will do top-to-bottom sorting.
            
            // Sort from top to bottom
            val sortedByY = questions.sortedBy { Imgproc.boundingRect(it).y }
            
            val detectedAnswers = mutableListOf<Int>()
            
            // Group by row (assuming optionsPerQuestion bubbles per row)
            for (i in 0 until expectedQuestions) {
                val startIndex = i * optionsPerQuestion
                if (startIndex + optionsPerQuestion > sortedByY.size) {
                    // Not enough bubbles detected
                    detectedAnswers.add(-1)
                    continue
                }
                
                val rowBubbles = sortedByY.subList(startIndex, startIndex + optionsPerQuestion)
                // Sort by X (left to right)
                val sortedByX = rowBubbles.sortedBy { Imgproc.boundingRect(it).x }
                
                var bubbledCount = 0
                var selectedOption = -1
                
                for (j in 0 until optionsPerQuestion) {
                    val bubble = sortedByX[j]
                    val mask = Mat.zeros(thresh.size(), CvType.CV_8UC1)
                    Imgproc.drawContours(mask, listOf(bubble), -1, Scalar(255.0), Core.FILLED)
                    
                    val mean = Core.mean(thresh, mask).`val`[0]
                    // If the fill ratio is high enough, consider it marked
                    if (mean > 150) {
                        bubbledCount++
                        selectedOption = j
                    }
                }
                
                if (bubbledCount == 1) {
                    detectedAnswers.add(selectedOption)
                } else if (bubbledCount > 1) {
                    detectedAnswers.add(-2) // Multiple
                } else {
                    detectedAnswers.add(-1) // Unanswered
                }
            }

            return DetectionResult(detectedAnswers, true)

        } catch (e: Exception) {
            return DetectionResult(emptyList(), false, e.message)
        }
    }

    private fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun warpPerspective(image: Mat, contour: MatOfPoint2f): Mat {
        val points = contour.toArray()
        // Sort points: top-left, top-right, bottom-right, bottom-left
        val sum = points.map { it.x + it.y }
        val diff = points.map { it.y - it.x }
        
        val tl = points[sum.indexOf(sum.minOrNull()!!)]
        val br = points[sum.indexOf(sum.maxOrNull()!!)]
        val tr = points[diff.indexOf(diff.minOrNull()!!)]
        val bl = points[diff.indexOf(diff.maxOrNull()!!)]

        val widthA = Math.sqrt(Math.pow(br.x - bl.x, 2.0) + Math.pow(br.y - bl.y, 2.0))
        val widthB = Math.sqrt(Math.pow(tr.x - tl.x, 2.0) + Math.pow(tr.y - tl.y, 2.0))
        val maxWidth = max(widthA, widthB).toInt()

        val heightA = Math.sqrt(Math.pow(tr.x - br.x, 2.0) + Math.pow(tr.y - br.y, 2.0))
        val heightB = Math.sqrt(Math.pow(tl.x - bl.x, 2.0) + Math.pow(tl.y - bl.y, 2.0))
        val maxHeight = max(heightA, heightB).toInt()

        val src = MatOfPoint2f(tl, tr, br, bl)
        val dst = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(maxWidth - 1.0, 0.0),
            Point(maxWidth - 1.0, maxHeight - 1.0),
            Point(0.0, maxHeight - 1.0)
        )

        val transformMatrix = Imgproc.getPerspectiveTransform(src, dst)
        val warped = Mat()
        Imgproc.warpPerspective(image, warped, transformMatrix, Size(maxWidth.toDouble(), maxHeight.toDouble()))
        return warped
    }
}
