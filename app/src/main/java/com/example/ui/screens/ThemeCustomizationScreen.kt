package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.ui.viewmodel.ScheduleViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit
) {
    val currentAccent by viewModel.userPreferencesManager.accentColor.collectAsStateWithLifecycle()

    var redVal by remember { mutableStateOf(currentAccent.primary.red * 255f) }
    var greenVal by remember { mutableStateOf(currentAccent.primary.green * 255f) }
    var blueVal by remember { mutableStateOf(currentAccent.primary.blue * 255f) }

    val previewColor = Color(redVal / 255f, greenVal / 255f, blueVal / 255f)
    val animatedPreviewColor by animateColorAsState(targetValue = previewColor, label = "previewColor")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Theme Customization".tr, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back".tr)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Preset Accents Section
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(AppIcons.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Preset Accents".tr,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppAccents.ALL.forEach { accent ->
                            val isSelected = currentAccent.id == accent.id
                            val scale by animateFloatAsState(
                                targetValue = if (isSelected) 1.02f else 1.0f,
                                animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
                                label = "scale"
                            )

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) accent.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .scale(scale)
                                    .clickable {
                                        viewModel.userPreferencesManager.setAccentColor(accent)
                                        redVal = accent.primary.red * 255f
                                        greenVal = accent.primary.green * 255f
                                        blueVal = accent.primary.blue * 255f
                                    }
                                    .testTag("accent_preset_${accent.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    brush = Brush.linearGradient(accent.softGlowGradient)
                                                )
                                                .border(2.dp, accent.primary, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = accent.onPrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = accent.tag,
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = accent.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (isSelected) {
                                        Badge(containerColor = accent.primary) {
                                            Text("Selected".tr, color = accent.onPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Custom Color Picker Section
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Custom Color Picker".tr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    // Color Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(animatedPreviewColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RGB: (${redVal.toInt()}, ${greenVal.toInt()}, ${blueVal.toInt()})",
                            color = if (animatedPreviewColor.luminance() > 0.5f) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Red Slider
                    Column {
                        Text("Red: ${redVal.toInt()}", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = redVal,
                            onValueChange = { redVal = it },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors(thumbColor = Color.Red, activeTrackColor = Color.Red)
                        )
                    }

                    // Green Slider
                    Column {
                        Text("Green: ${greenVal.toInt()}", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = greenVal,
                            onValueChange = { greenVal = it },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors(thumbColor = Color.Green, activeTrackColor = Color.Green)
                        )
                    }

                    // Blue Slider
                    Column {
                        Text("Blue: ${blueVal.toInt()}", style = MaterialTheme.typography.labelSmall)
                        Slider(
                            value = blueVal,
                            onValueChange = { blueVal = it },
                            valueRange = 0f..255f,
                            colors = SliderDefaults.colors(thumbColor = Color.Blue, activeTrackColor = Color.Blue)
                        )
                    }

                    Button(
                        onClick = {
                            val customAccent = AppAccentColor(
                                id = "custom_${previewColor.value}",
                                name = "Custom Color",
                                tag = "🎨 Custom",
                                primary = previewColor,
                                onPrimary = if (previewColor.luminance() > 0.5f) Color.Black else Color.White,
                                primaryContainer = previewColor.copy(alpha = 0.2f),
                                onPrimaryContainer = previewColor,
                                glowColor = previewColor,
                                softGlowGradient = listOf(previewColor, previewColor.copy(alpha = 0.6f), previewColor.copy(alpha = 0.2f))
                            )
                            viewModel.userPreferencesManager.setAccentColor(customAccent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_custom_color_button")
                    ) {
                        Text("Apply Custom Color".tr)
                    }
                }
            }

            // 3. Live Theme Preview Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Live Theme Preview".tr,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(onClick = {}, modifier = Modifier.weight(1f)) {
                            Text("Primary Button".tr)
                        }
                        OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) {
                            Text("Secondary Action".tr)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Sample Badge".tr,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("New", color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
