package com.venom.trans

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.view.MotionEvent
import android.widget.ImageView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.venom.trans.Tools.Companion.copyToClipboard
import com.venom.trans.Tools.Companion.showToast

class OcrMlkit(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val imageUri: Uri?,
    private val imageView: ImageView
) {

    fun recognizeText(callback: (String?) -> Unit) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        imageUri?.let { uri ->
            val inputImage = InputImage.fromFilePath(context, uri)
            try {
                recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    drawTextBlocksOnImage(visionText)
                    val recognizedText = visionText.textBlocks.flatMap { block ->
                        block.lines.map { line -> line.text }
                    }.joinToString("\n")
                    callback(recognizedText)
                }
            } catch (e: Exception) {
                callback(null)
            }
        }
    }


    private fun drawTextBlocksOnImage(visionText: Text?) {
        val bitmapUtils = BitmapUtils(contentResolver)
        val uriBitmap = imageUri?.let { bitmapUtils.getBitmapFromUri(it) }
        val mutableBitmap = uriBitmap?.copy(Bitmap.Config.ARGB_8888, true)

        mutableBitmap?.let { bitmap ->
            visionText?.let {
                val canvas = Canvas(bitmap)
                val paint = Paint().apply {
                    color = Color.parseColor(BOX_COLOR)
                    style = Paint.Style.STROKE
                    strokeWidth = TEXT_BOX_STROKE_WIDTH
                }
                for (block in visionText.textBlocks) {
                    block.boundingBox?.let {
                        canvas.drawRect(it, paint)
                    }
                }
                imageView.setImageBitmap(bitmap)
                setupTouchHandling(visionText)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchHandling(visionText: Text) {

        val imageViewMatrix = imageView.imageMatrix
        val imageMatrixValues = FloatArray(9)
        imageViewMatrix.getValues(imageMatrixValues)
        val scaleX = imageMatrixValues[Matrix.MSCALE_X]
        val scaleY = imageMatrixValues[Matrix.MSCALE_Y]

        imageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val x = ((event.x - imageMatrixValues[Matrix.MTRANS_X]) / scaleX).toInt()
                val y = ((event.y - imageMatrixValues[Matrix.MTRANS_Y]) / scaleY).toInt()
                recognizeTextFromSelectedArea(visionText, x, y)?.let { selectedText ->
                    context.copyToClipboard(selectedText)
                    context.showToast(selectedText)
                }
                imageView.performClick()
            }
            false
        }
    }

    private fun recognizeTextFromSelectedArea(text: Text, x: Int, y: Int): String? {
        for (block in text.textBlocks) {
            val box = block.boundingBox
            if (box != null && x >= box.left && x <= box.right && y >= box.top && y <= box.bottom) {
                return block.text
            }
            if (box != null && x < box.left) {
                return null
            }
        }
        return null
    }


    companion object {
        private const val TEXT_BOX_STROKE_WIDTH = 5.0f
        private const val BOX_COLOR = "#3069FF"
    }
}
