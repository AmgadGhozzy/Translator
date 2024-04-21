package com.venom.trans

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.text.Editable
import android.text.TextWatcher
import android.util.LruCache
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import java.util.Locale

/**
 * Fragment view for handling translations
 */
class TranslateOffline : AppCompatActivity() {
    var spokenText: String = ""
    lateinit var srcTextView: TextView
    private val navListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Handle home action
                    val options =
                        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent, options.toBundle())

                    Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.navigation_offline -> {
                    // Handle Offline action
                    item.isChecked = true
                    Toast.makeText(this, "Offline clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.navigation_setting -> {
                    // Handle setting action
                    val options =
                        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent, options.toBundle())

                    Toast.makeText(this, "Setting clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> {
//                    item.isChecked = true
                    false
                }
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        val isLightTheme = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .getBoolean("light_theme", true)
        setTheme(if (isLightTheme) R.style.AppTheme_Light else R.style.AppTheme_Dark)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.translate_offline)

        srcTextView = findViewById(R.id.sourceText)
        val targetSyncButton = findViewById<ToggleButton>(R.id.buttonSyncTarget)
        val switchButton = findViewById<Button>(R.id.buttonSwitchLang)
        val sourceSyncButton = findViewById<ToggleButton>(R.id.buttonSyncSource)
        var textInputLayout: TextInputLayout = findViewById(R.id.textInputLayout)
        val targetTextView = findViewById<TextView>(R.id.targetText)
        val downloadedModelsTextView = findViewById<TextView>(R.id.downloadedModels)
        val sourceLangSelector = findViewById<Spinner>(R.id.sourceLangSelector)
        val targetLangSelector = findViewById<Spinner>(R.id.targetLangSelector)
        val viewModel = ViewModelProviders.of(this)[TranslateViewModel::class.java]

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnNavigationItemSelectedListener(navListener) // Set the Navigation as the action bar
        bottomNav.selectedItemId = R.id.navigation_offline

        textInputLayout.setStartIconOnLongClickListener {
            speechToText()
            srcTextView.setText(spokenText)
            true
        }
        textInputLayout.setStartIconOnClickListener {
            srcTextView.text = Tools.pasteFromClipboard(this)
        }

        // Get available language list and set up source and target language spinners
        // with default selections.
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item, viewModel.availableLanguages
        )
        sourceLangSelector.adapter = adapter
        targetLangSelector.adapter = adapter
        sourceLangSelector.setSelection(adapter.getPosition(TranslateViewModel.Language("ar")))
        targetLangSelector.setSelection(adapter.getPosition(TranslateViewModel.Language("en")))
        sourceLangSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                setProgressText(targetTextView) // Update targetTextView to show progress
                viewModel.sourceLang.value =
                    adapter.getItem(position) // Update source language in view model
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                targetTextView.text = "" // Clear target text view
            }
        }
        targetLangSelector.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long,
            ) {
                setProgressText(targetTextView) // Update targetTextView to show progress
                viewModel.targetLang.value =
                    adapter.getItem(position) // Update target language in view model
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                targetTextView.text = "" // Clear target text view
            }
        }
        switchButton.setOnClickListener {
            val targetText = targetTextView.text.toString()
            setProgressText(targetTextView) // Update targetTextView to show progress
            val sourceLangPosition = sourceLangSelector.selectedItemPosition
            sourceLangSelector.setSelection(targetLangSelector.selectedItemPosition)
            targetLangSelector.setSelection(sourceLangPosition)

            // Also update srcTextView with targetText
            srcTextView.setText(targetText)
            viewModel.sourceText.value = targetText // Update source text in view model
        }

        // Set up toggle buttons to delete or download remote models locally.
        sourceSyncButton.setOnCheckedChangeListener { buttonView, isChecked ->
            val language = adapter.getItem(sourceLangSelector.selectedItemPosition)
            if (isChecked) {
                viewModel.downloadLanguage(language!!) // Download language model
            } else {
                viewModel.deleteLanguage(language!!) // Delete language model
            }
        }
        targetSyncButton.setOnCheckedChangeListener { buttonView, isChecked ->
            val language = adapter.getItem(targetLangSelector.selectedItemPosition)
            if (isChecked) {
                viewModel.downloadLanguage(language!!) // Download language model
            } else {
                viewModel.deleteLanguage(language!!) // Delete language model
            }
        }

        // Translate input text as it is typed
        srcTextView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                setProgressText(targetTextView) // Update targetTextView to show progress
                viewModel.sourceText.postValue(s.toString()) // Update source text in view model
            }
        })
        viewModel.translatedText.observe(
            this
        ) { resultOrError ->
            if (resultOrError.error != null) {
                srcTextView.error =
                    resultOrError.error!!.localizedMessage // Show error if translation fails
            } else {
                targetTextView.text = resultOrError.result // Set translated text
            }
        }

        // Update sync toggle button states based on downloaded models list.
        viewModel.availableModels.observe(
            this
        ) { translateRemoteModels ->
            val output = getString(
                R.string.downloaded_models_label,
                translateRemoteModels
            )
            downloadedModelsTextView.text = output // Update downloaded models text view

            // Set sync toggle button states based on whether models are downloaded or not
            sourceSyncButton.isChecked = !viewModel.requiresModelDownload(
                adapter.getItem(sourceLangSelector.selectedItemPosition)!!,
                translateRemoteModels
            )
            targetSyncButton.isChecked = !viewModel.requiresModelDownload(
                adapter.getItem(targetLangSelector.selectedItemPosition)!!,
                translateRemoteModels
            )
        }
    }

    private fun setProgressText(tv: TextView) {
        tv.text = getString(R.string.translate_progress)
    }


    //  tools functions
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == Activity.RESULT_OK) {
            data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                ?.let { spoken ->
                    spokenText = spoken
                    srcTextView.setText(spokenText)
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

}


/**
 * Model class for tracking available models and performing live translations
 */
class TranslateViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        // This specifies the number of translators instance we want to keep in our LRU cache.
        // Each instance of the translator is built with different options based on the source
        // language and the target language, and since we want to be able to manage the number of
        // translator instances to keep around, an LRU cache is an easy way to achieve this.
        private const val NUM_TRANSLATORS = 3
    }

    // RemoteModelManager for managing remote translation models
    private val modelManager: RemoteModelManager = RemoteModelManager.getInstance()

    // HashMap to keep track of pending model downloads
    private val pendingDownloads: HashMap<String, Task<Void>> = hashMapOf()

    // LruCache to store Translator instances
    private val translators =
        object : LruCache<TranslatorOptions, Translator>(NUM_TRANSLATORS) {
            override fun create(options: TranslatorOptions): Translator {
                return Translation.getClient(options)
            }

            override fun entryRemoved(
                evicted: Boolean,
                key: TranslatorOptions,
                oldValue: Translator,
                newValue: Translator?,
            ) {
                oldValue.close()
            }
        }

    // MutableLiveData to hold source language
    val sourceLang = MutableLiveData<Language>()

    // MutableLiveData to hold target language
    val targetLang = MutableLiveData<Language>()

    // MutableLiveData to hold source text
    val sourceText = MutableLiveData<String>()

    // MediatorLiveData to hold translated text or error
    val translatedText = MediatorLiveData<ResultOrError>()

    // MutableLiveData to hold available translation models
    val availableModels = MutableLiveData<List<String>>()

    // Gets a list of all available translation languages.
    val availableLanguages: List<Language> =
        TranslateLanguage.getAllLanguages().map { Language(it) }

    init {
        // Create a translation result or error object.
        val processTranslation =
            OnCompleteListener { task ->
                if (task.isSuccessful) {
                    translatedText.value = ResultOrError(task.result, null)
                } else {
                    translatedText.value = ResultOrError(null, task.exception)
                }
                // Update the list of downloaded models as more may have been
                // automatically downloaded due to requested translation.
                fetchDownloadedModels()
            }
        // Start translation if any of the following change: input text, source lang, target lang.
        translatedText.addSource(sourceText) { translate().addOnCompleteListener(processTranslation) }
        val languageObserver =
            Observer<Language> { translate().addOnCompleteListener(processTranslation) }
        translatedText.addSource(sourceLang, languageObserver)
        translatedText.addSource(targetLang, languageObserver)

        // Update the list of downloaded models.
        fetchDownloadedModels()
    }

    // Function to get TranslateRemoteModel for a given language code
    private fun getModel(languageCode: String): TranslateRemoteModel {
        return TranslateRemoteModel.Builder(languageCode).build()
    }

    // Updates the list of downloaded models available for local translation.
    private fun fetchDownloadedModels() {
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { remoteModels ->
                availableModels.value = remoteModels.sortedBy { it.language }.map { it.language }
            }
    }

    // Starts downloading a remote model for local translation.
    internal fun downloadLanguage(language: Language) {
        val model = getModel(TranslateLanguage.fromLanguageTag(language.code)!!)
        var downloadTask: Task<Void>?
        if (pendingDownloads.containsKey(language.code)) {
            downloadTask = pendingDownloads[language.code]
            // found existing task. exiting
            if (downloadTask != null && !downloadTask.isCanceled) {
                return
            }
        }
        downloadTask =
            modelManager.download(model, DownloadConditions.Builder().build())
                .addOnCompleteListener {
                    pendingDownloads.remove(language.code)
                    fetchDownloadedModels()
                }
        pendingDownloads[language.code] = downloadTask
    }

    // Returns if a new model download task should be started.
    fun requiresModelDownload(
        lang: Language,
        downloadedModels: List<String?>?,
    ): Boolean {
        return if (downloadedModels == null) {
            true
        } else !downloadedModels.contains(lang.code) && !pendingDownloads.containsKey(lang.code)
    }

    // Deletes a locally stored translation model.
    internal fun deleteLanguage(language: Language) {
        val model = getModel(TranslateLanguage.fromLanguageTag(language.code)!!)
        modelManager.deleteDownloadedModel(model).addOnCompleteListener { fetchDownloadedModels() }
        pendingDownloads.remove(language.code)
    }

    // Function to initiate translation
    fun translate(): Task<String> {
        val text = sourceText.value
        val source = sourceLang.value
        val target = targetLang.value
        if (source == null || target == null || text.isNullOrEmpty()) {
            return Tasks.forResult("")
        }
        val sourceLangCode = TranslateLanguage.fromLanguageTag(source.code)!!
        val targetLangCode = TranslateLanguage.fromLanguageTag(target.code)!!
        val options =
            TranslatorOptions.Builder()
                .setSourceLanguage(sourceLangCode)
                .setTargetLanguage(targetLangCode)
                .build()
        return translators[options].downloadModelIfNeeded().continueWithTask { task ->
            if (task.isSuccessful) {
                translators[options].translate(text)
            } else {
                Tasks.forException(
                    task.exception
                        ?: Exception(getApplication<Application>().getString(R.string.unknown_error))
                )
            }
        }
    }

    /** Holds the result of the translation or any error. */
    inner class ResultOrError(var result: String?, var error: Exception?)

    /**
     * Holds the language code (i.e. "en") and the corresponding localized full language name (i.e.
     * "English")
     */
    class Language(val code: String) : Comparable<Language> {

        // Function to get localized language name
        private val displayName: String
            get() = Locale(code).displayName

        override fun equals(other: Any?): Boolean {
            if (other === this) {
                return true
            }

            if (other !is Language) {
                return false
            }

            val otherLang = other as Language?
            return otherLang!!.code == code
        }

        override fun toString(): String {
            return "$code - $displayName"
        }

        override fun compareTo(other: Language): Int {
            return this.displayName.compareTo(other.displayName)
        }

        override fun hashCode(): Int {
            return code.hashCode()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Each new instance of a translator needs to be closed appropriately. Here we utilize the
        // ViewModel's onCleared() to clear our LruCache and close each Translator instance when
        // this ViewModel is no longer used and destroyed.
        translators.evictAll()
    }
}
