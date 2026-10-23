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
        var mat: Mat? = null
        var gray: Mat? = null
        var blurred: Mat? = null
        var edged: Mat? = null
        var hierarchy: Mat? = null
        var thresh: Mat? = null
        var warped: Mat? = null
        try {
            val bitmap = uriToBitmap(context, imageUri) ?: return DetectionResult(emptyList(), false, "Failed to load image")
            
            mat = Mat()
            Utils.bitmapToMat(bitmap, mat)
            
            // 1. Preprocessing
            gray = Mat()
            Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
            blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(5.0, 5.0), 0.0)
            edged = Mat()
            Imgproc.Canny(blurred, edged, 75.0, 200.0)

            // 2. Find paper contour
            val contours = ArrayList<MatOfPoint>()
            hierarchy = Mat()
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

            warped = gray // Fallback to original if paper boundary not found
            if (paperContour != null) {
                warped = warpPerspective(gray, paperContour)
            }

            // 3. Thresholding for bubbles
            thresh = Mat()
            // Adaptive threshold is often more robust for shadows than global Otsu
            Imgproc.adaptiveThreshold(warped, thresh, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 21, 10.0)

            // 4. Find bubbles
            val bubbleContours = ArrayList<MatOfPoint>()
            Imgproc.findContours(thresh, bubbleContours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

            val questions = ArrayList<MatOfPoint>()
            for (c in bubbleContours) {
                val rect = Imgproc.boundingRect(c)
                val ar = rect.width / rect.height.toFloat()
                // Relaxed aspect ratio to account for camera angle/squash
                if (rect.width >= 15 && rect.height >= 15 && ar >= 0.6 && ar <= 1.4) {
                    questions.add(c)
                }
            }

            // Fallback: If no 4-point perspective warp happened, and the page is slightly rotated,
            // sorting purely by Y can fail. We proceed assuming mostly horizontal/vertical alignment.
            val sortedByY = questions.sortedBy { Imgproc.boundingRect(it).y }
            
            val detectedAnswers = mutableListOf<Int>()
            
            for (i in 0 until expectedQuestions) {
                val startIndex = i * optionsPerQuestion
                if (startIndex + optionsPerQuestion > sortedByY.size) {
                    // Not enough bubbles detected to confidently map this row
                    detectedAnswers.add(-1)
                    continue
                }
                
                val rowBubbles = sortedByY.subList(startIndex, startIndex + optionsPerQuestion)
                val sortedByX = rowBubbles.sortedBy { Imgproc.boundingRect(it).x }
                
                val fillValues = DoubleArray(optionsPerQuestion)
                for (j in 0 until optionsPerQuestion) {
                    val bubble = sortedByX[j]
                    val mask = Mat.zeros(thresh.size(), CvType.CV_8UC1)
                    // Draw filled contour of the bubble on the mask
                    Imgproc.drawContours(mask, listOf(bubble), -1, Scalar(255.0), Core.FILLED)
                    
                    // Measure how many white pixels are inside the inverted threshold bubble
                    fillValues[j] = Core.mean(thresh, mask).`val`[0]
                }
                
                // Relative comparison: Find the darkest/most filled bubbles
                val maxFill = fillValues.maxOrNull() ?: 0.0
                
                // Define a dynamic minimum fill threshold (e.g., must be decently dark to be considered)
                val minFillThreshold = 100.0 // Configurable threshold (0-255 scale)
                
                if (maxFill > minFillThreshold) {
                    var markedCount = 0
                    var selectedIndex = -1
                    // Any bubble that is within 80% of the maximum fill is also considered marked
                    // This catches multiple-marked scenarios where both bubbles are dark
                    val relativeThreshold = max(maxFill * 0.8, minFillThreshold)
                    
                    for (j in 0 until optionsPerQuestion) {
                        if (fillValues[j] >= relativeThreshold) {
                            markedCount++
                            selectedIndex = j
                        }
                    }
                    
                    if (markedCount == 1) {
                        detectedAnswers.add(selectedIndex)
                    } else {
                        detectedAnswers.add(-2) // Multiple marked
                    }
                } else {
                    detectedAnswers.add(-1) // Unanswered
                }
            }

            return DetectionResult(detectedAnswers, true)

        } catch (e: Exception) {
            return DetectionResult(emptyList(), false, e.message)
        } finally {
            mat?.release()
            gray?.release()
            blurred?.release()
            edged?.release()
            hierarchy?.release()
            thresh?.release()
            warped?.takeIf { it != gray }?.release()
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
