package com.venom.trans

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

fun translate(text: String, target: String, callback: TranslateCallback) {
    val client = OkHttpClient()
    val request: Request = Request.Builder()
        .url("https://translate.googleapis.com/translate_a/single?client=gtx&dt=t&sl=auto&tl=$target&q=$text")
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            // Handle failure
            callback.onTranslationComplete(null)
        }

        override fun onResponse(call: Call, response: Response) {
            try {
                if (response.isSuccessful) {
                    val jsonArray = JSONArray(response.body!!.string()).getJSONArray(0)
                    val translatedText = StringBuilder()
                    for (i in 0 until jsonArray.length()) {
                        val translation = jsonArray.getJSONArray(i)
                        translatedText.append(translation.getString(0)).append(" ")
                    }
                    callback.onTranslationComplete(translatedText.toString().trim())
                } else {
                    callback.onTranslationComplete(null)
                }
            } catch (e: JSONException) {
                e.printStackTrace()
                callback.onTranslationComplete(null)
            }
        }
    })
}

fun interface TranslateCallback {
    fun onTranslationComplete(translatedText: String?)
}
