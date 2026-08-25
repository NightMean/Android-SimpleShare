package com.foss.simpleshare.ui.components

import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.grid.LazyGridLayoutInfo
import androidx.compose.foundation.lazy.LazyListItemInfo

/**
 * Pure scroll-position math for the fast scrollbar.
 *
 * Both functions return `(progress, thumbFraction)`:
 *  - progress: how far the content is scrolled, 0f..1f
 *  - thumbFraction: viewport size relative to content size (thumb height ratio)
 */

/** Compute scroll progress for a linear [LazyListLayoutInfo] with uniform item heights. */
fun computeListScrollProgress(layout: LazyListLayoutInfo): Pair<Float, Float> {
    val totalItems = layout.totalItemsCount
    val visibleInfo = layout.visibleItemsInfo
    if (totalItems == 0 || visibleInfo.isEmpty()) return 0f to 0f

    val itemHeight = visibleInfo.first().size
    val viewportHeight = layout.viewportSize.height.toFloat()
    if (itemHeight <= 0) return 0f to 0f

    val contentHeight = itemHeight.toFloat() * totalItems
    // Offset of the first visible item is <= 0 once partially scrolled out;
    // convert to a positive scroll amount.
    val scrollOffset = (visibleInfo.first().index * itemHeight) - visibleInfo.first().offset

    val fraction = (viewportHeight / contentHeight).coerceIn(0f, 1f)
    val progress =
        if (contentHeight > viewportHeight) (scrollOffset / (contentHeight - viewportHeight)).coerceIn(0f, 1f)
        else 0f
    return progress to fraction
}

/**
 * Compute scroll progress for a grid [LazyGridLayoutInfo].
 * Rows are derived from the first visible item's width; items are assumed uniform.
 */
fun computeGridScrollProgress(layout: LazyGridLayoutInfo): Pair<Float, Float> {
    val totalItems = layout.totalItemsCount
    val visibleInfo = layout.visibleItemsInfo
    if (totalItems == 0 || visibleInfo.isEmpty()) return 0f to 0f

    val firstItem = visibleInfo.first()
    val itemHeight = firstItem.size.height
    val itemWidth = firstItem.size.width
    val viewportWidth = layout.viewportSize.width
    val viewportHeight = layout.viewportSize.height.toFloat()

    if (itemHeight <= 0 || itemWidth <= 0) return 0f to 0f

    val spanCount = (viewportWidth / itemWidth).coerceAtLeast(1)
    val totalRows = (totalItems + spanCount - 1) / spanCount

    // Use row index for the calculation so partial columns don't skew progress.
    val currentRow = firstItem.index / spanCount
    // Offset of the first visible row is <= 0 once partially scrolled out.
    val rowOffset = -firstItem.offset.y.toInt()

    val contentHeight = totalRows * itemHeight.toFloat()
    val scrollOffset = (currentRow * itemHeight) + rowOffset

    val fraction = (viewportHeight / contentHeight).coerceIn(0f, 1f)
    val progress =
        if (contentHeight > viewportHeight) (scrollOffset / (contentHeight - viewportHeight)).coerceIn(0f, 1f)
        else 0f
    return progress to fraction
}

/** Find the index of the list item occupying the given y offset, if any. */
fun listItemIndexAtOffset(layout: LazyListLayoutInfo, y: Float): Int? =
    layout.visibleItemsInfo.firstOrNull { item ->
        y >= item.offset && y <= item.offset + item.size
    }?.index

/** Find the index of the grid item occupying the given x/y offset, if any. */
fun gridItemIndexAtOffset(layout: LazyGridLayoutInfo, x: Float, y: Float): Int? =
    layout.visibleItemsInfo.firstOrNull { item ->
        x >= item.offset.x && x <= item.offset.x + item.size.width &&
                y >= item.offset.y && y <= item.offset.y + item.size.height
    }?.index
