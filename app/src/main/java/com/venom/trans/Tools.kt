package com.venom.trans

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.util.Locale

/**
 * Provides utility methods commonly used in Android development.
 */
class Tools {
    companion object {
        // Displays a toast message.
        fun Context.showToast(message: String?) {
            message?.let { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
        }

        // Sets progress text for a TextView.
        fun TextView.setProgressText(progressText: String) {
            text = progressText
        }

        // Gets selected or all text from a TextView.
        fun TextView.getSelectedOrAllText(): String {
            return if (isFocused && selectionStart != selectionEnd) {
                text.subSequence(selectionStart, selectionEnd).toString()
            } else {
                text.toString()
            }
        }

        // Copies text to the clipboard.
        fun Context.copyToClipboard(textToCopy: CharSequence) {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Translator", textToCopy))
        }

        // Pastes text from the clipboard.
        fun Context.pasteFromClipboard(): String? {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            return clipboard.primaryClip?.getItemAt(0)?.text?.toString().also {
                if (it.isNullOrEmpty()) {
                    showToast(getString(R.string.clipboard_empty_message))
                }
            }
        }

        // Shares content.
        fun Activity.shareContent(
            text: String? = null,
            imageUri: Uri? = null,
            mimeType: String = "text/plain"
        ) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                text?.let { putExtra(Intent.EXTRA_TEXT, it) }
                imageUri?.let {
                    putExtra(Intent.EXTRA_STREAM, it)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }

        // Converts image URI to its corresponding file path.
        fun imageUriToPath(context: Context, imageUri: Uri?): String? {
            return try {
                imageUri?.let { uri ->
                    context.contentResolver.query(
                        uri,
                        arrayOf(MediaStore.Images.Media.DATA),
                        null,
                        null,
                        null
                    )?.use { cursor ->
                        cursor.moveToFirst()
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        // Opens the app settings page.
        fun Context.openAppSettings() {
            val intent = Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", packageName, null)
            }
            startActivity(intent)
        }

        // Converts speech to text.
        fun speakToText(context: Context, onTextRecognized: (String) -> Unit) {
            val speechRecognizer: SpeechRecognizer =
                SpeechRecognizer.createSpeechRecognizer(context)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_SECURE, true)
                putExtra(
                    RecognizerIntent.EXTRA_MAX_RESULTS,
                    1
                ) // Set max results to 1 for simplicity
            }

            // Log: Ready for speech recognition
            Log.d("SpeechManager", "Ready for speech recognition")

            speechRecognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle) {
                    AlertDialog.Builder(context)
                        .setView(LayoutInflater.from(context).inflate(R.layout.speakdialog, null))
                        .create()
                        .apply {
                            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                            show()
                        }
                }

                override fun onBeginningOfSpeech() {
                    // Log: Speech recognition started
                    Log.d("SpeechManager", "Speech recognition started")
                }

                override fun onBufferReceived(buffer: ByteArray) {}

                override fun onEndOfSpeech() {
                    // Log: Speech recognition ended
                    Log.d("SpeechManager", "Speech recognition ended")
                }

                override fun onEvent(eventType: Int, params: Bundle) {}

                override fun onPartialResults(partialResults: Bundle) {}

                override fun onRmsChanged(rmsdB: Float) {}

                override fun onError(error: Int) {
                    // Log: Speech recognition error
                    Log.e("SpeechManager", "Speech recognition error: $error")
                    // Handle recognition errors, if any
                }

                override fun onResults(results: Bundle) {
                    val recognizedText =
                        results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                    recognizedText?.let {
                        // Log: Speech recognized
                        Log.d("SpeechManager", "Speech recognized: $it")
                        onTextRecognized(it)
                    }
                }
            })

            // Log: Starting speech recognition
            Log.d("SpeechManager", "Starting speech recognition")
            speechRecognizer.startListening(intent)
        }

    }

    object SpeechManager {

        private var textToSpeech: TextToSpeech? = null
        private var initializeCallback: ((Boolean) -> Unit)? = null

        fun initialize(context: Context, callback: (Boolean) -> Unit) {
            // Store the callback to be invoked later
            initializeCallback = callback

            // Initialize the TextToSpeech engine
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    initializeCallback?.invoke(true)
                } else {
                    initializeCallback?.invoke(false)
                    Log.e("SpeechManager", "TextToSpeech initialization failed")
                }
            }
        }

        fun textToSpeak(text: String) {
            textToSpeech?.setLanguage(Locale.getDefault())
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }

        fun shutdown() {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
        }
    }
}

//    // Display a toast message.
//    context.showToast("Hello, world!")
//
//// Set progress text for a TextView.
//    textView.setProgressText("Loading...")
//
//    // Get selected or all text from a TextView.
//    val selectedOrAllText = textView.getSelectedOrAllText()
//
//// Copy text to the clipboard.
//    context.copyToClipboard("Text to copy")
//
//    // Paste text from the clipboard.
//    val clipboardText = context.pasteFromClipboard()
//
//// Share content.
//    activity.shareContent(text = "Shared text")
//
//// Open the app settings page.
//    context.openAppSettings()
//
//// Convert speech to text.
//    Tools.speakToText(context) { recognizedText ->
//        // Handle recognized text here
//    }
//
//// Tools.SpeechManager.initialize(this) { success ->
//         if (success) Tools.SpeechManager.textToSpeak("Hello world")
//      }
//
//// Shutdown TextToSpeech engine.
//    Tools.SpeechManager.shutdown()
//
