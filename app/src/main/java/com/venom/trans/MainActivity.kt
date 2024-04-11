package com.venom.trans

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.venom.trans.DialogWindow.show
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    lateinit var textInputEditText: TextInputEditText
    lateinit var textInputLayout: TextInputLayout
    lateinit var outputTextView: TextView
    lateinit var scrollView: ScrollView
    lateinit var imageView: ImageView
    private var spokenText: String = ""

    var isDictionary: Boolean = true
    var isOcrOffline: Boolean = true
    var isDialog: Boolean = true


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreference = getDefaultSharedPreferences(applicationContext)
        val isLightTheme = sharedPreference.getBoolean("light_theme", true)
        setTheme(if (isLightTheme) R.style.AppTheme_Light else R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_trans)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar) // Set the toolbar as the action bar
        val translateButton = findViewById<Button>(R.id.translateButton)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnNavigationItemSelectedListener(navListener) // Set the Navigation as the action bar

        textInputEditText = findViewById(R.id.editInputText)
        textInputLayout = findViewById(R.id.inputText)
        outputTextView = findViewById(R.id.translatedText)
        outputTextView.setTextIsSelectable(true) // Set the text view as selectable
        imageView = findViewById(R.id.imageView)
        scrollView = findViewById(R.id.scrollMenu)
        val dictionarySwitch: MaterialSwitch = findViewById(R.id.switch_dictionary)
        val ocrSwitch: MaterialSwitch = findViewById(R.id.switch_ocr)
        val ocrButton = findViewById<Button>(R.id.ocrButton)
        textInputEditText.setText(intent.getStringExtra("LastText"))


        textInputLayout.setStartIconOnClickListener {
            textInputEditText.setText(
                Tools.pasteFromClipboard(this)
            )
        }

        textInputLayout.setStartIconOnLongClickListener {
            speechToText()
            textInputEditText.setText(spokenText)

            true
        }
//        Tools.speechToText(this)
//        textInputEditText.setText(Tools.spokenText)
        translateButton.setOnLongClickListener {
            speechToText()
            textInputEditText.setText(spokenText)
            true
        }
        translateButton.setOnClickListener {
            translate()
        }

        fun openImagePicker() {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImage.launch(intent)
        }

        ocrButton.setOnClickListener { openImagePicker() }
        //ocrButton.setOnClickListener { requestPermission() }


        val sharedEditor = sharedPreference.edit()
        isDialog = sharedPreference.getBoolean("trans_in_dialog", true)
        isDictionary = sharedPreference.getBoolean("trans_dictionary", true)
        dictionarySwitch.isChecked = isDictionary

        dictionarySwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked != isDictionary) {
                sharedEditor.putBoolean("trans_dictionary", isChecked).apply()
                recreate()
            }
        }

        isOcrOffline = sharedPreference.getBoolean("ocr_offline", true)
        ocrSwitch.isChecked = isOcrOffline

        ocrSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked != isOcrOffline) {
                sharedEditor.putBoolean("ocr_offline", isChecked).apply()
                recreate()
            }
        }



        scrollView.setOnTouchListener(object : OnTouchListener {
            var startX = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> startX = event.x
                    MotionEvent.ACTION_UP -> {
                        val endX = event.x
                        val deltaX = endX - startX
                        if (abs(deltaX.toDouble()) > 200) { // Adjust this threshold as needed
                            if (deltaX < 0) {
                                // Swiped left
                                Speech.shutdown()
                                Speech(this@MainActivity, outputTextView.getText().toString())
                            } else {
                                // Swiped right
                                Tools.copyToClipboard(applicationContext, outputTextView.getText())

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
                    Tools.showToast(this, "Home clicked")
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
                    Tools.showToast(this, "Offline clicked")
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

                    Tools.showToast(this, "Setting clicked")
                    true
                }

                else -> {
                    item.isChecked = true
                    false
                }
            }
        }  //  navigation bottom

    private fun requestPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //openImagePicker()
            // Permission already granted
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES),
                1
            )
        }
    } //  request  READ_MEDIA_IMAGES  permission

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // selectImage()
        } else {
            Tools.showToast(this, "Permission denied to access external storage")
        }
    } //  handel requested permissions

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.popup_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        when (id) {
            R.id.action_copy -> {
                // Handle copy action
                Tools.copyToClipboard(this, outputTextView.getText())
                Tools.showToast(this, "Copy Done")
                return true
            }

            R.id.action_speak -> {
                // Handle speak action
                Speech.shutdown()
                Speech(this@MainActivity, outputTextView.getText().toString())
                Tools.showToast(this, "Speak Done")
                return true
            }

            R.id.action_share -> {
                // Handle share action
                Tools.shareContent(this, outputTextView.getText().toString(), null, "text/*")
                Tools.showToast(this, "Share Done")
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }   // Tools bar


    private fun translate() {
        val targetLang =
            getResources().getStringArray(R.array.LangCodeArray)[findViewById<Spinner>(R.id.spinner_to_language).selectedItemPosition]
        val text = textInputEditText.text.toString()
        val translationCallback: (String?) -> Unit = { result ->
            val outputText = result ?: "Failed to translate text"
            runOnUiThread {
                if (isDialog) {
                    show(outputText, this)
                } else {
                    outputTextView.text = outputText
                }
            }
        }
        if (isDictionary) {
            dictionary(text, targetLang, translationCallback)
        } else {
            translate(text, targetLang, translationCallback)
        }
    }   //  translate and dictionary


//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        Tools.onActivityResult(requestCode, resultCode, data)
//    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                ?.let { spoken ->
                    spokenText = spoken
                    textInputEditText.setText(spokenText)
                }
        }
    } //   intent  response  handler

    private fun speechToText() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak Something...")
        }
        startActivityForResult(intent, 123)
    }   //  Speech  To  Text




    private val pickImage =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageUri: Uri? = result.data?.data
                val picturePath = imageUri?.let { Tools.imageUriToPath(this, it) }
                val ocrFunction: (String?) -> Unit = { ocrText ->
                    val textToShow = ocrText ?: "Failed to recognize text"
                    runOnUiThread {
                        if (isDialog) show(textToShow, this) else outputTextView.text = textToShow
                    }
                }
                if (isOcrOffline) OcrMlkit().recognizeText(this, imageUri, ocrFunction)
                else ocrRequest(picturePath, ocrFunction)
            }
        }   //  pick    Image   and   OCR
}
