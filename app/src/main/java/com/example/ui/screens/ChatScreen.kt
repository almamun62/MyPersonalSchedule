package com.example.ui.screens

import android.content.Intent
import com.example.ui.theme.tr
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.ChatViewModel
import android.speech.tts.TextToSpeech
import java.util.Locale

import com.example.ui.viewmodel.ScheduleViewModel
import com.example.data.local.UserPreferencesManager

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    scheduleViewModel: ScheduleViewModel,
    viewModel: ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    
    val prefs = remember { UserPreferencesManager.getInstance(context) }
    val apiKey by prefs.geminiApiKey.collectAsStateWithLifecycle()
    val aiBaseUrl by prefs.aiBaseUrl.collectAsStateWithLifecycle()
    val aiModelName by prefs.aiModelName.collectAsStateWithLifecycle()
    val scheduleState by scheduleViewModel.uiState.collectAsStateWithLifecycle()

    val tts = remember {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.getDefault()
            }
        }
        ttsInstance
    }

    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val contextData = "Courses: ${scheduleState.courses.joinToString { it.name }}. Tasks: ${scheduleState.tasks.joinToString { it.title }}"
            startListening(speechRecognizer, viewModel, apiKey ?: "", contextData, aiBaseUrl ?: "", aiModelName ?: "")
        } else {
            Toast.makeText(context, "Microphone permission is required for voice chat.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(messages) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            
            // Speak the last message if it's from the model and just finished generating
            val lastMessage = messages.last()
            if (!lastMessage.isUser && !lastMessage.isGenerating && !lastMessage.isError && lastMessage.text.isNotEmpty() && !lastMessage.hasBeenSpoken) {
                tts?.speak(lastMessage.text, TextToSpeech.QUEUE_FLUSH, null, lastMessage.id)
                viewModel.markAsSpoken(lastMessage.id)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (onNavigateBack != null) {
            TopAppBar(
                title = { Text("AI Chat Assistant".tr, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back".tr)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { message ->
                ChatBubble(
                    text = message.text,
                    isUser = message.isUser,
                    isGenerating = message.isGenerating,
                    isError = message.isError
                )
            }
        }

        Surface(
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Ask Gemini...".tr) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .testTag("chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )

                if (inputText.isBlank()) {
                    FloatingActionButton(
                        onClick = {
                            if (isRecording) {
                                speechRecognizer?.stopListening()
                                viewModel.setRecording(false)
                            } else {
                                requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.testTag("voice_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop Recording" else "Start Voice Input",
                            tint = if (isRecording) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    FloatingActionButton(
                        onClick = {
                            val contextData = "Courses: ${scheduleState.courses.joinToString { it.name }}. Tasks: ${scheduleState.tasks.joinToString { it.title }}"
                            viewModel.sendMessage(inputText, apiKey ?: "", contextData, aiBaseUrl ?: "", aiModelName ?: "")
                            inputText = ""
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("send_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message".tr,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun startListening(speechRecognizer: SpeechRecognizer?, viewModel: ChatViewModel, apiKey: String, contextData: String, aiBaseUrl: String, aiModelName: String) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }

    speechRecognizer?.setRecognitionListener(object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) { viewModel.setRecording(true) }
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() { viewModel.setRecording(false) }
        override fun onError(error: Int) { viewModel.setRecording(false) }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                viewModel.sendMessage(matches[0], apiKey, contextData, aiBaseUrl, aiModelName)
            }
            viewModel.setRecording(false)
        }

        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    })
    
    speechRecognizer?.startListening(intent)
}

@Composable
fun ChatBubble(
    text: String,
    isUser: Boolean,
    isGenerating: Boolean,
    isError: Boolean
) {
    val backgroundColor = when {
        isUser -> MaterialTheme.colorScheme.primaryContainer
        isError -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when {
        isUser -> MaterialTheme.colorScheme.onPrimaryContainer
        isError -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 20.dp
                    )
                )
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            if (isGenerating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini is thinking...".tr, color = contentColor, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Text(
                    text = text,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
