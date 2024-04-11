package com.venom.trans

import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrMlkit : AppCompatActivity() {

    fun recognizeText(context: Context, image: Uri?, callback: (String) -> Unit) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        if (image != null) {
            val inputImage = InputImage.fromFilePath(context, image)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
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

        fun processTextBlock(result: Text) {
            // [START mlkit_process_text_block]
            val resultText = result.text
            for (block in result.textBlocks) {
                val blockText = block.text
                val blockCornerPoints = block.cornerPoints
                val blockFrame = block.boundingBox
                for (line in block.lines) {
                    val lineText = line.text
                    val lineCornerPoints = line.cornerPoints
                    val lineFrame = line.boundingBox
                    for (element in line.elements) {
                        val elementText = element.text
                        val elementCornerPoints = element.cornerPoints
                        val elementFrame = element.boundingBox
                    }
                }
            }
            // [END mlkit_process_text_block]
        }

        fun getTextRecognizer(): TextRecognizer {
            // [START mlkit_local_doc_recognizer]
            return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            // [END mlkit_local_doc_recognizer]
        }
    }
}
