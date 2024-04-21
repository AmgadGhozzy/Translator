package com.venom.trans

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.view.MotionEvent
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrMlkit : AppCompatActivity() {
    fun recognizeText(
        context: Context,
        imageUri: Uri?,
        imageView: ImageView,
        callback: (String) -> Unit
    ) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        if (imageUri != null) {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    drawTextBlocksOnImage(imageView, visionText, imageUri)
                    val recognizedText = StringBuilder()
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            recognizedText.append(line.text).append("\n")
                        }
                    }
                    callback(recognizedText.toString())
                }
                .addOnFailureListener { e ->
                    e.printStackTrace()
                    callback("")
                }
        } else {
            callback("")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun drawTextBlocksOnImage(imageView: ImageView, visionText: Text?, imageUri: Uri) {
        val context = imageView.context
        val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageUri))
        visionText?.let {
            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val paint = Paint().apply {
                color = Color.parseColor("#3069FF")
                style = Paint.Style.STROKE
                strokeWidth = 5.0f
            }
            for (block in visionText.textBlocks) {
                block.boundingBox?.let {
                    canvas.drawRect(it, paint)
                }
            }

            val imageViewMatrix = imageView.imageMatrix
            val imageMatrixValues = FloatArray(9)
            imageViewMatrix.getValues(imageMatrixValues)
            val scaleX = imageMatrixValues[Matrix.MSCALE_X]
            val scaleY = imageMatrixValues[Matrix.MSCALE_Y]

            imageView.setImageBitmap(mutableBitmap)
            imageView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val x = ((event.x - imageMatrixValues[Matrix.MTRANS_X]) / scaleX).toInt()
                    val y = ((event.y - imageMatrixValues[Matrix.MTRANS_Y]) / scaleY).toInt()
                    recognizeTextFromSelectedArea(visionText, x, y)?.let { selectedText ->
                        Tools.copyToClipboard(context, selectedText)
                        Tools.showToast(context, selectedText)
                    }
                    imageView.performClick()
                }
                false
            }
        }
    }

    private fun recognizeTextFromSelectedArea(text: Text, x: Int, y: Int): String? {
        for (block in text.textBlocks) {
            val box = block.boundingBox
            if (box != null && x >= box.left && x <= box.right && y >= box.top && y <= box.bottom) {
                return block.text
            }
        }
        return null
    }
}
