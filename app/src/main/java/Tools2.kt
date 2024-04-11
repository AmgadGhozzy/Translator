package com.venom.trans

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern


class Tools2 {

    // Date and Time Utils

    /**
     * Formats the given date into the specified format.
     * @param date The date to format.
     * @param format The format string (e.g., "yyyy-MM-dd HH:mm:ss").
     * @return The formatted date string.
     */
    fun formatDate(date: Date, format: String): String {
        val formatter = SimpleDateFormat(format, Locale.getDefault())
        return formatter.format(date)
    }

    /**
     * Calculates the time difference between two dates in milliseconds.
     * @param start The start date.
     * @param end The end date.
     * @return The time difference in milliseconds.
     */
    fun getTimeDifference(start: Date, end: Date): Long {
        return end.time - start.time
    }

    /**
     * Returns the current date and time in "yyyy-MM-dd HH:mm:ss" format.
     * @return The current date and time string.
     */
    fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    // File Utils

    /**
     * Reads the contents of a file as a string.
     * @param filePath The path of the file to read.
     * @return The contents of the file as a string.
     */
    fun readFile(filePath: String): String {
        val file = File(filePath)
        return file.readText()
    }

    /**
     * Writes content to a file.
     * @param filePath The path of the file to write to.
     * @param content The content to write to the file.
     */
    fun writeFile(filePath: String, content: String) {
        val file = File(filePath)
        file.writeText(content)
    }

    /**
     * Creates a directory at the specified path.
     * @param directoryPath The path of the directory to create.
     */
    fun createDirectory(directoryPath: String) {
        val directory = File(directoryPath)
        directory.mkdirs()
    }

    /**
     * Checks if a file exists at the specified path.
     * @param filePath The path of the file to check.
     * @return True if the file exists, false otherwise.
     */
    fun doesFileExist(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists()
    }

    /**
     * Creates a file with the given name and content in the external storage directory.
     * @param fileName The name of the file to create.
     * @param fileContent The content to write to the file.
     * @return The created file object.
     */
    fun createFile(fileName: String, fileContent: String): File {
        val directory = File(Environment.getExternalStorageDirectory(), "YourDirectory")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        file.writeText(fileContent)
        return file
    }

    // Permission Utils

    /**
     * Checks if the app has the specified permission.
     * @param context The context.
     * @param permission The permission to check.
     * @return True if the app has the permission, false otherwise.
     */
    fun hasPermission(context: Context, permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Requests a permission from the user.
     * @param activity The activity.
     * @param permission The permission to request.
     * @param requestCode The request code to identify the permission request.
     */
    fun requestPermission(activity: Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

    /**
     * Opens the app settings page.
     * @param context The context.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        intent.data = Uri.fromParts("package", context.packageName, null)
        context.startActivity(intent)
    }

    // Validation Utils

    /**
     * Validates a password.
     * @param password The password to validate.
     * @return True if the password is valid, false otherwise.
     */
    fun isValidPassword(password: CharSequence): Boolean {
        // Implement your password validation logic here
        return password.length >= 8
    }

    /**
     * Validates an email address.
     * @param email The email address to validate.
     * @return True if the email address is valid, false otherwise.
     */
    fun isValidEmail(email: String): Boolean {
        val pattern = Pattern.compile(
            "^(([\\w-]+\\.)+[\\w-]+|([a-zA-Z]{1}|[\\w-]{2,}))@"
                    + "[a-zA-Z]{2,4}$"
        )
        return pattern.matcher(email).matches()
    }
//    // Getting the current date and time
//    val currentDateTime = Tools2().getCurrentDateTime()
//
//    // Reading a file
//    val fileContent = Tools2().readFile("/path/to/file.txt")
//
//    // Checking permission
//    val hasPermission = Tools2().hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
//
//    // Validating a password
//    val isValidPassword = Tools2().isValidPassword(password)
//
//    // Validating an email
//    val isValidEmail = Tools2().isValidEmail(email)

}
