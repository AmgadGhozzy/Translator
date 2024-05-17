package com.venom.trans

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.venom.trans.DialogWindow.show
import com.venom.trans.Tools.Companion.copyToClipboard
import com.venom.trans.Tools.Companion.getSelectedOrAllText
import com.venom.trans.Tools.Companion.setProgressText
import com.venom.trans.Tools.Companion.shareContent
import com.venom.trans.Tools.Companion.showToast
import com.venom.trans.databinding.ActivityMainTransBinding
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreference = getDefaultSharedPreferences(applicationContext)
        val isLightTheme = sharedPreference.getBoolean("light_theme", true)
        setTheme(if (isLightTheme) R.style.AppTheme_Light else R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState)
        val binding: ActivityMainTransBinding = ActivityMainTransBinding.inflate(layoutInflater)
        setContentView(binding.root)

        handleShareText(intent)
        handleShareImage(intent)
        handleSelectText(intent)

        setSupportActionBar(binding.toolsMenu) // Set the toolbar as the action bar
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnNavigationItemSelectedListener(navListener) // Set the Navigation as the action bar

        //bottomNav.selectedItemId = R.id.navigation_home
        textInputEditText = binding.editInputText
        textInputLayout = binding.inputText
        outPutTextView = binding.translatedText
        outPutTextView.setTextIsSelectable(true) // Set the text view as selectable
        imageView = binding.imageView
        fabCancel = binding.fabCancel
        scrollView = binding.scrollMenu
        val dictionarySwitch = binding.dictionarySwitch
        val ocrSwitch = binding.switchOcr
        val ocrButton = binding.ocrButton
        textInputEditText.setText(intent.getStringExtra("LastText"))

        val transProvidersSpinner = binding.transProvidersAutoComplete
        val listOfTransProvider = listOf("google", "amazon")
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listOfTransProvider)
        transProvidersSpinner.setAdapter(adapter)

        textInputLayout.setStartIconOnClickListener {
            textInputEditText.setText(
                outPutTextView.getSelectedOrAllText()
                //this.pasteFromClipboard()
            )
        }
        textInputLayout.setStartIconOnLongClickListener {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    1
                )
            } else {
                Tools.speakToText(this) {
                    textInputEditText.setText(it)
                }
            }
            true
        }
        binding.translateButton.setOnLongClickListener {
            speechToText()
            textInputEditText.setText(spokenText)
            true
        }
        binding.translateButton.setOnClickListener {
            translate()
        }

        fabCancel.setOnClickListener {
            // Replace the image with the default drawable
            imageView.setImageResource(R.drawable.translate_ic)
            fabCancel.hide()
        }

        fun requestPermission() {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startImagePicker()
                //startChooseImageIntent()
                //startCameraIntent()

                // Permission already granted
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                    1
                )
            }
        } //  request  READ_MEDIA_IMAGES  permission

        fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
        ) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startImagePicker()
                //startChooseImageIntent()
                //startCameraIntent()
            } else {
                this.showToast("Permission denied to access external storage")
            }
        } //  handel requested permissions

        val sharedEditor = sharedPreference.edit()
        isDialog = sharedPreference.getBoolean("trans_in_dialog", false)
        isDictionary = sharedPreference.getBoolean("trans_dictionary", false)
        isOcrOffline = sharedPreference.getBoolean("ocr_offline", true)
        dictionarySwitch.isChecked = isDictionary
        dictionarySwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked != isDictionary) {
                sharedEditor.putBoolean("trans_dictionary", isChecked).apply()
                recreate()
            }
        }
        ocrSwitch.isChecked = isOcrOffline

        ocrButton.setOnClickListener { requestPermission() }
        ocrSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked != isOcrOffline) {
                sharedEditor.putBoolean("ocr_offline", isChecked).apply()
                recreate()
            }
        }

        scrollView.setOnTouchListener(object : OnTouchListener {
            var startX = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val context = v.context
                when (event.action) {
                    ACTION_DOWN -> startX = event.x
                    ACTION_UP -> {
                        val endX = event.x
                        val deltaX = endX - startX
                        if (abs(deltaX.toDouble()) > 200) { // Adjust this threshold as needed
                            if (deltaX < 0) {
                                // Swiped left
                                Tools.SpeechManager.initialize(context) { success ->
                                    if (success) Tools.SpeechManager.textToSpeak(outPutTextView.text.toString())
                                }
                            } else {
                                // Swiped right
                                context.copyToClipboard(outPutTextView.toString())
                            }
                        }
                    }
                }
                return true
            }
        }) // swipe
    }

    private val navListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Handle home action
                    this.showToast("Home clicked")
                    true
                }

                R.id.navigation_offline -> {
                    // Handle Offline action
                    val options =
                        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
                    val intent = Intent(this, TranslateOffline::class.java).apply {
                        putExtra(
                            "LastText",
                            textInputEditText.text.toString()
                        )
                    }
                    startActivity(intent, options.toBundle())
                    this.showToast("Offline clicked")
                    true
                }

                R.id.navigation_setting -> {
                    // Handle setting action
                    val options =
                        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
                    val intent = Intent(this, SettingsActivity::class.java).apply {
                        putExtra(
                            "LastText",
                            textInputEditText.text.toString()
                        )
                    }
                    startActivity(intent, options.toBundle())

                    this.showToast("Setting clicked")
                    true
                }

                else -> {
//                    item.isChecked = true
                    false
                }
            }
        }  //  navigation bottom
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.tools_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_copy -> {
                // Handle copy action
                this.copyToClipboard(outPutTextView.text.toString())
                this.showToast("Copy Done")
                return true
            }

            R.id.action_speak -> {
                // Initialize TextToSpeech engine.
                Tools.SpeechManager.initialize(this) { success ->
                    if (success) Tools.SpeechManager.textToSpeak(outPutTextView.text.toString())
                }
                this.showToast("Speak Done")
                return true
            }

            R.id.action_share -> {
                // Handle share action
                this.shareContent(text = outPutTextView.getText().toString())
                this.showToast("Share Done")
                return true
            }

            R.id.action_expand -> {
                // Handle share action
                show(outPutTextView.getText().toString(), dialogTitle, this)
                this.showToast("Expand Done")
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }   // Tools bar
    private fun translate() {
        val targetLang =
            getResources().getStringArray(R.array.LangCodeArray)[findViewById<Spinner>(R.id.languageSpinner).selectedItemPosition]
        outPutTextView.setProgressText(progressText)
        val text = textInputEditText.text.toString()
        dialogTitle = "Translated Text"
        val translationCallback: (String?) -> Unit = { result ->
            val outputText = result ?: "Failed to translate text"
            runOnUiThread {
                if (isDialog) {
                    show(outputText, dialogTitle, this)
                } else {
                    outPutTextView.text = outputText
                }
            }
        }
        if (isDictionary) {
            dictionary(text, targetLang, translationCallback)
        } else {
            translate(text, targetLang, translationCallback)
        }
    }   //  translate and dictionary
    private fun speechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Something...")
        }
        pickSpeech.launch(intent)
    }   //  Speech  To  Text

    private val pickSpeech =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                ?.let { spoken ->
                    spokenText = spoken
                    textInputEditText.setText(spokenText)
                }
        }

    private fun imageHandler() {
        //imageView.setImageURI(imageUri)
        fabCancel.show()
        dialogTitle = "Recognized Text"
        val ocrFunction: (String?) -> Unit = { ocrText ->
            val textToShow = ocrText ?: "Failed to recognize text"
            runOnUiThread {
                if (isDialog) show(textToShow, dialogTitle, this) else outPutTextView.text =
                    textToShow
            }
        }
        if (isOcrOffline) OcrMlkit(this, contentResolver, imageUri, imageView).recognizeText(
            ocrFunction
        )
        else imagePath?.let {
            ocrRequest(
                this,
                contentResolver,
                imageView,
                it,
                imageUri,
                ocrFunction
            )
        }
    }

    private val pickImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                imageUri = result.data?.data
                imageUri?.let { uri ->
                    imagePath = Tools.imageUriToPath(this, uri)
                    imageHandler()
                } ?: run { this.showToast("Failed to get image") }
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                this.showToast("Image capture operation canceled")
            }
        }

    private fun startCameraIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            val values = ContentValues().apply {
                put(
                    MediaStore.Images.Media.TITLE,
                    "New Picture"
                ); put(
                MediaStore.Images.Media.DESCRIPTION,
                "From Camera"
            )
            }
            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            pickImage.launch(takePictureIntent)
        } else this.showToast("Failed to open camera")
    }

    private fun startImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImage.launch(intent)
    }

    private fun startChooseImageIntent() {
        val chooseImageIntent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        pickImage.launch(chooseImageIntent)
    }


    private fun handleShareImage(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { imageUri ->
                imageView.setImageURI(imageUri)
                val imagePath = Tools.imageUriToPath(this, imageUri)
                this.showToast(imagePath)
            }
        }
    }

    private fun handleShareText(intent: Intent?) {
        // Check if the activity is started by a share action
        if (Intent.ACTION_SEND == intent?.action && intent.type == "text/plain") {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT).toString()
            textInputEditText.setText(sharedText)
            this.showToast(sharedText)
        }
    }

    private fun handleSelectText(intent: Intent?) {
        // Check if the intent is from the text selection
        if (Intent.ACTION_PROCESS_TEXT == intent?.action) {
            // Get the selected text
            selectedText = intent.getStringExtra(Intent.EXTRA_PROCESS_TEXT).toString()
            textInputEditText.setText(selectedText)
            this.showToast(selectedText)
        }
    }

    @SuppressLint("StaticFieldLeak")
    companion object {
        private const val TAG = "MainActivity"
        private lateinit var textInputEditText: TextInputEditText
        private lateinit var textInputLayout: TextInputLayout
        private lateinit var outPutTextView: TextView
        private lateinit var scrollView: ScrollView
        private lateinit var imageView: ImageView
        private lateinit var fabCancel: FloatingActionButton
        private lateinit var spokenText: String
        private lateinit var sharedText: String
        private lateinit var selectedText: String


        private var dialogTitle: String = ""
        private var progressText = "Translating..."

        private var imageUri: Uri? = null
        private var imagePath: String? = null

        private var isDictionary: Boolean = true
        private var isOcrOffline: Boolean = true
        private var isDialog: Boolean = true

    }

    override fun onDestroy() {
        super.onDestroy()
        // Shutdown SpeechManager
        Tools.SpeechManager.shutdown()
    }

}





