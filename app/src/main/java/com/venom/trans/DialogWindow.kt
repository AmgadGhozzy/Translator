package com.venom.trans
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import androidx.preference.PreferenceManager

object DialogWindow {
    fun show(message: String?, dialogTitle: String, context: Context) {
        // Create layout inflater
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.dialog_layout, null)

        // Initialize views
        val messageTextView = dialogView.findViewById<TextView>(R.id.translatedDialog)

        // Fetch theme preference
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val dialogTheme = if (sharedPreferences.getBoolean("light_theme", true))
            R.style.Dialog_Light
        else
            R.style.Dialog_Dark

        // Create AlertDialog with correct theme
        val builder = AlertDialog.Builder(context, dialogTheme)

        // Set up dialog
        builder.setView(dialogView)
            .setTitle(dialogTitle)
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        // Set message
        setMessage(messageTextView, message)

        dialog.show()
    }

    private fun setMessage(textView: TextView, message: String?) {
        textView.apply {
            text = message
            scrollY = 1
            setTextIsSelectable(true)
        }
    }
}
