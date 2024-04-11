package com.venom.trans

import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException


fun dictionary(text: String, target: String, callback: DictionaryCallback) {
    val client = OkHttpClient()
    val data: RequestBody = FormBody.Builder()
        .add(
            "f.req",
            "[[[\"MkEWBc\",\"[[\\\"$text\\\",\\\"auto\\\",\\\"$target\\\",1],[]]\",\"null\",\"generic\"]]]"
        ).build()
    val request = Request.Builder()
        .url("https://translate.google.com/_/TranslateWebserverUi/data/batchexecute")
        .post(data)
        .build()
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            callback.onDictionaryComplete(null)
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful && response.body != null) {
                try {
                    val responseData = response.body!!.string().substring(6)
                    val jsonArray = JSONArray(responseData)
                    val parsedData = jsonArray.getJSONArray(0).getString(2)
                    val words = extractWords(JSONArray(parsedData))
                    val filteredWords = StringBuilder()
                    for (word in words) {
                        if (!listOf("ar", "en", "auto", target).contains(word)) {
                            filteredWords.append(word)
                        }
                    }
                    val wordMeanings =
                        filteredWords.toString().trim().replace("\\n{3,}".toRegex(), "\n\n\t#")
                    callback.onDictionaryComplete(wordMeanings)
                } catch (e: IOException) {
                    e.printStackTrace()
                    callback.onDictionaryComplete(null)
                } catch (e: JSONException) {
                    e.printStackTrace()
                    callback.onDictionaryComplete(null)
                }
            } else {
                callback.onDictionaryComplete(null)
            }
        }
    })
}

@Throws(JSONException::class)
private fun extractWords(data: JSONArray): List<String> {
    val words: MutableList<String> = ArrayList()
    for (i in 0 until data.length()) {
        val obj = data[i]
        if (obj is JSONArray) {
            words.addAll(extractWords(obj))
            words.add("\n")
        } else if (obj is String) {
            words.add(obj)
            words.add("\n")
        }
    }
    return words
}

fun interface DictionaryCallback {
    fun onDictionaryComplete(wordMeanings: String?)
}

