package com.venom.trans

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

// Function to perform OCR request
fun ocrRequest(picturePath: String?, callback: OcrRequestCallback) {
    val client = OkHttpClient()

    val file = File(picturePath!!)

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
        .post(requestBody)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callback.onOcrComplete(null)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                val responseBody = response.body?.string() ?: ""
                val jsonObject = JSONObject(responseBody)
                val ocrText = jsonObject.getJSONObject("google").getString("text")
                callback.onOcrComplete(ocrText)
            } catch (e: JSONException) {
                callback.onOcrComplete(null)
            } catch (e: IOException) {
                callback.onOcrComplete(null)
            }
        }
    })
}

fun interface OcrRequestCallback {
    fun onOcrComplete(ocrText: String?)
}
