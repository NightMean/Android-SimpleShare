package com.foss.simpleshare.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Minimal Painter for android.graphics.drawable instances.
 *
 * Replaces the deprecated Accompanist drawablepainter. The app only paints
 * static app/activity icons, so drawable invalidation callbacks are not
 * wired; revisit if animated drawables ever need to be shown.
 */
class DrawablePainter(private val drawable: Drawable?) : Painter() {

    override val intrinsicSize: Size
        get() {
            val d = drawable ?: return Size.Unspecified
            val w = d.intrinsicWidth
            val h = d.intrinsicHeight
            return if (w > 0 && h > 0) Size(w.toFloat(), h.toFloat()) else Size.Unspecified
        }

    override fun DrawScope.onDraw() {
        val d = drawable ?: return
        d.setBounds(0, 0, size.width.toInt(), size.height.toInt())
        d.draw(drawContext.canvas.nativeCanvas)
    }
}

/** Remember a [DrawablePainter] for [drawable], recreated only when it changes. */
@Composable
fun rememberDrawablePainter(drawable: Drawable?): Painter =
    remember(drawable) { DrawablePainter(drawable) }
