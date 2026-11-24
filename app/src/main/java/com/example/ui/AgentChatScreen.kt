package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.signOut
import com.example.data.AiRepository
import com.example.data.ChatRepository
import com.example.model.AgentMode
import com.example.model.ChatMessage
import com.example.model.StarterPrompt
import com.example.ui.components.AgentModeSelectorSheet
import com.example.ui.components.AgentThinkingPill
import com.example.ui.components.AmbientBackdrop
import com.example.ui.components.FloatingComposer
import com.example.ui.components.FollowUpSuggestionsRow
import com.example.ui.components.MarkdownMessageContent
import com.example.ui.components.MessageActionRow
import com.example.ui.components.SidebarContent
import com.example.ui.components.parseAiMetadata
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorContainerDark
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.voice.VoiceController
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AgentChatScreen(
    currentUserId: String,
    displayName: String,
    email: String?,
    photoUrl: String?,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val viewModel: ChatViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                ChatViewModel(
                    chatRepository = ChatRepository(db),
                    aiRepository = AiRepository(),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val voiceController = remember(context) { VoiceController(context) }
    DisposableEffect(voiceController) {
        onDispose { voiceController.release() }
    }

    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val activeConversationId by viewModel.activeConversationId.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val hinglishPreferred by viewModel.hinglishPreferred.collectAsStateWithLifecycle()
    val inputText by viewModel.inputText.collectAsStateWithLifecycle()
    val pendingAttachments by viewModel.pendingAttachments.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val isListening by voiceController.isListening.collectAsStateWithLifecycle()
    val partialTranscript by voiceController.partialTranscript.collectAsStateWithLifecycle()
    val speakingMessageId by voiceController.speakingMessageId.collectAsStateWithLifecycle()
    val voiceError by voiceController.voiceError.collectAsStateWithLifecycle()

    var showModeSheet by remember { mutableStateOf(false) }

    // Photo Picker (Zero-permission)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = decodeSampledBitmap(context, uri)
            val name = queryDisplayName(context, uri) ?: "photo.jpg"
            if (bmp != null) {
                viewModel.addImageAttachment(name, bmp)
            }
        }
    }

    // Document Picker (Text / CSV / Code)
    val docPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val name = queryDisplayName(context, uri) ?: "document.txt"
            val text = readTextFromUri(context, uri)
            if (text != null) {
                viewModel.addTextAttachment(name, text)
            }
        }
    }

    // Runtime RECORD_AUDIO Permission for Voice Input
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceController.startListening(hinglishPreferred) { spoken ->
                viewModel.appendVoiceTranscript(spoken)
            }
        }
    }

    val handleToggleVoice: () -> Unit = {
        if (isListening) {
            voiceController.stopListening()
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                voiceController.startListening(hinglishPreferred) { spoken ->
                    viewModel.appendVoiceTranscript(spoken)
                }
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val conversations = (conversationsState as? ConversationsUiState.Success)?.items.orEmpty()
    val activeConv = conversations.firstOrNull { it.id == activeConversationId }

    val handleSignOut: () -> Unit = {
        voiceController.release()
        signOut(credentialManager, scope, onSignOutComplete = onSignedOut)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val isExpandedScreen = maxWidth >= 840.dp
        var expandedSidebarVisible by remember { mutableStateOf(true) }
        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

        BackHandler(enabled = !isExpandedScreen && drawerState.isOpen) {
            scope.launch { drawerState.close() }
        }

        val chatMainContent: @Composable () -> Unit = {
            Box(modifier = Modifier.fillMaxSize()) {
                AmbientBackdrop(
                    modifier = Modifier.fillMaxSize(),
                    accentOverride = selectedMode.accentColor
                )

                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Frosted Header Bar
                    ChatTopBar(
                        title = activeConv?.title ?: selectedMode.displayName,
                        selectedMode = selectedMode,
                        hinglishPreferred = hinglishPreferred,
                        onToggleSidebar = {
                            if (isExpandedScreen) {
                                expandedSidebarVisible = !expandedSidebarVisible
                            } else {
                                scope.launch {
                                    if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                }
                            }
                        },
                        onToggleHinglish = { viewModel.toggleHinglishPreference() },
                        onOpenModeSelector = { showModeSheet = true },
                        onNewChat = { viewModel.startNewChat() }
                    )

                    // Dismissible Error Banner (Firestore / AI / Voice)
                    val activeError = errorMessage ?: voiceError
                    AnimatedVisibility(visible = activeError != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ErrorContainerDark,
                            border = BorderStroke(1.dp, ErrorRose.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = activeError.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFFD9DF),
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.clearError()
                                        voiceController.clearError()
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Dismiss error",
                                        tint = Color(0xFFFFD9DF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Main Scrollable Chat or Staggered Empty State
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (messages.isEmpty() && !generationState.isGenerating) {
                            EmptyGreetingState(
                                displayName = displayName,
                                selectedMode = selectedMode,
                                onStarterClick = { starter ->
                                    viewModel.sendPrompt(starter.promptText)
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 760.dp)
                            )
                        } else {
                            val listState = rememberLazyListState()
                            LaunchedEffect(messages.size, generationState.streamingText.length) {
                                val totalItems = messages.size + if (generationState.isGenerating) 1 else 0
                                if (totalItems > 0) {
                                    listState.animateScrollToItem(totalItems - 1)
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .widthIn(max = 760.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                itemsIndexed(
                                    items = messages,
                                    key = { _, msg -> msg.id.ifBlank { msg.hashCode().toString() } }
                                ) { index, msg ->
                                    val isLastModelMsg = !msg.isUserMessage() && index == messages.lastIndex && !generationState.isGenerating
                                    if (msg.isUserMessage()) {
                                        UserMessageBubble(message = msg)
                                    } else {
                                        AiMessageBubble(
                                            message = msg,
                                            isLatest = isLastModelMsg,
                                            isSpeaking = speakingMessageId == msg.id,
                                            onRegenerate = { viewModel.regenerateResponse(msg) },
                                            onToggleReadAloud = {
                                                voiceController.toggleSpeak(
                                                    messageId = msg.id,
                                                    rawMarkdownText = msg.text,
                                                    hinglishPreferred = hinglishPreferred
                                                )
                                            },
                                            onFollowUpClick = { prompt ->
                                                viewModel.sendPrompt(prompt)
                                            }
                                        )
                                    }
                                }

                                if (generationState.isGenerating) {
                                    item(key = "streaming_turn") {
                                        StreamingAiBubble(state = generationState)
                                    }
                                }
                            }
                        }
                    }

                    // Floating Bottom Composer
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingComposer(
                            inputText = inputText,
                            onInputTextChanged = viewModel::updateInputText,
                            selectedMode = selectedMode,
                            pendingAttachments = pendingAttachments,
                            isGenerating = generationState.isGenerating,
                            isListening = isListening,
                            partialTranscript = partialTranscript,
                            onOpenModeSelector = { showModeSheet = true },
                            onPickImage = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onPickDocument = {
                                docPickerLauncher.launch(arrayOf("text/*", "application/json", "application/csv"))
                            },
                            onRemoveAttachment = viewModel::removeAttachment,
                            onToggleVoiceInput = handleToggleVoice,
                            onSend = { viewModel.sendPrompt() },
                            onStopGenerating = { viewModel.stopGenerating(persistPartial = true) },
                            modifier = Modifier.widthIn(max = 760.dp)
                        )
                    }
                }
            }
        }

        if (isExpandedScreen) {
            Row(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(visible = expandedSidebarVisible) {
                    Row(modifier = Modifier.fillMaxHeight()) {
                        SidebarContent(
                            conversations = conversations,
                            activeConversationId = activeConversationId,
                            displayName = displayName,
                            email = email,
                            photoUrl = photoUrl,
                            onNewChat = { viewModel.startNewChat() },
                            onSelectConversation = { id ->
                                conversations.firstOrNull { it.id == id }?.let {
                                    viewModel.selectConversation(it)
                                }
                            },
                            onDeleteConversation = viewModel::deleteConversation,
                            onSignOut = handleSignOut,
                            modifier = Modifier.width(290.dp)
                        )
                        VerticalDivider(color = ObsidianBorder, thickness = 1.dp)
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    chatMainContent()
                }
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerContainerColor = ObsidianSurface,
                        modifier = Modifier.width(300.dp)
                    ) {
                        SidebarContent(
                            conversations = conversations,
                            activeConversationId = activeConversationId,
                            displayName = displayName,
                            email = email,
                            photoUrl = photoUrl,
                            onNewChat = {
                                viewModel.startNewChat()
                                scope.launch { drawerState.close() }
                            },
                            onSelectConversation = { id ->
                                conversations.firstOrNull { it.id == id }?.let {
                                    viewModel.selectConversation(it)
                                }
                                scope.launch { drawerState.close() }
                            },
                            onDeleteConversation = viewModel::deleteConversation,
                            onSignOut = handleSignOut
                        )
                    }
                },
                content = chatMainContent
            )
        }
    }

    if (showModeSheet) {
        AgentModeSelectorSheet(
            selectedMode = selectedMode,
            onSelectMode = viewModel::selectAgentMode,
            onDismiss = { showModeSheet = false }
        )
    }
}

@Composable
private fun ChatTopBar(
    title: String,
    selectedMode: AgentMode,
    hinglishPreferred: Boolean,
    onToggleSidebar: () -> Unit,
    onToggleHinglish: () -> Unit,
    onOpenModeSelector: () -> Unit,
    onNewChat: () -> Unit
) {
    Surface(
        color = ObsidianGlass,
        border = BorderStroke(0.5.dp, ObsidianBorder.copy(alpha = 0.7f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onToggleSidebar,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("sidebar_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Menu,
                        contentDescription = "Toggle chat history sidebar",
                        tint = TextPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onOpenModeSelector() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(selectedMode.accentColor)
                        )
                        Text(
                            text = selectedMode.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = selectedMode.accentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hinglishPreferred) EmeraldPrimary.copy(alpha = 0.2f) else ObsidianSurfaceVariant,
                    border = BorderStroke(
                        1.dp,
                        if (hinglishPreferred) EmeraldPrimary.copy(alpha = 0.6f) else ObsidianBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleHinglish() }
                        .testTag("hinglish_toggle_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Translate,
                            contentDescription = "Toggle Hinglish mode",
                            tint = if (hinglishPreferred) EmeraldPrimary else TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (hinglishPreferred) "Hinglish" else "EN/हिं",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (hinglishPreferred) TextPrimary else TextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = onNewChat,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("header_new_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Start new chat",
                        tint = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyGreetingState(
    displayName: String,
    selectedMode: AgentMode,
    onStarterClick: (StarterPrompt) -> Unit,
    modifier: Modifier = Modifier
) {
    var headerVisible by remember { mutableStateOf(false) }
    var cardsVisible by remember(selectedMode) { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        headerVisible = true
    }

    LaunchedEffect(selectedMode) {
        cardsVisible = false
        delay(80L)
        cardsVisible = true
    }

    val firstName = displayName.substringBefore(" ").ifBlank { "there" }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn(tween(420)) + slideInVertically(
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            ) { 24 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = ObsidianElevated,
                    border = BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(selectedMode.accentColor.copy(alpha = 0.65f), CyanAccent.copy(alpha = 0.35f))
                        )
                    ),
                    modifier = Modifier.size(64.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_aether_emblem_1795509697785),
                        contentDescription = "Aether AI",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Hi $firstName, how can I help you today?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = selectedMode.accentColor.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, selectedMode.accentColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = selectedMode.icon,
                            contentDescription = null,
                            tint = selectedMode.accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${selectedMode.displayName} • ${selectedMode.shortDescription}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4 Interactive Starter Prompt Cards with Staggered Delay
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            selectedMode.starterPrompts.chunked(2).forEachIndexed { rowIdx, pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEachIndexed { colIdx, starter ->
                        val cardIdx = rowIdx * 2 + colIdx
                        Box(modifier = Modifier.weight(1f)) {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = cardsVisible,
                                enter = fadeIn(tween(320, delayMillis = cardIdx * 85)) +
                                    slideInVertically(
                                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                    ) { 28 }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = ObsidianGlass,
                                    border = BorderStroke(1.dp, ObsidianBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .clickable { onStarterClick(starter) }
                                        .testTag("starter_card_$cardIdx")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            text = starter.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = starter.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserMessageBubble(message: ChatMessage) {
    var visible by remember(message.id) { mutableStateOf(false) }
    LaunchedEffect(message.id) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)) + slideInVertically(
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ) { 20 }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
                color = ObsidianElevated,
                border = BorderStroke(1.dp, ObsidianBorder),
                modifier = Modifier.widthIn(max = 520.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!message.attachmentLabel.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = ObsidianSurfaceVariant,
                            border = BorderStroke(0.5.dp, ObsidianBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = "Attachment",
                                    tint = CyanAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = message.attachmentLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyanAccent
                                )
                            }
                        }
                    }
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: ChatMessage,
    isLatest: Boolean,
    isSpeaking: Boolean,
    onRegenerate: () -> Unit,
    onToggleReadAloud: () -> Unit,
    onFollowUpClick: (String) -> Unit
) {
    val mode = message.resolvedMode()
    val parsed = remember(message.text) { parseAiMetadata(message.text, isStreaming = false) }
    val followUps = message.followUpSuggestions.ifEmpty { parsed.followUpSuggestions }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = mode.accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, mode.accentColor.copy(alpha = 0.45f)),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = mode.icon,
                        contentDescription = mode.displayName,
                        tint = mode.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "Aether • ${mode.displayName}",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }

        if (message.thinkingSteps.isNotEmpty()) {
            AgentThinkingPill(
                steps = message.thinkingSteps,
                isActive = false,
                accentColor = mode.accentColor
            )
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = ObsidianGlass,
            border = BorderStroke(1.dp, ObsidianBorder.copy(alpha = 0.85f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                MarkdownMessageContent(
                    text = parsed.cleanDisplayText,
                    isStreaming = false,
                    accentColor = mode.accentColor
                )
            }
        }

        MessageActionRow(
            messageText = parsed.cleanDisplayText,
            isSpeaking = isSpeaking,
            onRegenerate = onRegenerate,
            onToggleReadAloud = onToggleReadAloud
        )

        if (isLatest && followUps.isNotEmpty()) {
            FollowUpSuggestionsRow(
                suggestions = followUps,
                accentColor = mode.accentColor,
                onSuggestionClick = onFollowUpClick
            )
        }
    }
}

@Composable
private fun StreamingAiBubble(state: ActiveGenerationState) {
    val parsed = remember(state.streamingText) {
        parseAiMetadata(state.streamingText, isStreaming = true)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = state.mode.accentColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, state.mode.accentColor.copy(alpha = 0.55f)),
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = state.mode.icon,
                        contentDescription = state.mode.displayName,
                        tint = state.mode.accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = "Aether • ${state.mode.displayName}",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }

        AgentThinkingPill(
            steps = state.thinkingSteps,
            isActive = state.streamingText.isEmpty(),
            accentColor = state.mode.accentColor
        )

        if (parsed.cleanDisplayText.isNotEmpty() || state.streamingText.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = ObsidianGlass,
                border = BorderStroke(1.dp, state.mode.accentColor.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    MarkdownMessageContent(
                        text = parsed.cleanDisplayText,
                        isStreaming = true,
                        accentColor = state.mode.accentColor
                    )
                }
            }
        }
    }
}

private fun decodeSampledBitmap(context: Context, uri: Uri, maxDim: Int = 1280): Bitmap? {
    return try {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > maxDim || boundsOpts.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    } catch (_: Exception) {
        null
    }
}

private fun readTextFromUri(context: Context, uri: Uri, maxChars: Int = 12_000): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.readText().take(maxChars)
        }
    } catch (_: Exception) {
        null
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) cursor.getString(idx) else null
            } else null
        }
    } catch (_: Exception) {
        null
    }
}
