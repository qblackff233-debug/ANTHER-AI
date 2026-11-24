package com.example.model

import android.graphics.Bitmap
import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val photoUrl: String? = null,
    val preferredMode: String = AgentMode.GENERAL_ASSISTANT.name,
    val hinglishPreferred: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedMode(): AgentMode = AgentMode.fromName(preferredMode)

    fun toCreateMap(): Map<String, Any> {
        val raw = mutableMapOf<String, Any?>(
            "userId" to userId,
            "displayName" to displayName.take(100),
            "email" to email?.take(150),
            "photoUrl" to photoUrl?.take(500),
            "preferredMode" to preferredMode,
            "hinglishPreferred" to hinglishPreferred,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "New Chat",
    val agentMode: String = AgentMode.GENERAL_ASSISTANT.name,
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedMode(): AgentMode = AgentMode.fromName(agentMode)
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val role: String = "user",
    val text: String = "",
    val agentMode: String = AgentMode.GENERAL_ASSISTANT.name,
    val thinkingSteps: List<String> = emptyList(),
    val followUpSuggestions: List<String> = emptyList(),
    val attachmentLabel: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun resolvedMode(): AgentMode = AgentMode.fromName(agentMode)
    fun isUserMessage(): Boolean = role == "user"
}

data class PendingAttachment(
    val id: String,
    val displayName: String,
    val isImage: Boolean,
    val bitmap: Bitmap? = null,
    val textContent: String? = null
)

data class ChartDataPoint(
    val label: String,
    val value: Double
)

data class ChartSpec(
    val title: String,
    val type: String,
    val unit: String,
    val items: List<ChartDataPoint>
)
