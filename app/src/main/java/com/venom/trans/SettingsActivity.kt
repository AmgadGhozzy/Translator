package com.venom.trans

import android.app.ActivityOptions
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.materialswitch.MaterialSwitch

class SettingsActivity : AppCompatActivity() {
    private val navListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item: MenuItem ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Handle home action
                    val options =
                        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra(
                            "LastText",
                            intent.getStringExtra("LastText")
                        )
                    }
                    startActivity(intent, options.toBundle())

                    Toast.makeText(this, "Home clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.navigation_offline -> {
                    // Handle Offline action
                    val options =
                        ActivityOptions.makeCustomAnimation(this, R.anim.slide_in, R.anim.slide_out)
                    val intent = Intent(this, TranslateOffline::class.java)
                    startActivity(intent, options.toBundle())

                    Toast.makeText(this, "Offline clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                R.id.navigation_setting -> {
                    // Handle setting action
                    Toast.makeText(this, "Setting clicked", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> {
                    item.isChecked = true
                    false
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences = getDefaultSharedPreferences(this)
        val isLightTheme = sharedPreferences.getBoolean("light_theme", true)
        setTheme(if (isLightTheme) R.style.AppTheme_Light else R.style.AppTheme_Dark)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)


        val sharedEditor = sharedPreferences.edit()
        val themeButton = findViewById<Button>(R.id.ThemeButton)

        val themeSwitch = findViewById<MaterialSwitch>(R.id.switch_theme)
        themeSwitch.setChecked(isLightTheme)
        themeSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            if (isChecked) {
                // Update the shared preference value based on the state of the switch
                sharedEditor.putBoolean("light_theme", true).apply()
                recreate()
            } else {
                // Update the shared preference value based on the state of the switch
                sharedEditor.putBoolean("light_theme", false).apply()
                recreate()
            }
        }


        val dialogSwitch = findViewById<MaterialSwitch>(R.id.switch_dialog)
        val isInDialog = sharedPreferences.getBoolean("trans_in_dialog", false)
        dialogSwitch.setChecked(isInDialog)
        dialogSwitch.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
            sharedEditor.putBoolean("trans_in_dialog", isChecked).apply()
        }


        val bottomNavigationView =
            findViewById<BottomNavigationView>(R.id.bottom_navigation) // Initialize bottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener(navListener) // Set the Navigation as the action bar
        bottomNavigationView.selectedItemId = R.id.navigation_setting
    }
}
