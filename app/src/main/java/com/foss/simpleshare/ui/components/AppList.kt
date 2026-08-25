package com.foss.simpleshare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foss.simpleshare.data.AppModel
import androidx.compose.ui.res.stringResource
import com.foss.simpleshare.R
import com.foss.simpleshare.ui.components.rememberDrawablePainter
import kotlinx.coroutines.launch

@Composable
fun AppList(
    apps: List<AppModel>,
    searchQuery: String,
    selectedPackage: String?,
    onAppSelected: (String) -> Unit 
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val filteredApps by remember(apps, searchQuery) {
        derivedStateOf {
            if (searchQuery.isBlank()) apps
            else apps.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                it.packageName.contains(searchQuery, ignoreCase = true) 
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(filteredApps.size) { index ->
                val app = filteredApps[index]
                val componentString = "${app.packageName}/${app.activityName}"
                // Handle legacy package-only selection or component selection
                val isSelected = componentString == selectedPackage || selectedPackage == app.packageName
                
                ListItem(
                    headlineContent = { Text(app.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    supportingContent = { Text(app.packageName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingContent = {
                        Image(
                            painter = rememberDrawablePainter(drawable = app.icon),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    },
                    trailingContent = {
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.cd_selected), tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = if (isSelected) ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) else ListItemDefaults.colors(),
                    modifier = Modifier.clickable { onAppSelected(componentString) }
                )
                Divider()
            }
        }
        
        val scrollStateValues = remember(listState, filteredApps) {
            derivedStateOf {
                val totalItems = listState.layoutInfo.totalItemsCount
                val visibleItemsCount = listState.layoutInfo.visibleItemsInfo.size
                val firstIndex = listState.firstVisibleItemIndex
                val firstOffset = listState.firstVisibleItemScrollOffset
                val itemSize = listState.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 0
                
                if (totalItems == 0 || visibleItemsCount == 0 || itemSize <= 0) 0f to 0f
                else {
                     val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
                     val contentHeight = itemSize * totalItems.toFloat()
                     val scrollOffset = (firstIndex * itemSize) + firstOffset
                     
                     val fraction = (viewportHeight / contentHeight).coerceIn(0f, 1f)
                     // Calculate progress based on actual pixel offset rather than just index for smoothness
                     val progress = (scrollOffset / (contentHeight - viewportHeight)).coerceIn(0f, 1f)
                     progress to fraction
                }
            }
        }
        
        FastScrollbar(
             listSize = filteredApps.size,
             scrollState = scrollStateValues,
             modifier = Modifier.padding(vertical = 16.dp),
             getLabelForIndex = { index -> 
                filteredApps.getOrNull(index)?.name?.firstOrNull()?.uppercaseChar() ?: '#' 
             },
             onScrollTo = { progress ->
                  val totalItems = filteredApps.size
                  val targetIndex = (progress * (totalItems - 1)).toInt()
                  coroutineScope.launch {
                      listState.scrollToItem(targetIndex)
                  }
             }
        )
    }
}
