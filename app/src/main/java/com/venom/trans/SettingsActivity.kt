package com.venom.trans

import android.app.ActivityOptions
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.venom.trans.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity(),
    BottomNavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val isLightTheme = sharedPreferences.getBoolean("light_theme", false)
        setTheme(if (isLightTheme) R.style.AppTheme_Light else R.style.AppTheme_Dark)

        setupThemeSwitch(sharedPreferences)
        setupDialogSwitch(sharedPreferences)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(this)
        bottomNavigationView.selectedItemId = R.id.navigation_setting
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.navigation_home -> {
                val options =
                    ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
                val intent = Intent(this, MainActivity::class.java).apply {
                    putExtra("LastText", intent.getStringExtra("LastText"))
                }
                startActivity(intent, options.toBundle())
                Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.navigation_offline -> {
                val options =
                    ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
                val intent = Intent(this, TranslateOffline::class.java)
                startActivity(intent, options.toBundle())
                Toast.makeText(this, "Offline clicked", Toast.LENGTH_SHORT).show()
                true
            }

            R.id.navigation_setting -> {
                Toast.makeText(this, "Setting clicked", Toast.LENGTH_SHORT).show()
                true
            }

            else -> false
        }
    }

    private fun setupThemeSwitch(sharedPreferences: SharedPreferences) {
        val sharedEditor = sharedPreferences.edit()
        val themeSwitch = binding.switchTheme
        val isLightTheme = sharedPreferences.getBoolean("light_theme", false)
        themeSwitch.isChecked = isLightTheme

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedEditor.putBoolean("light_theme", isChecked).apply()
            recreate() // Recreate activity to apply theme change
        }
    }

    private fun setupDialogSwitch(sharedPreferences: SharedPreferences) {
        val sharedEditor = sharedPreferences.edit()
        val dialogSwitch = binding.switchDialog
        val isInDialog = sharedPreferences.getBoolean("trans_in_dialog", false)
        dialogSwitch.isChecked = isInDialog

        dialogSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedEditor.putBoolean("trans_in_dialog", isChecked).apply()
        }
    }
}
