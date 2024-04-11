package com.venom.trans

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast

class Tools {


    companion object {
//        var spokenText: String = ""
//        fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//            if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
//                data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { spoken ->
//                    spokenText = spoken
//                }
//            }
//        } //   intent  response  handler
//        fun speechToText(activity: Activity) {
//            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
//                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
//                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Something...")
//            }
//            activity.startActivityForResult(intent, 123)
//        }   //  Speech  To  Text
//

        // Displays a toast message.
        fun showToast(context: Context, message: String?) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        // Copies text to the clipboard.
        fun copyToClipboard(context: Context, textToCopy: CharSequence) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("label", textToCopy))
        }

        // Pastes text from the clipboard.
        fun pasteFromClipboard(context: Context): String? {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            return if (clipboard.hasPrimaryClip()) {
                showToast(context, "Text pasted from clipboard")
                clipboard.primaryClip!!.getItemAt(0).text.toString()
            } else {
                showToast(context, "Clipboard is empty")
                null
            }
        }

        // Shares content.
        fun shareContent(activity: Activity, text: String?, imageUri: Uri?, mimeType: String) {
            val shareIntent = Intent(Intent.ACTION_SEND)
            shareIntent.type = mimeType
            if (text != null) {
                shareIntent.putExtra(Intent.EXTRA_TEXT, text)
            }
            if (imageUri != null) {
                shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(shareIntent, "Share"))
        }

        // Converts image URI to its corresponding file path.
        fun imageUriToPath(context: Context, imageUri: Uri): String? {
            return context.contentResolver.query(
                imageUri,
                arrayOf(MediaStore.Images.Media.DATA),
                null,
                null,
                null
            )
                ?.use { cursor ->
                    cursor.moveToFirst().takeIf { it }?.let {
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    }
                }
        }
    }
}

//    import com.venom.trans.Tools
//
//      // Example of calling the onActivityResult method
//   override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//      super.onActivityResult(requestCode, resultCode, data)
//      Tools.onActivityResult(this, requestCode, resultCode, data, textInputEditText)
//}
//      // Example of calling the speechToText method
//    Tools.speechToText(this)
//
//      // Example of calling the showToast method
//    Tools.showToast(this, "Your message")
//
//      // Example of calling the copyToClipboard method
//    Tools.copyToClipboard(applicationContext, outputTextView.getText())
//    Tools.copyToClipboard(this, "Text to copy")
//
//      // Example of calling the pasteFromClipboard method
//    Tools.pasteFromClipboard(this)
//
//      // Example of calling the shareContent method
//    val textToShare = "Text to share"
//    val imageUri: Uri? = // Your image URI
//        Tools.shareContent(this, textToShare, imageUri, "image/*")
//
//      // Example of calling the imageUriToPath method
//    val imageUri: Uri = // Your image URI
//    val imagePath = Tools.imageUriToPath(this, imageUri)


//private val speech: SpeechRecognizer by lazy { SpeechRecognizer.createSpeechRecognizer(this) }
//
//    if (SpeechRecognizer.isRecognitionAvailable(this)) {
//        Toast.makeText(this, "Voice recognition available.", Toast.LENGTH_SHORT).show()
//        val speechRecognizer = speech.setRecognitionListener(this)
//
//        val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
//            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH)
//            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, applicationContext.packageName)
//            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
//            }
//        }
//
//        speechRecognizer.startListening(recognizerIntent)

