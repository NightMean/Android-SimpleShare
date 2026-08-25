package com.foss.simpleshare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

@Composable
fun FastScrollbar(
    listSize: Int,
    scrollState: androidx.compose.runtime.State<Pair<Float, Float>>,
    onScrollTo: (Float) -> Unit,
    modifier: Modifier = Modifier,
    getLabelForIndex: ((Int) -> Char?)? = null
) {
    if (listSize < 10) return

    val isDraggingState = remember { mutableStateOf(false) }
    val currentLetterState = remember { mutableStateOf<Char?>(null) }
    val currentYState = remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val trackHeight = constraints.maxHeight.toFloat()
        val minThumbHeight = with(LocalDensity.current) { 48.dp.toPx() }
        
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 2.dp)
                .width(32.dp) 
                .fillMaxHeight()
                .pointerInput(listSize, trackHeight, scrollState) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            isDraggingState.value = true
                            currentYState.value = offset.y.coerceIn(0f, trackHeight)
                        },
                        onDragEnd = { isDraggingState.value = false },
                        onDragCancel = { isDraggingState.value = false },
                        onVerticalDrag = { change, _ ->
                            val y = change.position.y.coerceIn(0f, trackHeight)
                            currentYState.value = y
                            
                            val (_, fraction) = scrollState.value
                            val thumbHeight = (trackHeight * fraction).coerceAtLeast(minThumbHeight)
                            val travelDistance = trackHeight - thumbHeight
                            
                            val rawProgress = (y - thumbHeight / 2) / travelDistance
                            val progress = rawProgress.coerceIn(0f, 1f)
                            onScrollTo(progress)
                            
                            if (getLabelForIndex != null) {
                                val index = (progress * (listSize - 1)).toInt()
                                currentLetterState.value = getLabelForIndex(index)
                            }
                        }
                    )
                }
        ) {
              Box(
                  modifier = Modifier
                      .align(Alignment.TopCenter)
                      .width(4.dp)
                      .layout { measurable, constraints ->
                          val (progress, fraction) = scrollState.value
                          val thumbHeightValue = (trackHeight * fraction).coerceAtLeast(minThumbHeight)
                          
                          val placeable = measurable.measure(
                              constraints.copy(
                                  minHeight = thumbHeightValue.toInt(),
                                  maxHeight = thumbHeightValue.toInt()
                              )
                          )
                          
                          val travelDistance = trackHeight - thumbHeightValue
                          val isDragging = isDraggingState.value
                          val currentY = currentYState.value
                          
                          val visualY = if (isDragging) {
                              currentY - thumbHeightValue / 2
                          } else {
                              progress * travelDistance
                          }
                          val effectiveY = visualY.coerceIn(0f, travelDistance)
                          
                          layout(placeable.width, placeable.height) {
                              placeable.place(0, effectiveY.toInt())
                          }
                      }
                      .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
              )
        }

        if (getLabelForIndex != null) {
            FastScrollBubble(
                isDraggingState = isDraggingState,
                currentLetterState = currentLetterState,
                currentYState = currentYState,
                trackHeight = trackHeight
            )
        }
    }
}

@Composable
private fun FastScrollBubble(
    isDraggingState: androidx.compose.runtime.State<Boolean>,
    currentLetterState: androidx.compose.runtime.State<Char?>,
    currentYState: androidx.compose.runtime.State<Float>,
    trackHeight: Float
) {
    val isDragging = isDraggingState.value
    val currentLetter = currentLetterState.value
    val currentY = currentYState.value

    if (isDragging && currentLetter != null) {
        val bubbleSize = 64.dp
        val bubbleSizePx = with(LocalDensity.current) { bubbleSize.toPx() }
        val bubbleY = (currentY - bubbleSizePx / 2f).coerceIn(0f, trackHeight - bubbleSizePx) 
        
        Box(
            modifier = Modifier
                .fillMaxSize() 
                .wrapContentSize(Alignment.TopEnd) 
                .offset { IntOffset(0, bubbleY.toInt()) }
                .padding(end = 48.dp) 
                .size(bubbleSize)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentLetter.toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
