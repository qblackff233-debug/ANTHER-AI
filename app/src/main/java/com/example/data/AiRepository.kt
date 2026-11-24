package com.example.data

import android.graphics.Bitmap
import android.util.Log
import com.example.model.AgentMode
import com.example.model.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "AiRepository"
private const val STREAM_TIMEOUT_MS = 60_000L
private const val MODEL_NAME = "gemini-3.8-flash"

class AiRepository {
    private val modelCache = mutableMapOf<Pair<AgentMode, Boolean>, GenerativeModel>()

    private fun getOrCreateModel(mode: AgentMode, hinglishPreferred: Boolean): GenerativeModel {
        val key = mode to hinglishPreferred
        return modelCache.getOrPut(key) {
            Firebase.ai.generativeModel(
                modelName = MODEL_NAME,
                systemInstruction = content {
                    text(mode.buildSystemInstruction(hinglishPreferred))
                }
            )
        }
    }

    fun streamReply(
        history: List<ChatMessage>,
        promptText: String,
        attachedImages: List<Bitmap>,
        attachedFileContext: String?,
        mode: AgentMode,
        hinglishPreferred: Boolean
    ): Flow<String> = flow {
        val model = getOrCreateModel(mode, hinglishPreferred)

        // Build clean multi-turn Gemini chat history (excluding empty or current turn)
        val sdkHistory: List<Content> = history
            .filter { it.text.isNotBlank() }
            .takeLast(14)
            .map { msg ->
                val sdkRole = if (msg.isUserMessage()) "user" else "model"
                content(role = sdkRole) {
                    text(msg.text)
                }
            }

        val chat = model.startChat(history = sdkHistory)

        val userContent = content("user") {
            attachedImages.forEach { bmp ->
                image(scaleBitmapIfNeeded(bmp))
            }
            val fullPrompt = buildString {
                if (!attachedFileContext.isNullOrBlank()) {
                    appendLine("Attached file content:")
                    appendLine(attachedFileContext.take(12_000))
                    appendLine()
                }
                append(promptText)
            }
            text(fullPrompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(STREAM_TIMEOUT_MS) {
                chat.sendMessageStream(userContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini streaming failed", e)
            throw e
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int = 1280): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val scale = maxDimension.toFloat() / maxOf(width, height).toFloat()
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }
}
