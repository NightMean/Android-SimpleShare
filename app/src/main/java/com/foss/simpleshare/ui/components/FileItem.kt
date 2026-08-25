package com.foss.simpleshare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.awaitCancellation
import com.foss.simpleshare.data.FileModel

@Composable
fun FileListItem(
    file: FileModel,
    showThumbnail: Boolean,
    isPressed: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    // Drive Ripple from isPressed state
    LaunchedEffect(isPressed) {
        if (isPressed) {
             val press = androidx.compose.foundation.interaction.PressInteraction.Press(androidx.compose.ui.geometry.Offset.Zero)
             interactionSource.emit(press)
             try {
                 awaitCancellation()
             } finally {
                 interactionSource.emit(androidx.compose.foundation.interaction.PressInteraction.Release(press))
             }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .indication(interactionSource, androidx.compose.material3.ripple())
            .background(if (file.isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FileThumbnail(file = file, showThumbnail = showThumbnail, modifier = Modifier.size(48.dp))
        
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!file.isDirectory) {
                Text(
                    text = "${formatFileSize(file.size)} • ${formatFileDate(LocalContext.current, file.file.lastModified())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                // Show placeholder "..." while size/count are being calculated (-1)
                val sizeText = if (file.size == -1L) "..." else formatFileSize(file.size)
                val countText = if (file.itemCount == -1) "…" else "${file.itemCount} items"
                 Text(
                    text = "$countText • $sizeText",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!file.isDirectory) {
            Checkbox(
                checked = file.isSelected,
                onCheckedChange = { onClick() }
            )
        }
    }
}

@Composable
fun FileGridItem(
    file: FileModel,
    showThumbnail: Boolean,
    isPressed: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Drive Ripple from isPressed state
    LaunchedEffect(isPressed) {
        if (isPressed) {
             val press = androidx.compose.foundation.interaction.PressInteraction.Press(androidx.compose.ui.geometry.Offset.Zero)
             interactionSource.emit(press)
             try {
                 awaitCancellation()
             } finally {
                 interactionSource.emit(androidx.compose.foundation.interaction.PressInteraction.Release(press))
             }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .aspectRatio(1f)
            .indication(interactionSource, androidx.compose.material3.ripple())
            .border(
                width = if (file.isSelected) 3.dp else 0.dp,
                color = if (file.isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                   FileThumbnail(file = file, showThumbnail = showThumbnail, modifier = Modifier.fillMaxSize())
                }
                
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(6.dp)
                )
            }
             if (file.isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                )
            }
        }
    }
}

@OptIn(com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi::class)
@Composable
fun FileThumbnail(
    file: FileModel,
    showThumbnail: Boolean,
    modifier: Modifier = Modifier
) {
    if (file.isDirectory) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
             Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = "Directory",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    } else {

        val isVideo = file.extension in setOf("mp4", "mkv", "webm", "avi", "mov", "3gp")
        val isImage = file.extension in setOf("jpg", "jpeg", "png", "gif", "heic", "webp", "bmp")
        val isAudio = file.extension in setOf("mp3", "wav", "flac", "ogg", "m4a", "aac", "wma")
        val isArchive = file.extension in setOf("zip", "rar", "7z", "tar", "gz", "bz2")
        val isPdf = file.extension == "pdf"
        val isDoc = file.extension in setOf("doc", "docx", "txt", "rtf", "odt", "xls", "xlsx", "ppt", "pptx")
        val isApp = file.extension in setOf("apk", "apkm", "xapk")
        
        val iconVector = when {
            isVideo -> Icons.Default.VideoFile
            isImage -> Icons.Default.Image
            isAudio -> Icons.Default.AudioFile
            isArchive -> Icons.Default.FolderZip
            isPdf -> Icons.Default.PictureAsPdf
            isDoc -> Icons.Default.Description
            isApp -> Icons.Default.Android
            else -> Icons.Default.InsertDriveFile // Fallback
        }

        // We use a Box to layer the GlideImage OVER the default Icon.
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant), // Always show background for consistency
            contentAlignment = Alignment.Center
        ) {
            
            // Reusable Icon Composable
            val FileIcon = @Composable {
                if (!isVideo && !isImage && !isAudio && !isArchive && !isPdf && !isDoc && !isApp) {
                     Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.fillMaxSize()) {
                         Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxSize(0.5f).align(Alignment.Center)
                        )
                         Icon(
                            imageVector = Icons.Default.QuestionMark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp).align(Alignment.Center)
                        )
                     }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxSize(0.5f)
                        )
                    }
                }
            }

            // Logic:
            // 1. Always show FileIcon (Background Layer) -> Handles Loading & Failure states
            FileIcon()
            
            // 2. Show GlideImage (Foreground Layer) -> Covers icon when loaded
            if (showThumbnail && (isImage || isVideo)) {
                com.bumptech.glide.integration.compose.GlideImage(
                    model = file.file,
                    contentDescription = "Thumbnail",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                ) {
                    it.override(256)
                }
            }
        }
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatFileDate(context: android.content.Context, timestamp: Long): String {
    val date = java.util.Date(timestamp)
    val dateFormat = android.text.format.DateFormat.getDateFormat(context)
    val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
    return "${dateFormat.format(date)} ${timeFormat.format(date)}"
}
