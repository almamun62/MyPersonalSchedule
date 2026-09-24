package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.tr

@Composable
fun FormattedNoteView(
    content: String,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    onViewDocument: ((filePath: String, fileName: String) -> Unit)? = null,
    onViewImage: ((filePath: String, title: String) -> Unit)? = null
) {
    val lines = remember(content) { content.lines() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        var lineCount = 0
        lines.forEach { line ->
            if (lineCount >= maxLines) return@Column

            val trimmed = line.trim()
            when {
                // Header 1
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# ").trim(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                    lineCount++
                }
                // Header 2
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## ").trim(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    lineCount++
                }
                // Header 3
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### ").trim(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    lineCount++
                }
                // Checkbox checked
                trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Done".tr,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = parseSimpleMarkdown(trimmed.substring(6)),
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    lineCount++
                }
                // Checkbox unchecked
                trimmed.startsWith("- [ ] ") -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp, top = 1.dp)
                    ) {
                        Icon(
                            Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Pending".tr,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = parseSimpleMarkdown(trimmed.substring(6)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    lineCount++
                }
                // Bullet point
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.padding(start = 8.dp, top = 1.dp)
                    ) {
                        Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = parseSimpleMarkdown(trimmed.substring(2)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    lineCount++
                }
                // Quote
                trimmed.startsWith("> ") -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = parseSimpleMarkdown(trimmed.removePrefix("> ")),
                            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    lineCount++
                }
                // Attachment lines (Photo / Drawing / Voice / Document)
                trimmed.startsWith("📷 Photo: ") || trimmed.startsWith("🖼️ Attachment: ") -> {
                    val filePath = trimmed.substringAfter(": ").trim()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .clickable { onViewImage?.invoke(filePath, "Photo Attachment") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Attached Photo".tr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    lineCount++
                }
                trimmed.startsWith("✍️ Stylus Sketch: ") -> {
                    val filePath = trimmed.substringAfter(": ").trim()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .clickable { onViewImage?.invoke(filePath, "Handwriting Sketch") }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Stylus Drawing".tr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    lineCount++
                }
                trimmed.startsWith("📄 File: ") -> {
                    val filePath = trimmed.substringAfter(": ").trim()
                    val fileName = filePath.substringAfterLast("/")
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .padding(vertical = 2.dp)
                            .clickable { onViewDocument?.invoke(filePath, fileName) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Attached Document ($fileName)".tr, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    lineCount++
                }
                trimmed.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseSimpleMarkdown(line),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    lineCount++
                }
            }
        }
    }
}

@Composable
fun parseSimpleMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    val primaryColor = MaterialTheme.colorScheme.primary
    return remember(text, primaryColor) {
        buildAnnotatedString {
            var i = 0
            while (i < text.length) {
                if (i + 1 < text.length && text[i] == '*' && text[i + 1] == '*') {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(text.substring(i + 2, end))
                        }
                        i = end + 2
                        continue
                    }
                } else if (text[i] == '*') {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                            append(text.substring(i + 1, end))
                        }
                        i = end + 1
                        continue
                    }
                }
                append(text[i])
                i++
            }
        }
    }
}
