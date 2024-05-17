package com.venom.trans

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.IOException

/**
 * Utility class for handling Bitmap operations.
 */
class BitmapUtils(private val contentResolver: ContentResolver) {
    /**
     * Retrieves a Bitmap from a content URI and applies any necessary rotation and flipping.
     *
     * @param imageUri The URI of the image.
     * @return The Bitmap with correct orientation.
     */
    fun getBitmapFromUri(imageUri: Uri): Bitmap? {
        // Decode the bitmap from the content URI.
        val decodedBitmap =
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri) ?: return null
        val orientation = getExifOrientationTag(imageUri)
        var rotationDegrees = 0
        var flipX = false
        var flipY = false

        // Determine rotation and flipping based on Exif orientation tag.
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipX = true
            ExifInterface.ORIENTATION_ROTATE_90 -> rotationDegrees = 90
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                rotationDegrees = 90
                flipX = true
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> rotationDegrees = 180
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipY = true
            ExifInterface.ORIENTATION_ROTATE_270 -> rotationDegrees = -90
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                rotationDegrees = -90
                flipX = true
            }

            else -> {} // Default case, no rotation or flipping.
        }

        // Rotate and flip the bitmap.
        return rotateBitmap(decodedBitmap, rotationDegrees, flipX, flipY)
    }

    /**
     * Rotates a Bitmap by the specified degrees and flips it if necessary.
     *
     * @param bitmap The Bitmap to rotate.
     * @param rotationDegrees The degrees to rotate the Bitmap.
     * @param flipX Whether to flip the Bitmap along the X axis.
     * @param flipY Whether to flip the Bitmap along the Y axis.
     * @return The rotated Bitmap.
     */
    private fun rotateBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int,
        flipX: Boolean,
        flipY: Boolean
    ): Bitmap {
        val matrix = Matrix().apply {
            // Rotate the image back to straight.
            postRotate(rotationDegrees.toFloat())

            // Mirror the image along the X or Y axis.
            postScale(if (flipX) -1.0f else 1.0f, if (flipY) -1.0f else 1.0f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            // Recycle the old bitmap if it has changed.
            if (it != bitmap) {
                bitmap.recycle()
            }
        }
    }

    /**
     * Gets the Exif orientation tag from the image.
     *
     * @param imageUri The URI of the image.
     * @return The Exif orientation tag.
     * @throws IOException if there is an error reading the Exif orientation tag.
     */
    private fun getExifOrientationTag(imageUri: Uri): Int {
        if (imageUri.scheme != ContentResolver.SCHEME_CONTENT && imageUri.scheme != ContentResolver.SCHEME_FILE) {
            // If the URI scheme is not content or file, return default orientation.
            return 0
        }
        return contentResolver.openInputStream(imageUri)?.use { inputStream ->
            ExifInterface(inputStream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        } ?: throw IOException("Error reading Exif orientation tag")
    }

    /** Takes an imageUri as input and returns a resized bitmap.
     */
    fun getResizedBitmap(imageUri: Uri): Bitmap {
        val imageBitmap = getBitmapFromUri(imageUri)
        val options = BitmapFactory.Options()
        options.inSampleSize = 4 // Downscale the image to 1/4 of the original size

        val bitmap = Bitmap.createBitmap(0, 0, Bitmap.Config.ARGB_8888)
        //val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageUri), null, options)

        val scaleFactor = (imageBitmap!!.width / 1024).coerceAtLeast(imageBitmap.height / 768)
        return Bitmap.createScaledBitmap(
            imageBitmap,
            (imageBitmap.width / scaleFactor),
            (imageBitmap.height / scaleFactor),
            true
        )
    }
}
