package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val isGenerating: Boolean = false,
    val isError: Boolean = false,
    val hasBeenSpoken: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val systemInstruction = Content(
        role = "system",
        parts = listOf(Part(text = "You are an AI assistant built into 'MySchedule', a completely offline-first, private personal course assistant for university students. Be helpful, concise, and friendly."))
    )

    fun sendMessage(text: String, apiKey: String, appContextData: String = "", baseUrl: String = "https://generativelanguage.googleapis.com/v1beta/openai/", modelName: String = "gemini-1.5-flash") {
        if (text.isBlank()) return

        val userMessage = ChatMessage(
            id = java.util.UUID.randomUUID().toString(),
            text = text,
            isUser = true
        )
        
        val typingId = java.util.UUID.randomUUID().toString()
        val typingMessage = ChatMessage(
            id = typingId,
            text = "",
            isUser = false,
            isGenerating = true
        )

        _messages.value = _messages.value + listOf(userMessage, typingMessage)

        if (apiKey.isBlank()) {
            updateMessageText(typingId, "Please add your AI API Key in Settings.", isGenerating = false, isError = true)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val provider = com.example.domain.ai.AiServiceProviderFactory.getProvider(baseUrl)
                val systemInstruction = "You are an AI assistant built into 'MySchedule', a completely offline-first, private personal course assistant for university students. Be helpful, concise, and friendly. Here is the current context of the user's schedule and tasks:\n$appContextData"
                
                val history = _messages.value
                    .filter { !it.isGenerating && !it.isError }
                    .takeLast(10)
                    
                val responseText = provider.generateResponse(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    modelName = modelName,
                    systemInstruction = systemInstruction,
                    history = history
                )
                
                updateMessageText(typingId, responseText, isGenerating = false)
            } catch (e: Exception) {
                updateMessageText(typingId, "API Error: ${e.localizedMessage}", isGenerating = false, isError = true)
            }
        }
    }

    private fun updateMessageText(id: String, text: String, isGenerating: Boolean = false, isError: Boolean = false) {
        _messages.value = _messages.value.map {
            if (it.id == id) {
                it.copy(text = text, isGenerating = isGenerating, isError = isError)
            } else {
                it
            }
        }
    }

    fun setRecording(isRecording: Boolean) {
        _isRecording.value = isRecording
    }

    fun markAsSpoken(id: String) {
        _messages.value = _messages.value.map {
            if (it.id == id) it.copy(hasBeenSpoken = true) else it
        }
    }
}
