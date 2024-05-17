package com.venom.trans


import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.IOException

private const val TAG = "OcrRequest"

fun ocrRequest(
    context: Context,
    contentResolver: ContentResolver,
    imageView: ImageView,
    picturePath: String,
    imageUri: Uri?,
    callback: OcrRequestCallback
) {
    val client = OkHttpClient()
    val file = File(picturePath)
    val requestBody: RequestBody = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("providers", "google")
        .addFormDataPart("file", file.name, file.asRequestBody("image/png".toMediaTypeOrNull()))
        .build()

    val request: Request = Request.Builder()
        .url("https://api.edenai.run/v2/ocr/ocr")
        .addHeader(
            "Authorization",
            "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoiNDRjYmNkODMtODkzYi00NWMyLWI2MTUtMjcwN2Q3YTM0OTliIiwidHlwZSI6ImFwaV90b2tlbiJ9.bM9NEGtMEIcVDepfpb-qPqJDYnl6j5qkrdSE7oCbh1M"
        )
        .post(requestBody).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "OCR request failed", e)
            callback.onOcrComplete(null)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val responseBody = response.body?.string() ?: ""
                val jsonObject = JSONObject(responseBody)
                Log.d(TAG, "responseBody = $responseBody")
                val googleObject = jsonObject.getJSONObject("google")
                val ocrText = googleObject.getString("text")
                val boundingBoxes = googleObject.getJSONArray("bounding_boxes")
                //val visionText = Text(boundingBoxes)

                for (i in 0 until boundingBoxes.length()) {
                    val boxObject = boundingBoxes.getJSONObject(i)
                    val text = boxObject.getString("text")
                    val left = boxObject.getDouble("left")
                    val top = boxObject.getDouble("top")
                    val width = boxObject.getDouble("width")
                    val height = boxObject.getDouble("height")

                    // Use the data to draw the box on your view
                    // (implementation depends on your drawing library)
                    drawBox(contentResolver, text, imageView, left, top, width, height, imageUri)
                }
                // Update UI on the main thread
                imageView.post {
                    //drawTextBlocksOnImage(context, contentResolver, visionText, imageView, imageUri)
                }


                callback.onOcrComplete(ocrText)
            } catch (e: JSONException) {
                Log.e(TAG, "Error parsing JSON response", e)
                callback.onOcrComplete(null)
            } catch (e: IOException) {
                Log.e(TAG, "IOException", e)
                callback.onOcrComplete(null)
            }
        }

    })
}

fun drawBox(
    contentResolver: ContentResolver,
    text: String,
    imageView: ImageView,
    left: Double,
    top: Double,
    width: Double,
    height: Double,
    imageUri: Uri?
) {
    val bitmapUtils = BitmapUtils(contentResolver)
    val uriBitmap = imageUri?.let { bitmapUtils.getBitmapFromUri(it) }
    val mutableBitmap = uriBitmap?.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = mutableBitmap?.let { Canvas(it) }
    val paint = Paint()
    paint.color = Color.RED
    paint.style = Paint.Style.STROKE
    canvas?.drawRect(
        (left * canvas.width).toFloat(),
        (top * canvas.height).toFloat(),
        ((left + width) * canvas.width).toFloat(),
        ((top + height) * canvas.height).toFloat(),
        paint
    )
    imageView.setImageBitmap(mutableBitmap)
}
//private fun drawTextBlocksOnImage(context: Context, contentResolver: ContentResolver, visionText: Text, imageView: ImageView, imageUri: Uri?) {
//    val bitmapUtils = BitmapUtils(contentResolver)
//    val uriBitmap = imageUri?.let { bitmapUtils.getBitmapFromUri(it) }
//    val mutableBitmap = uriBitmap?.copy(Bitmap.Config.ARGB_8888, true)
//
//    mutableBitmap?.let { bitmap ->
//        val canvas = Canvas(bitmap)
//        val paint = Paint().apply {
//            color = Color.parseColor("#3069FF")
//            style = Paint.Style.STROKE
//            strokeWidth = 5.0f
//        }
//        for (block in visionText.boundingBoxes) {
//            block.draw(canvas, paint)
//        }
//        imageView.setImageBitmap(bitmap)
//        setupTouchHandling(context, visionText, imageView)
//    }
//}
//
//@SuppressLint("ClickableViewAccessibility")
//private fun setupTouchHandling(context: Context, visionText: Text, imageView: ImageView) {
//    val imageViewMatrix = imageView.imageMatrix
//    val imageMatrixValues = FloatArray(9)
//    imageViewMatrix.getValues(imageMatrixValues)
//    val scaleX = imageMatrixValues[Matrix.MSCALE_X]
//    val scaleY = imageMatrixValues[Matrix.MSCALE_Y]
//
//    imageView.setOnTouchListener { _, event ->
//        if (event.action == MotionEvent.ACTION_DOWN) {
//            val x = ((event.x - imageMatrixValues[Matrix.MTRANS_X]) / scaleX).toInt()
//            val y = ((event.y - imageMatrixValues[Matrix.MTRANS_Y]) / scaleY).toInt()
//            recognizeTextFromSelectedArea(visionText, x, y)?.let { selectedText ->
//                Tools.copyToClipboard(context, selectedText)
//                Tools.showToast(context, selectedText)
//                Log.d(TAG, "responseBody = $selectedText")
//                Log.d(TAG, "Touch event coordinates: x=$x, y=$y")
//            }
//            imageView.performClick()
//        }
//        false
//    }
//}
//
//private fun recognizeTextFromSelectedArea(visionText: Text, x: Int, y: Int): String? {
//    for (block in visionText.boundingBoxes) {
//        Log.d(TAG, "Touch event coordinates: x=$x, y=$y")
//        if (block.contains(x, y)) {
//            return block.text
//        }
//    }
//    return null
//}
//
//class Text(boundingBoxes: JSONArray) {
//    val boundingBoxes: List<BoundingBox> = parseGoogleObject(boundingBoxes)
//
//    private fun parseGoogleObject(boundingBoxes: JSONArray): List<BoundingBox> {
//        val blocks = mutableListOf<BoundingBox>()
//        for (i in 0 until boundingBoxes.length()) {
//            val boundingBoxObject = boundingBoxes.getJSONObject(i)
//            val boxText = boundingBoxObject.getString("text")
//            val left = boundingBoxObject.getDouble("left")
//            val top = boundingBoxObject.getDouble("top")
//            val width = boundingBoxObject.getDouble("width")
//            val height = boundingBoxObject.getDouble("height")
//            blocks.add(BoundingBox(boxText, left, top, width, height))
//        }
//        return blocks
//    }
//}
//
//data class BoundingBox(val text: String, val left: Double, val top: Double, val width: Double, val height: Double) {
//    fun draw(canvas: Canvas, paint: Paint) {
//        val leftPos = left * canvas.width
//        val topPos = top * canvas.height
//        val rightPos = (left + width) * canvas.width
//        val bottomPos = (top + height) * canvas.height
//        canvas.drawRect(leftPos.toFloat(), topPos.toFloat(), rightPos.toFloat(), bottomPos.toFloat(), paint)
//    }
//
//    fun contains(x: Int, y: Int): Boolean {
//        val leftPos = left
//        val topPos = top
//        val rightPos = left + width
//        val bottomPos = top + height
//        return x >= leftPos && x <= rightPos && y >= topPos && y <= bottomPos
//    }
//}
//


fun interface OcrRequestCallback {
    fun onOcrComplete(ocrText: String?)
}





