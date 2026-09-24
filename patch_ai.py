with open('app/src/main/java/com/example/domain/ai/AiServiceProvider.kt', 'r') as f:
    content = f.read()

# Instead of blindly patching, let's just make sure both Gemini and Groq implement try/catch.
content = content.replace("override suspend fun generateResponse(messages: List<ChatMessage>, onUpdate: (String) -> Unit)", "override suspend fun generateResponse(messages: List<ChatMessage>, onUpdate: (String) -> Unit) {\n        try {\n            generateResponseInternal(messages, onUpdate)\n        } catch (e: Exception) {\n            onUpdate(\"**Error:** Connection failed. [${e.message}]\")\n        }\n    }\n    suspend fun generateResponseInternal(messages: List<ChatMessage>, onUpdate: (String) -> Unit)")

with open('app/src/main/java/com/example/domain/ai/AiServiceProvider.kt', 'w') as f:
    f.write(content)
