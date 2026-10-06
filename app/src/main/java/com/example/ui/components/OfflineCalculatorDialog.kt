package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.CalculatorMemoryManager
import com.example.ui.viewmodel.ScheduleViewModel
import java.text.DecimalFormat
import kotlin.math.*

data class CalcHistoryItem(
    val equation: String,
    val result: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class CalculatorMode {
    STANDARD,
    SCIENTIFIC,
    TARGET_SCORE_SOLVER,
    GPA_BUFFER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineCalculatorDialog(
    viewModel: ScheduleViewModel? = null,
    isExamLockedMode: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val allCourses = viewModel?.filteredCourses?.collectAsStateWithLifecycle()?.value ?: emptyList()
    val savedScores by CalculatorMemoryManager.savedCourseScores.collectAsStateWithLifecycle()

    var mode by remember { mutableStateOf(if (isExamLockedMode) CalculatorMode.STANDARD else CalculatorMode.STANDARD) }
    var displayText by remember { mutableStateOf("0") }
    var expressionHistory by remember { mutableStateOf("") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var shouldClearOnNextDigit by remember { mutableStateOf(false) }

    // History Tape
    val historyList = remember { mutableStateListOf<CalcHistoryItem>() }
    var showHistorySidebar by remember { mutableStateOf(false) }

    // Floating Bubble / Resizable Mode (Tablet Picture-in-Picture)
    var isFloatingWindow by remember { mutableStateOf(false) }
    var floatOffsetX by remember { mutableFloatStateOf(0f) }
    var floatOffsetY by remember { mutableFloatStateOf(0f) }

    // Target Score Solver inputs
    var solverCurrentGrade by remember { mutableStateOf("85") }
    var solverFinalWeight by remember { mutableStateOf("40") }
    var solverTargetGrade by remember { mutableStateOf("90") }
    var solverResultText by remember { mutableStateOf<String?>(null) }

    // Save to Course Buffer Dialog
    var showSaveToCourseDialog by remember { mutableStateOf(false) }
    var selectedSaveCourseName by remember { mutableStateOf(allCourses.firstOrNull()?.name ?: "General") }

    // Haptic feedback click
    fun triggerHapticClick() {
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (e: Exception) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
                }
            } catch (ex: Exception) {
                // Ignore
            }
        }
    }

    fun formatNumber(num: Double): String {
        return if (num.isInfinite() || num.isNaN()) {
            "Error"
        } else if (num == num.toLong().toDouble()) {
            num.toLong().toString()
        } else {
            val df = DecimalFormat("#.########")
            df.format(num)
        }
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Study Calculator Result", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied $text to clipboard", Toast.LENGTH_SHORT).show()
    }

    fun onDigit(d: String) {
        triggerHapticClick()
        if (shouldClearOnNextDigit || displayText == "0" || displayText == "Error") {
            displayText = d
            shouldClearOnNextDigit = false
        } else {
            if (displayText.length < 14) {
                displayText += d
            }
        }
    }

    fun onDot() {
        triggerHapticClick()
        if (shouldClearOnNextDigit) {
            displayText = "0."
            shouldClearOnNextDigit = false
        } else if (!displayText.contains(".")) {
            displayText += "."
        }
    }

    fun calculate() {
        triggerHapticClick()
        val op = pendingOp ?: return
        val first = operand1 ?: return
        val second = displayText.toDoubleOrNull() ?: return

        val res = when (op) {
            "+" -> first + second
            "-" -> first - second
            "×" -> first * second
            "÷" -> if (second != 0.0) first / second else Double.NaN
            "^" -> first.pow(second)
            else -> second
        }

        val eqString = "${formatNumber(first)} $op ${formatNumber(second)}"
        val resultString = if (res.isNaN()) "Error" else formatNumber(res)

        displayText = resultString
        expressionHistory = eqString
        operand1 = null
        pendingOp = null
        shouldClearOnNextDigit = true

        if (!isExamLockedMode && resultString != "Error") {
            historyList.add(0, CalcHistoryItem(eqString, resultString))
            CalculatorMemoryManager.updateLastResult(resultString, eqString)
        }
    }

    fun onOperator(op: String) {
        triggerHapticClick()
        if (pendingOp != null && !shouldClearOnNextDigit) {
            calculate()
        }
        val currentVal = displayText.toDoubleOrNull() ?: 0.0
        operand1 = currentVal
        pendingOp = op
        expressionHistory = "${formatNumber(currentVal)} $op"
        shouldClearOnNextDigit = true
    }

    fun onScientific(fn: String) {
        triggerHapticClick()
        val num = displayText.toDoubleOrNull() ?: return
        val res = when (fn) {
            "sin" -> sin(Math.toRadians(num))
            "cos" -> cos(Math.toRadians(num))
            "tan" -> tan(Math.toRadians(num))
            "ln" -> if (num > 0) ln(num) else Double.NaN
            "log" -> if (num > 0) log10(num) else Double.NaN
            "√" -> if (num >= 0) sqrt(num) else Double.NaN
            "x²" -> num * num
            "1/x" -> if (num != 0.0) 1.0 / num else Double.NaN
            "π" -> Math.PI
            "e" -> Math.E
            else -> num
        }
        val resString = if (res.isNaN()) "Error" else formatNumber(res)
        val eqString = "$fn(${formatNumber(num)})"
        expressionHistory = eqString
        displayText = resString
        shouldClearOnNextDigit = true
        if (!isExamLockedMode && resString != "Error") {
            historyList.add(0, CalcHistoryItem(eqString, resString))
            CalculatorMemoryManager.updateLastResult(resString, eqString)
        }
    }

    fun onClear() {
        triggerHapticClick()
        displayText = "0"
        expressionHistory = ""
        operand1 = null
        pendingOp = null
        shouldClearOnNextDigit = false
    }

    fun onBackspace() {
        triggerHapticClick()
        if (displayText.length > 1 && displayText != "Error") {
            displayText = displayText.dropLast(1)
        } else {
            displayText = "0"
        }
    }

    fun onPercent() {
        triggerHapticClick()
        val num = displayText.toDoubleOrNull() ?: return
        val res = formatNumber(num / 100.0)
        displayText = res
        CalculatorMemoryManager.updateLastResult(res, "$num%")
    }

    fun onToggleSign() {
        triggerHapticClick()
        val num = displayText.toDoubleOrNull() ?: return
        displayText = formatNumber(-num)
    }

    // Solve "What do I need on final?"
    fun solveTargetScore() {
        triggerHapticClick()
        val current = solverCurrentGrade.toDoubleOrNull()
        val weight = solverFinalWeight.toDoubleOrNull()
        val target = solverTargetGrade.toDoubleOrNull()

        if (current == null || weight == null || target == null || weight <= 0.0 || weight > 100.0) {
            solverResultText = "Please enter valid numbers (Weight: 1-100%)."
            return
        }

        val courseworkWeight = (100.0 - weight) / 100.0
        val finalWeightDecimal = weight / 100.0
        val needed = (target - (current * courseworkWeight)) / finalWeightDecimal

        val neededRounded = (needed * 10.0).roundToInt() / 10.0
        val message = if (needed <= 0.0) {
            "You need 0% on the final! You already secured this grade."
        } else if (needed > 100.0) {
            "You need ${neededRounded}% on the final (Requires extra credit)."
        } else {
            "You need ${neededRounded}% on the final exam."
        }
        solverResultText = message
        displayText = neededRounded.toString()
        CalculatorMemoryManager.updateLastResult("${neededRounded}%", "Final Needed ($solverTargetGrade target)")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFloatingWindow) 16.dp else 0.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .then(
                        if (isFloatingWindow) {
                            Modifier
                                .offset { IntOffset(floatOffsetX.roundToInt(), floatOffsetY.roundToInt()) }
                                .pointerInput(Unit) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        floatOffsetX += dragAmount.x
                                        floatOffsetY += dragAmount.y
                                    }
                                }
                                .width(360.dp)
                                .wrapContentHeight()
                        } else {
                            Modifier
                                .widthIn(max = 500.dp)
                                .fillMaxWidth(0.94f)
                                .wrapContentHeight()
                        }
                    ),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 10.dp,
                shadowElevation = if (isFloatingWindow) 16.dp else 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Bar: Academic Identity & Floating Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isExamLockedMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isExamLockedMode) "EXAM LOCKED" else "STUDY CALC",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isExamLockedMode) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                text = if (isFloatingWindow) "Study Calc (Floating)" else "Academic Study Calculator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Floating Window Toggle (for tablet Picture-in-Picture)
                            IconButton(
                                onClick = {
                                    isFloatingWindow = !isFloatingWindow
                                    floatOffsetX = 0f
                                    floatOffsetY = 0f
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    if (isFloatingWindow) Icons.Outlined.Fullscreen else Icons.Outlined.PictureInPictureAlt,
                                    contentDescription = "Toggle Floating Window",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            // History Tape Toggle
                            if (!isExamLockedMode) {
                                IconButton(
                                    onClick = { showHistorySidebar = !showHistorySidebar },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.History,
                                        contentDescription = "Calculation History Tape",
                                        modifier = Modifier.size(18.dp),
                                        tint = if (showHistorySidebar) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // 1. Academic Modes Segmented Row
                    if (!isExamLockedMode) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val modes = listOf(
                                CalculatorMode.STANDARD to "Standard",
                                CalculatorMode.SCIENTIFIC to "Scientific",
                                CalculatorMode.TARGET_SCORE_SOLVER to "Target Score 🎯",
                                CalculatorMode.GPA_BUFFER to "GPA Buffer"
                            )
                            modes.forEachIndexed { idx, (m, label) ->
                                SegmentedButton(
                                    selected = mode == m,
                                    onClick = {
                                        triggerHapticClick()
                                        mode = m
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index = idx, count = modes.size)
                                ) {
                                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }
                        }
                    }

                    // 2. TARGET SCORE SOLVER VIEW
                    if (mode == CalculatorMode.TARGET_SCORE_SOLVER && !isExamLockedMode) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Outlined.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    Text("What do I need on the Final?", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = solverCurrentGrade,
                                        onValueChange = { solverCurrentGrade = it },
                                        label = { Text("Current Grade", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = solverFinalWeight,
                                        onValueChange = { solverFinalWeight = it },
                                        label = { Text("Final Weight %", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = solverTargetGrade,
                                        onValueChange = { solverTargetGrade = it },
                                        label = { Text("Target Goal", fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }

                                Button(
                                    onClick = { solveTargetScore() },
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Solve Target Score", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                if (solverResultText != null) {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = solverResultText!!,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.weight(1f)
                                            )
                                            IconButton(onClick = { copyToClipboard(displayText) }) {
                                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy Result", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. GPA BUFFER VIEW (Course-Linked Memory)
                    if (mode == CalculatorMode.GPA_BUFFER && !isExamLockedMode) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Course-Linked Scores (Semester Memory)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    TextButton(onClick = { showSaveToCourseDialog = true }) {
                                        Text("+ Link Result", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (savedScores.isEmpty()) {
                                    Text(
                                        "No linked grades saved yet. Calculate a score and tap 'Save to Course' to track exam buffers.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                                        items(savedScores.entries.toList()) { entry ->
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = MaterialTheme.colorScheme.surface,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 3.dp)
                                                    .clickable {
                                                        val num = entry.value.filter { it.isDigit() || it == '.' }
                                                        if (num.isNotBlank()) displayText = num
                                                        Toast.makeText(context, "Loaded ${entry.key} score", Toast.LENGTH_SHORT).show()
                                                    }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(entry.key, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text(entry.value, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. DISPLAY SCREEN WITH COPY & HYPERISLAND SYNC
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(82.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Copy result icon
                                    IconButton(
                                        onClick = { copyToClipboard(displayText) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy Result", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    // Save to course memory button
                                    if (!isExamLockedMode) {
                                        TextButton(
                                            onClick = { showSaveToCourseDialog = true },
                                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save to Course", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Text(
                                    text = expressionHistory,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = displayText,
                                fontSize = if (displayText.length > 9) 28.sp else 34.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }

                    // 5. HISTORY TAPE (SLIDE-OUT ROLL)
                    AnimatedVisibility(visible = showHistorySidebar && !isExamLockedMode) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("History Tape", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    TextButton(onClick = { historyList.clear() }) {
                                        Text("Clear", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                if (historyList.isEmpty()) {
                                    Text("No calculation history yet.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    LazyColumn(modifier = Modifier.heightIn(max = 110.dp)) {
                                        items(historyList) { item ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        displayText = item.result
                                                        expressionHistory = item.equation
                                                    }
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(item.equation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("= ${item.result}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 6. SCIENTIFIC FUNCTIONS DRAWER (Swipe/Mode reveal)
                    if (mode == CalculatorMode.SCIENTIFIC && !isExamLockedMode) {
                        val sciRow1 = listOf("sin", "cos", "tan", "ln", "log")
                        val sciRow2 = listOf("√", "x²", "^", "π", "e")

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sciRow1.forEach { fn ->
                                    Button(
                                        onClick = { onScientific(fn) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Text(fn, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                sciRow2.forEach { fn ->
                                    Button(
                                        onClick = {
                                            if (fn == "^") onOperator("^")
                                            else onScientific(fn)
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Text(fn, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    }
                                }
                            }
                        }
                    }

                    // 7. KEYPAD GRID
                    val buttons = listOf(
                        listOf("C", "±", "%", "÷"),
                        listOf("7", "8", "9", "×"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("0", ".", "⌫", "=")
                    )

                    buttons.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { key ->
                                val isOperator = key in listOf("÷", "×", "-", "+", "=")
                                val isAction = key in listOf("C", "±", "%", "⌫")

                                val btnBg = when {
                                    key == "=" -> MaterialTheme.colorScheme.primary
                                    isOperator -> MaterialTheme.colorScheme.primaryContainer
                                    isAction -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }

                                val textColor = when {
                                    key == "=" -> MaterialTheme.colorScheme.onPrimary
                                    isOperator -> MaterialTheme.colorScheme.onPrimaryContainer
                                    isAction -> MaterialTheme.colorScheme.onSecondaryContainer
                                    else -> MaterialTheme.colorScheme.onSurface
                                }

                                Button(
                                    onClick = {
                                        when (key) {
                                            "C" -> onClear()
                                            "±" -> onToggleSign()
                                            "%" -> onPercent()
                                            "⌫" -> onBackspace()
                                            "=" -> calculate()
                                            "÷", "×", "-", "+" -> onOperator(key)
                                            "." -> onDot()
                                            else -> onDigit(key)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = btnBg),
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save to Course Buffer Dialog
    if (showSaveToCourseDialog) {
        val courseNames = remember(allCourses) {
            if (allCourses.isEmpty()) listOf("General", "Math", "Physics", "Computer Science")
            else allCourses.map { it.name }.distinct()
        }
        var courseDesc by remember { mutableStateOf("Final Exam Buffer") }

        AlertDialog(
            onDismissRequest = { showSaveToCourseDialog = false },
            title = { Text("Save Score to Course Memory", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Link current calculated value ($displayText) to a semester course:", fontSize = 12.sp)

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(courseNames) { cName ->
                            FilterChip(
                                selected = selectedSaveCourseName == cName,
                                onClick = { selectedSaveCourseName = cName },
                                label = { Text(cName, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = courseDesc,
                        onValueChange = { courseDesc = it },
                        label = { Text("Note / Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        CalculatorMemoryManager.saveScoreToCourse(selectedSaveCourseName, displayText, courseDesc)
                        Toast.makeText(context, "Saved $displayText to $selectedSaveCourseName", Toast.LENGTH_SHORT).show()
                        showSaveToCourseDialog = false
                    }
                ) {
                    Text("Save to Memory")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveToCourseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
