package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AgentMode
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ChatRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid)
        docRef.snapshots()
            .map { snapshot ->
                snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun ensureUserProfile(user: FirebaseUser): Result<Unit> {
        val docPath = "users/${user.uid}"
        return try {
            val uid = requireUserId(user.uid)
            val docRef = db.collection("users").document(uid)
            val existing = docRef.get().await()
            val safeName = (user.displayName?.takeIf { it.isNotBlank() } ?: "Aether User").take(100)
            val safeEmail = user.email?.takeIf { it.isNotBlank() }?.take(150)
            val safePhoto = user.photoUrl?.toString()?.takeIf { it.isNotBlank() }?.take(500)

            if (!existing.exists()) {
                val profile = UserProfile(
                    userId = uid,
                    displayName = safeName,
                    email = safeEmail,
                    photoUrl = safePhoto,
                    preferredMode = AgentMode.GENERAL_ASSISTANT.name,
                    hinglishPreferred = false
                )
                docRef.set(profile.toCreateMap()).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to safeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeEmail != null) updates["email"] = safeEmail
                if (safePhoto != null) updates["photoUrl"] = safePhoto
                docRef.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateUserPreferences(
        userId: String,
        preferredMode: AgentMode,
        hinglishPreferred: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "preferredMode" to preferredMode.name,
                        "hinglishPreferred" to hinglishPreferred,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeConversations(userId: String): Flow<List<Conversation>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/conversations"
        db.collection("users").document(uid)
            .collection("conversations")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    fun observeMessages(userId: String, conversationId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId(userId)
        require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
        val path = "users/$uid/conversations/$conversationId/messages"
        db.collection("users").document(uid)
            .collection("conversations").document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ChatMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun createConversationWithFirstMessage(
        userId: String,
        title: String,
        agentMode: AgentMode,
        userText: String,
        attachmentLabel: String?
    ): Result<String> {
        var docPath = "users/$userId/conversations"
        return try {
            val uid = requireUserId(userId)
            val convRef = db.collection("users").document(uid)
                .collection("conversations").document()
            docPath = convRef.path
            val msgRef = convRef.collection("messages").document()

            val safeTitle = title.trim().ifBlank { "New Chat" }.take(120)
            val safeText = userText.take(32000)
            val preview = safeText.replace('\n', ' ').trim().take(200)

            val convPayload = mapOf(
                "userId" to uid,
                "title" to safeTitle,
                "agentMode" to agentMode.name,
                "lastMessagePreview" to preview,
                "messageCount" to 1L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val msgRaw = mutableMapOf<String, Any?>(
                "userId" to uid,
                "conversationId" to convRef.id,
                "role" to "user",
                "text" to safeText,
                "agentMode" to agentMode.name,
                "thinkingSteps" to emptyList<String>(),
                "followUpSuggestions" to emptyList<String>(),
                "attachmentLabel" to attachmentLabel?.take(200),
                "createdAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            val msgPayload = msgRaw.filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(convRef, convPayload)
            batch.set(msgRef, msgPayload)
            batch.commit().await()

            Result.success(convRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun addMessageAndTouchConversation(
        userId: String,
        conversationId: String,
        role: String,
        text: String,
        agentMode: AgentMode,
        thinkingSteps: List<String> = emptyList(),
        followUpSuggestions: List<String> = emptyList(),
        attachmentLabel: String? = null,
        newMessageCount: Long
    ): Result<String> {
        val docPath = "users/$userId/conversations/$conversationId/messages"
        return try {
            val uid = requireUserId(userId)
            require(conversationId.isNotBlank()) { "Conversation ID must not be blank" }
            require(role == "user" || role == "model") { "Invalid role: $role" }

            val convRef = db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
            val msgRef = convRef.collection("messages").document()

            val safeText = text.take(32000)
            val preview = safeText.replace('\n', ' ').trim().take(200)

            val msgRaw = mutableMapOf<String, Any?>(
                "userId" to uid,
                "conversationId" to conversationId,
                "role" to role,
                "text" to safeText,
                "agentMode" to agentMode.name,
                "thinkingSteps" to thinkingSteps.take(10),
                "followUpSuggestions" to followUpSuggestions.take(6),
                "attachmentLabel" to attachmentLabel?.take(200),
                "createdAt" to FieldValue.serverTimestamp()
            )
            @Suppress("UNCHECKED_CAST")
            val msgPayload = msgRaw.filterValues { it != null } as Map<String, Any>

            val batch = db.batch()
            batch.set(msgRef, msgPayload)
            batch.update(
                convRef,
                mapOf(
                    "agentMode" to agentMode.name,
                    "lastMessagePreview" to preview,
                    "messageCount" to newMessageCount.coerceAtLeast(1L).coerceAtMost(5000L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()

            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateModelMessage(
        userId: String,
        conversationId: String,
        messageId: String,
        newText: String,
        agentMode: AgentMode,
        thinkingSteps: List<String>,
        followUpSuggestions: List<String>
    ): Result<Unit> {
        val docPath = "users/$userId/conversations/$conversationId/messages/$messageId"
        return try {
            val uid = requireUserId(userId)
            require(conversationId.isNotBlank() && messageId.isNotBlank())
            val convRef = db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
            val msgRef = convRef.collection("messages").document(messageId)
            val safeText = newText.take(32000)
            val preview = safeText.replace('\n', ' ').trim().take(200)

            val batch = db.batch()
            batch.update(
                msgRef,
                mapOf(
                    "text" to safeText,
                    "agentMode" to agentMode.name,
                    "thinkingSteps" to thinkingSteps.take(10),
                    "followUpSuggestions" to followUpSuggestions.take(6)
                )
            )
            batch.update(
                convRef,
                mapOf(
                    "agentMode" to agentMode.name,
                    "lastMessagePreview" to preview,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteConversation(userId: String, conversationId: String): Result<Unit> {
        val docPath = "users/$userId/conversations/$conversationId"
        return try {
            val uid = requireUserId(userId)
            require(conversationId.isNotBlank())
            val convRef = db.collection("users").document(uid)
                .collection("conversations").document(conversationId)
            val messagesSnapshot = convRef.collection("messages").get().await()
            val batch = db.batch()
            messagesSnapshot.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.delete(convRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
