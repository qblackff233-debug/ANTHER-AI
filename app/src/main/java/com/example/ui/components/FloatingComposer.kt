package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.AgentMode
import com.example.model.PendingAttachment
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianGlass
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FloatingComposer(
    inputText: String,
    onInputTextChanged: (String) -> Unit,
    selectedMode: AgentMode,
    pendingAttachments: List<PendingAttachment>,
    isGenerating: Boolean,
    isListening: Boolean,
    partialTranscript: String,
    onOpenModeSelector: () -> Unit,
    onPickImage: () -> Unit,
    onPickDocument: () -> Unit,
    onRemoveAttachment: (String) -> Unit,
    onToggleVoiceInput: () -> Unit,
    onSend: () -> Unit,
    onStopGenerating: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "composerPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseVal"
    )

    val ringBrush = when {
        isListening -> Brush.horizontalGradient(listOf(ErrorRose, CyanAccent))
        isFocused || inputText.isNotBlank() -> Brush.horizontalGradient(
            listOf(selectedMode.accentColor.copy(alpha = 0.85f), CyanAccent.copy(alpha = 0.7f))
        )
        else -> Brush.horizontalGradient(listOf(ObsidianBorder, ObsidianBorder))
    }

    val sendInteraction = remember { MutableInteractionSource() }
    val sendPressed by sendInteraction.collectIsPressedAsState()
    val sendScale by animateFloatAsState(
        targetValue = if (sendPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "sendScale"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Live partial speech-to-text banner
        AnimatedVisibility(visible = isListening) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ErrorRose.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, ErrorRose.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(ErrorRose.copy(alpha = pulse))
                    )
                    Text(
                        text = if (partialTranscript.isNotBlank()) partialTranscript else "Listening... speak now",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Pending Attachments Preview Strip
        AnimatedVisibility(visible = pendingAttachments.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pendingAttachments.forEach { att ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ObsidianElevated,
                        border = BorderStroke(1.dp, ObsidianBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (att.isImage && att.bitmap != null) {
                                Image(
                                    bitmap = att.bitmap.asImageBitmap(),
                                    contentDescription = att.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Description,
                                    contentDescription = att.displayName,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = att.displayName.take(18),
                                style = MaterialTheme.typography.labelSmall,
                                color = TextPrimary
                            )
                            IconButton(
                                onClick = { onRemoveAttachment(att.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove attachment",
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Frosted-Glass Floating Input Container
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ObsidianGlass,
            border = BorderStroke(if (isFocused || isListening) 1.5.dp else 1.dp, ringBrush),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text = "Ask ${selectedMode.displayName} anything...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextMuted
                        )
                    }
                    BasicTextField(
                        value = inputText,
                        onValueChange = onInputTextChanged,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                        cursorBrush = SolidColor(selectedMode.accentColor),
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFocused = it.isFocused }
                            .testTag("chat_input_field")
                    )
                }

                // Bottom Controls Row: Attach + Agent Mode Pill + Mic + Send/Stop
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box {
                            IconButton(
                                onClick = { showAttachMenu = true },
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("attach_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AttachFile,
                                    contentDescription = "Attach image or file",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showAttachMenu,
                                onDismissRequest = { showAttachMenu = false },
                                containerColor = ObsidianElevated
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Attach Photo", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Image, contentDescription = null, tint = CyanAccent)
                                    },
                                    onClick = {
                                        showAttachMenu = false
                                        onPickImage()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Attach Text / CSV / Code File", color = TextPrimary) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.Description, contentDescription = null, tint = selectedMode.accentColor)
                                    },
                                    onClick = {
                                        showAttachMenu = false
                                        onPickDocument()
                                    }
                                )
                            }
                        }

                        // Quick Agent Mode Selector Pill
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = selectedMode.accentColor.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, selectedMode.accentColor.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onOpenModeSelector() }
                                .testTag("agent_mode_pill")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = selectedMode.icon,
                                    contentDescription = selectedMode.displayName,
                                    tint = selectedMode.accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = selectedMode.displayName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = "Switch agent mode",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Voice Input (STT) Button
                        val micBg by animateColorAsState(
                            targetValue = if (isListening) ErrorRose.copy(alpha = 0.25f) else ObsidianSurfaceVariant,
                            label = "micBg"
                        )
                        Surface(
                            shape = CircleShape,
                            color = micBg,
                            border = BorderStroke(
                                1.dp,
                                if (isListening) ErrorRose.copy(alpha = pulse) else ObsidianBorder
                            ),
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable { onToggleVoiceInput() }
                                .testTag("voice_input_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = if (isListening) "Stop listening" else "Voice input",
                                    tint = if (isListening) ErrorRose else TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Morphing Send <-> Pulsing Stop Button
                        val canSend = inputText.isNotBlank() || pendingAttachments.isNotEmpty()
                        val actionBg by animateColorAsState(
                            targetValue = when {
                                isGenerating -> ErrorRose.copy(alpha = 0.85f * pulse)
                                canSend -> selectedMode.accentColor
                                else -> ObsidianSurfaceVariant
                            },
                            label = "sendBtnBg"
                        )

                        Surface(
                            shape = CircleShape,
                            color = actionBg,
                            modifier = Modifier
                                .size(46.dp)
                                .scale(sendScale)
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = sendInteraction,
                                    indication = null,
                                    enabled = isGenerating || canSend,
                                    onClick = { if (isGenerating) onStopGenerating() else onSend() }
                                )
                                .testTag(if (isGenerating) "stop_button" else "send_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AnimatedContent(
                                    targetState = isGenerating,
                                    transitionSpec = {
                                        (scaleIn(spring(stiffness = Spring.StiffnessMedium)) + fadeIn()) togetherWith
                                            (scaleOut(tween(120)) + fadeOut())
                                    },
                                    label = "sendStopMorph"
                                ) { generating ->
                                    if (generating) {
                                        Icon(
                                            imageVector = Icons.Rounded.Stop,
                                            contentDescription = "Stop generating",
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Send,
                                            contentDescription = "Send prompt",
                                            tint = if (canSend) Color.White else TextMuted,
                                            modifier = Modifier.size(20.dp)
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
