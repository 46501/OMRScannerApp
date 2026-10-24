package com.example.omrscannerapp.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.omrscannerapp.data.AnswerKey
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun generateOmrSheet(context: Context, answerKey: AnswerKey): File? {
        val pdfDocument = PdfDocument()
        
        // A4 Page size (72 PPI) -> 595 x 842 points
        // Let's use 1190 x 1684 for higher resolution rendering
        val pageWidth = 1190
        val pageHeight = 1684
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        
        val canvas = page.canvas
        drawOmrContent(canvas, pageWidth, pageHeight, answerKey)
        
        pdfDocument.finishPage(page)
        
        val file = File(context.cacheDir, "OMR_Sheet_${answerKey.name.replace(" ", "_")}.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawOmrContent(canvas: Canvas, width: Int, height: Int, answerKey: AnswerKey) {
        // Paint configurations
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 60f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 30f
        }
        val bubbleOutlinePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }
        val borderPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }

        // Title
        canvas.drawText(answerKey.name, width / 2f, 120f, titlePaint)
        
        // Student Info
        val infoY = 220f
        canvas.drawText("Name: ________________________", 100f, infoY, textPaint)
        canvas.drawText("Roll No: _________________", 600f, infoY, textPaint)
        canvas.drawText("Date: _________________", 100f, infoY + 60f, textPaint)

        // Bounding Box (Required for 4-point contour detection)
        val boxMargin = 100f
        val boxTop = 350f
        val boxBottom = height - 100f
        canvas.drawRect(boxMargin, boxTop, width - boxMargin, boxBottom, borderPaint)

        // Bubbles Grid Config
        val questions = answerKey.numberOfQuestions
        val options = answerKey.optionsPerQuestion

        // Determine Columns (Max 25 questions per column)
        val maxPerColumn = 25
        val columns = Math.ceil(questions / maxPerColumn.toDouble()).toInt()
        val columnWidth = (width - 2 * boxMargin) / columns

        val bubbleRadius = 15f
        val bubbleGap = 45f // Horizontal gap between options
        val rowGap = 45f    // Vertical gap between questions
        val startY = boxTop + 60f

        val optionsLabels = if (options == 4) listOf("A", "B", "C", "D") else listOf("A", "B", "C", "D", "E")

        // Draw headers for options inside the box (first row of each column)
        for (c in 0 until columns) {
            val colStartX = boxMargin + c * columnWidth + 100f
            for (opt in 0 until options) {
                canvas.drawText(optionsLabels[opt], colStartX + opt * bubbleGap - 10f, startY - 20f, textPaint)
            }
        }

        // Draw questions and bubbles
        for (i in 0 until questions) {
            val col = i / maxPerColumn
            val rowInCol = i % maxPerColumn

            val qX = boxMargin + col * columnWidth + 40f
            val qY = startY + rowInCol * rowGap
            
            // Question number
            canvas.drawText("${i + 1}.", qX, qY + 10f, textPaint)

            // Bubbles
            val bX = qX + 60f
            for (opt in 0 until options) {
                val cx = bX + opt * bubbleGap
                val cy = qY
                canvas.drawCircle(cx, cy, bubbleRadius, bubbleOutlinePaint)
            }
        }
    }
}
