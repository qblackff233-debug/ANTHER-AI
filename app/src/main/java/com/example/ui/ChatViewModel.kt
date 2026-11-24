package com.example.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiRepository
import com.example.data.ChatRepository
import com.example.model.AgentMode
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.PendingAttachment
import com.example.ui.components.parseAiMetadata
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

private const val TAG = "ChatViewModel"

sealed interface ConversationsUiState {
    data object Loading : ConversationsUiState
    data class Success(val items: List<Conversation>) : ConversationsUiState
    data class Error(val message: String) : ConversationsUiState
}

data class ActiveGenerationState(
    val isGenerating: Boolean = false,
    val thinkingSteps: List<String> = emptyList(),
    val streamingText: String = "",
    val mode: AgentMode = AgentMode.GENERAL_ASSISTANT
)

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val aiRepository: AiRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    private val _selectedMode = MutableStateFlow(AgentMode.GENERAL_ASSISTANT)
    val selectedMode: StateFlow<AgentMode> = _selectedMode.asStateFlow()

    private val _hinglishPreferred = MutableStateFlow(false)
    val hinglishPreferred: StateFlow<Boolean> = _hinglishPreferred.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _pendingAttachments = MutableStateFlow<List<PendingAttachment>>(emptyList())
    val pendingAttachments: StateFlow<List<PendingAttachment>> = _pendingAttachments.asStateFlow()

    private val _generationState = MutableStateFlow(ActiveGenerationState())
    val generationState: StateFlow<ActiveGenerationState> = _generationState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var generationJob: Job? = null
    private var preferencesHydrated = false

    val conversationsState: StateFlow<ConversationsUiState> =
        chatRepository.observeConversations(currentUserId)
            .map<List<Conversation>, ConversationsUiState> { ConversationsUiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing conversations", rootError)
                emit(ConversationsUiState.Error(rootError.localizedMessage ?: "Failed to load conversations"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = ConversationsUiState.Loading
            )

    val messages: StateFlow<List<ChatMessage>> =
        _activeConversationId
            .flatMapLatest { convId ->
                if (convId == null) {
                    flowOf(emptyList())
                } else {
                    chatRepository.observeMessages(currentUserId, convId)
                        .catch { error ->
                            val firestoreError = generateSequence(error) { it.cause }
                                .filterIsInstance<FirebaseFirestoreException>()
                                .firstOrNull()
                            if (firestoreError == null && error is CancellationException) throw error
                            val rootError = firestoreError ?: error
                            Log.w(TAG, "Error observing messages", rootError)
                            _errorMessage.value = rootError.localizedMessage ?: "Failed to load messages"
                            emit(emptyList())
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { user ->
                chatRepository.ensureUserProfile(user)
            }
        }
        viewModelScope.launch {
            chatRepository.observeUserProfile(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    Log.w(TAG, "Profile observation error", firestoreError ?: error)
                }
                .collect { profile ->
                    if (profile != null && !preferencesHydrated) {
                        preferencesHydrated = true
                        _selectedMode.value = profile.resolvedMode()
                        _hinglishPreferred.value = profile.hinglishPreferred
                    }
                }
        }
    }

    fun updateInputText(newText: String) {
        _inputText.value = newText
    }

    fun appendVoiceTranscript(spoken: String) {
        if (spoken.isBlank()) return
        val current = _inputText.value.trimEnd()
        _inputText.value = if (current.isEmpty()) spoken else "$current $spoken"
    }

    fun selectAgentMode(mode: AgentMode) {
        _selectedMode.value = mode
        viewModelScope.launch {
            chatRepository.updateUserPreferences(
                userId = currentUserId,
                preferredMode = mode,
                hinglishPreferred = _hinglishPreferred.value
            )
        }
    }

    fun toggleHinglishPreference() {
        val next = !_hinglishPreferred.value
        _hinglishPreferred.value = next
        viewModelScope.launch {
            chatRepository.updateUserPreferences(
                userId = currentUserId,
                preferredMode = _selectedMode.value,
                hinglishPreferred = next
            )
        }
    }

    fun addImageAttachment(displayName: String, bitmap: Bitmap) {
        val att = PendingAttachment(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            isImage = true,
            bitmap = bitmap
        )
        _pendingAttachments.update { (it + att).take(4) }
    }

    fun addTextAttachment(displayName: String, textContent: String) {
        val att = PendingAttachment(
            id = UUID.randomUUID().toString(),
            displayName = displayName,
            isImage = false,
            textContent = textContent
        )
        _pendingAttachments.update { (it + att).take(4) }
    }

    fun removeAttachment(id: String) {
        _pendingAttachments.update { list -> list.filterNot { it.id == id } }
    }

    fun startNewChat() {
        stopGenerating(persistPartial = false)
        _activeConversationId.value = null
        _errorMessage.value = null
    }

    fun selectConversation(conversation: Conversation) {
        stopGenerating(persistPartial = false)
        _activeConversationId.value = conversation.id
        _selectedMode.value = conversation.resolvedMode()
        _errorMessage.value = null
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            if (_activeConversationId.value == conversationId) {
                stopGenerating(persistPartial = false)
                _activeConversationId.value = null
            }
            chatRepository.deleteConversation(currentUserId, conversationId)
                .onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "Failed to delete conversation"
                }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun sendPrompt(overridePrompt: String? = null) {
        if (_generationState.value.isGenerating) return

        val rawText = (overridePrompt ?: _inputText.value).trim()
        val attachments = _pendingAttachments.value
        if (rawText.isBlank() && attachments.isEmpty()) return

        val effectivePrompt = rawText.ifBlank { "Please analyze the attached content." }
        val attachmentLabel = attachments.takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { it.displayName }
            ?.take(200)
        val images = attachments.mapNotNull { it.bitmap }
        val fileContext = attachments
            .mapNotNull { att ->
                att.textContent?.let { "--- ${att.displayName} ---\n$it" }
            }
            .joinToString("\n\n")
            .takeIf { it.isNotBlank() }

        if (overridePrompt == null) {
            _inputText.value = ""
        }
        _pendingAttachments.value = emptyList()
        _errorMessage.value = null

        val mode = _selectedMode.value
        val hinglish = _hinglishPreferred.value
        val priorHistory = messages.value
        val existingConvId = _activeConversationId.value

        generationJob = viewModelScope.launch {
            var targetConvId = existingConvId
            val userMsgCountAfterAdd: Long

            if (targetConvId == null) {
                val title = effectivePrompt.replace('\n', ' ').trim().take(48).ifBlank { "New Chat" }
                val createRes = chatRepository.createConversationWithFirstMessage(
                    userId = currentUserId,
                    title = title,
                    agentMode = mode,
                    userText = effectivePrompt,
                    attachmentLabel = attachmentLabel
                )
                if (createRes.isFailure) {
                    _errorMessage.value = createRes.exceptionOrNull()?.localizedMessage
                        ?: "Could not start conversation."
                    return@launch
                }
                targetConvId = createRes.getOrThrow()
                _activeConversationId.value = targetConvId
                userMsgCountAfterAdd = 1L
            } else {
                userMsgCountAfterAdd = (priorHistory.size + 1).toLong()
                val addRes = chatRepository.addMessageAndTouchConversation(
                    userId = currentUserId,
                    conversationId = targetConvId,
                    role = "user",
                    text = effectivePrompt,
                    agentMode = mode,
                    attachmentLabel = attachmentLabel,
                    newMessageCount = userMsgCountAfterAdd
                )
                if (addRes.isFailure) {
                    _errorMessage.value = addRes.exceptionOrNull()?.localizedMessage
                        ?: "Could not save message."
                    return@launch
                }
            }

            runStreamingResponse(
                conversationId = targetConvId,
                history = priorHistory,
                promptText = effectivePrompt,
                images = images,
                fileContext = fileContext,
                mode = mode,
                hinglish = hinglish,
                expectedTotalCount = userMsgCountAfterAdd + 1L,
                replaceMessageId = null
            )
        }
    }

    fun regenerateResponse(modelMessage: ChatMessage) {
        if (_generationState.value.isGenerating) return
        val convId = _activeConversationId.value ?: return
        val list = messages.value
        val idx = list.indexOfFirst { it.id == modelMessage.id }
        if (idx <= 0) return

        val precedingUser = list.subList(0, idx).lastOrNull { it.isUserMessage() } ?: return
        val priorHistory = list.subList(0, list.indexOf(precedingUser))
        val mode = _selectedMode.value
        val hinglish = _hinglishPreferred.value

        _errorMessage.value = null
        generationJob = viewModelScope.launch {
            runStreamingResponse(
                conversationId = convId,
                history = priorHistory,
                promptText = precedingUser.text,
                images = emptyList(),
                fileContext = null,
                mode = mode,
                hinglish = hinglish,
                expectedTotalCount = list.size.toLong(),
                replaceMessageId = modelMessage.id
            )
        }
    }

    fun stopGenerating(persistPartial: Boolean = true) {
        val currentState = _generationState.value
        if (!currentState.isGenerating) return
        val convId = _activeConversationId.value
        generationJob?.cancel()
        generationJob = null

        val partial = parseAiMetadata(currentState.streamingText, isStreaming = false).cleanDisplayText
        _generationState.value = ActiveGenerationState()

        if (persistPartial && convId != null && partial.isNotBlank()) {
            viewModelScope.launch {
                chatRepository.addMessageAndTouchConversation(
                    userId = currentUserId,
                    conversationId = convId,
                    role = "model",
                    text = "$partial\n\n*(Stopped)*",
                    agentMode = currentState.mode,
                    thinkingSteps = currentState.thinkingSteps,
                    followUpSuggestions = emptyList(),
                    newMessageCount = (messages.value.size + 1).toLong()
                )
            }
        }
    }

    private suspend fun runStreamingResponse(
        conversationId: String,
        history: List<ChatMessage>,
        promptText: String,
        images: List<Bitmap>,
        fileContext: String?,
        mode: AgentMode,
        hinglish: Boolean,
        expectedTotalCount: Long,
        replaceMessageId: String?
    ) {
        val phases = mode.thinkingPhases
        _generationState.value = ActiveGenerationState(
            isGenerating = true,
            thinkingSteps = listOf(phases.first()),
            streamingText = "",
            mode = mode
        )

        // Advance thinking pill phases smoothly until first tokens arrive
        val thinkingAnimator = viewModelScope.launch {
            for (i in 1 until phases.size) {
                delay(420L)
                _generationState.update { state ->
                    if (state.isGenerating && state.streamingText.isEmpty()) {
                        state.copy(thinkingSteps = phases.take(i + 1))
                    } else state
                }
            }
        }

        var latestRawText = ""
        try {
            aiRepository.streamReply(
                history = history,
                promptText = promptText,
                attachedImages = images,
                attachedFileContext = fileContext,
                mode = mode,
                hinglishPreferred = hinglish
            ).collect { accumulated ->
                latestRawText = accumulated
                _generationState.update { state ->
                    state.copy(
                        thinkingSteps = phases,
                        streamingText = accumulated
                    )
                }
            }

            thinkingAnimator.cancel()

            if (latestRawText.isBlank()) {
                _errorMessage.value = "The AI returned an empty response. Please try again."
                _generationState.value = ActiveGenerationState()
                return
            }

            val parsed = parseAiMetadata(latestRawText, isStreaming = false)
            val finalText = parsed.cleanDisplayText.ifBlank { latestRawText.trim() }

            if (replaceMessageId != null) {
                chatRepository.updateModelMessage(
                    userId = currentUserId,
                    conversationId = conversationId,
                    messageId = replaceMessageId,
                    newText = finalText,
                    agentMode = mode,
                    thinkingSteps = phases,
                    followUpSuggestions = parsed.followUpSuggestions
                ).onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "Failed to save regenerated reply."
                }
            } else {
                chatRepository.addMessageAndTouchConversation(
                    userId = currentUserId,
                    conversationId = conversationId,
                    role = "model",
                    text = finalText,
                    agentMode = mode,
                    thinkingSteps = phases,
                    followUpSuggestions = parsed.followUpSuggestions,
                    newMessageCount = expectedTotalCount
                ).onFailure { err ->
                    _errorMessage.value = err.localizedMessage ?: "Failed to save AI reply."
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI generation timed out", e)
            _errorMessage.value = "Response timed out after 60 seconds. Please try again."
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI generation failed", e)
            _errorMessage.value = e.localizedMessage ?: "AI generation failed. Check network or App Check."
        } finally {
            thinkingAnimator.cancel()
            _generationState.value = ActiveGenerationState()
        }
    }
}
