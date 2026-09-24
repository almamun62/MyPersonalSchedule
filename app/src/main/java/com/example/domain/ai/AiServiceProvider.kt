package com.example.domain.ai

import com.example.data.api.RetrofitClient
import com.example.data.api.OpenAIChatRequest
import com.example.data.api.OpenAIMessage
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Content
import com.example.data.api.Part
import com.example.ui.viewmodel.ChatMessage

/**
 * Interface defining a contract for different LLM providers (e.g., Gemini natively vs OpenAI-compatible).
 */
interface AiServiceProvider {
    suspend fun generateResponse(
        apiKey: String,
        baseUrl: String,
        modelName: String,
        systemInstruction: String,
        history: List<ChatMessage>
    ): String
}

class LegacyGeminiProvider : AiServiceProvider {
    override suspend fun generateResponse(
        apiKey: String,
        baseUrl: String,
        modelName: String,
        systemInstruction: String,
        history: List<ChatMessage>
    ): String {
        val contents = history.map { msg ->
            Content(
                role = if (msg.isUser) "user" else "model",
                parts = listOf(Part(text = msg.text))
            )
        }

        val dynamicSystemInstruction = Content(
            role = "system",
            parts = listOf(Part(text = systemInstruction))
        )

        val request = GenerateContentRequest(
            contents = contents,
            systemInstruction = dynamicSystemInstruction
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        return response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
            ?: "I'm sorry, I couldn't generate a response."
    }
}

class OpenAICompatibleProvider : AiServiceProvider {
    override suspend fun generateResponse(
        apiKey: String,
        baseUrl: String,
        modelName: String,
        systemInstruction: String,
        history: List<ChatMessage>
    ): String {
        val messages = mutableListOf<OpenAIMessage>()
        
        // Add system message
        messages.add(OpenAIMessage(
            role = "system",
            content = systemInstruction
        ))
        
        // Add history
        history.forEach { msg ->
            messages.add(OpenAIMessage(
                role = if (msg.isUser) "user" else "assistant",
                content = msg.text
            ))
        }
        
        val request = OpenAIChatRequest(
            model = modelName,
            messages = messages
        )
        
        // Construct final URL ending in chat/completions
        val finalUrl = if (baseUrl.endsWith("/")) "${baseUrl}chat/completions" else "$baseUrl/chat/completions"
        val authHeader = "Bearer $apiKey"
        
        val response = RetrofitClient.openAiService.generateContent(finalUrl, authHeader, request)
        return response.choices?.firstOrNull()?.message?.content 
            ?: "I'm sorry, I couldn't generate a response."
    }
}

object AiServiceProviderFactory {
    fun getProvider(baseUrl: String): AiServiceProvider {
        return if (baseUrl.contains("generativelanguage.googleapis.com") && !baseUrl.endsWith("openai/") && !baseUrl.contains("openai")) {
            LegacyGeminiProvider()
        } else {
            OpenAICompatibleProvider()
        }
    }
}
