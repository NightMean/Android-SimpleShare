package com.foss.simpleshare.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilterModeSelector(
    selectedMode: String,
    customExtensions: String,
    onModeSelected: (String) -> Unit,
    onCustomExtensionsChanged: (String) -> Unit,
    isError: Boolean = false
) {
    Column {
        
        // Preset: All Files
        val isAllSelected = selectedMode == "PRESET_ALL"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onModeSelected("PRESET_ALL") }
                .then(
                    if (isAllSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    else Modifier
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isAllSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                 else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder, // Using Folder icon for "All"
                    contentDescription = null,
                    tint = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Everything",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Show all files in current folder", 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Preset: Media
        val isMediaSelected = selectedMode == "PRESET_MEDIA"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onModeSelected("PRESET_MEDIA") }
                .then(
                    if (isMediaSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    else Modifier
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isMediaSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                 else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = null,
                    tint = if (isMediaSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Media Only",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isMediaSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Marquee Text
                    // Using basicMarquee for circular animation
                    // Constructing string
                    val extensions = "jpg, jpeg, png, gif, mp4, mkv, webm, avi, heic, webp"
                    Text(
                        text = extensions, 
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE
                        )
                    )
                }
            }
        }

        // Preset: Custom
        val isCustomSelected = selectedMode == "CUSTOM"
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onModeSelected("CUSTOM") }
                 .then(
                    if (isCustomSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    else Modifier
                ),
             colors = CardDefaults.cardColors(
                containerColor = if (isCustomSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                 else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Custom Extensions",
                        style = MaterialTheme.typography.titleMedium,
                         color = if (isCustomSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                if (isCustomSelected) {
                     Spacer(modifier = Modifier.height(16.dp))
                     OutlinedTextField(
                        value = customExtensions,
                        onValueChange = { 
                            // Allow alphanumeric, comma, dot, space
                            if (it.all { char -> char.isLetterOrDigit() || char == ',' || char == '.' || char == ' ' }) {
                                onCustomExtensionsChanged(it) 
                            }
                        },
                        label = { Text("Extensions (comma separated)") },
                        placeholder = { Text("e.g. pdf, zip, .apk") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = isError,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                        )
                     )
                     Spacer(modifier = Modifier.height(4.dp))
                     Text(
                         text = "Supports extensions with or without dot (e.g. jpg, .pdf)",
                         style = MaterialTheme.typography.labelSmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant
                     )
                }
            }
        }
    }
}
