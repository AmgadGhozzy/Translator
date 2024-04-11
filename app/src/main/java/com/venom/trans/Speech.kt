package com.venom.trans

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class Speech(context: Context?, textToSpeak: String?) {
    init {
        tts = TextToSpeech(context) {
            tts!!.setLanguage(Locale.getDefault())
            tts!!.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    companion object {
        private var tts: TextToSpeech? = null
        fun shutdown() {
            if (tts != null) {
                tts!!.stop()
                tts!!.shutdown()
                tts = null
            }
        }
    }
}
