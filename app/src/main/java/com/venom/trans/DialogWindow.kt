package com.venom.trans

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.preference.PreferenceManager

object DialogWindow {
    @JvmStatic
    fun show(message: String?, context: Context?) {

        // Create layout inflater
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_layout, null)

        // Initialize views
        val messageTextView = dialogView.findViewById<TextView>(R.id.translatedDialog)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(
            context!!
        )

        // Apply theme consistently
        val dialogTheme = if (sharedPreferences.getBoolean(
                "light_theme",
                true
            )
        ) R.style.Dialog_Light // Define a light theme style
        else R.style.Dialog_Dark // Define a dark theme style

        // Create AlertDialog with correct theme
        val builder = AlertDialog.Builder(context, dialogTheme)

        // Set up dialog
        builder.setView(dialogView)
        builder.setTitle("Translated Text")
        builder.setNegativeButton("Cancel", null)
        builder.setPositiveButton("OK", null)
        val dialog = builder.create()
        messageTextView.text = message
        messageTextView.scrollY = 1
        messageTextView.setTextIsSelectable(true)
        dialog.show()
    }
}
