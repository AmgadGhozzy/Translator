package com.venom.trans

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.google.mlkit.vision.text.Text

/**
 * Graphic instance for rendering TextBlock position, size, and ID within an associated graphic
 * overlay view.
 */
class TextGraphic internal constructor(
    overlay: GraphicOverlay,
    private val textElement: Text.Element?
) : GraphicOverlay.Graphic(overlay) {

    private val rectPaint: Paint = Paint().apply {
        color = TEXT_COLOR
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
    }

    private val textPaint: Paint = Paint().apply {
        color = TEXT_COLOR
        textSize = TEXT_SIZE
    }

    /**
     * Draws the textElement block annotations for position, size, and raw value on the supplied canvas.
     */
    override fun draw(canvas: Canvas) {
        textElement ?: return // Skip drawing if textElement is null

        // Draws the bounding box around the TextBlock.
        val rect = RectF(textElement.boundingBox)
        rect.left = translateX(rect.left)
        rect.top = translateY(rect.top)
        rect.right = translateX(rect.right)
        rect.bottom = translateY(rect.bottom)
        canvas.drawRect(rect, rectPaint)

        // Renders the textElement at the bottom of the box with padding for readability.
        val padding = TEXT_PADDING
        canvas.drawText(textElement.text, rect.left + padding, rect.bottom - padding, textPaint)
    }

    companion object {
        private const val TEXT_COLOR = Color.WHITE
        private const val TEXT_SIZE = 54.0f
        private const val STROKE_WIDTH = 4.0f
        private const val TEXT_PADDING = 20.0f // Padding for text rendering
    }
}
