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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.AgentMode
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AgentThinkingPill(
    steps: List<String>,
    isActive: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (steps.isEmpty()) return

    var expanded by remember(isActive) { mutableStateOf(isActive) }
    val infiniteTransition = rememberInfiniteTransition(label = "thinkingPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thinkingGlow"
    )

    val borderBrush = if (isActive) {
        Brush.horizontalGradient(
            listOf(
                accentColor.copy(alpha = pulseAlpha),
                CyanAccent.copy(alpha = pulseAlpha * 0.85f)
            )
        )
    } else {
        Brush.horizontalGradient(listOf(ObsidianBorder, ObsidianBorder))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurfaceVariant.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, borderBrush),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
            .testTag("agent_thinking_pill")
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) accentColor.copy(alpha = pulseAlpha)
                            else accentColor.copy(alpha = 0.7f)
                        )
                )
                Text(
                    text = if (isActive) steps.lastOrNull() ?: "Agent Thinking..." else "Thought process (${steps.size} steps)",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isActive) TextPrimary else TextSecondary
                )
                Icon(
                    imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse thinking steps" else "Expand thinking steps",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    steps.forEachIndexed { idx, step ->
                        val isCurrent = isActive && idx == steps.lastIndex
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isCurrent) Icons.Rounded.AutoAwesome else Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = if (isCurrent) accentColor else SuccessGreen.copy(alpha = 0.85f),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isCurrent) TextPrimary else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageActionRow(
    messageText: String,
    isSpeaking: Boolean,
    onRegenerate: () -> Unit,
    onToggleReadAloud: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TactileActionChip(
            onClick = {
                clipboard.setText(AnnotatedString(messageText))
                copied = true
                scope.launch {
                    delay(1800)
                    copied = false
                }
            },
            testTag = "copy_message_button"
        ) {
            AnimatedContent(
                targetState = copied,
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "copyMsgAnim"
            ) { isCopied ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = "Copy response",
                        tint = if (isCopied) SuccessGreen else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = if (isCopied) "Copied" else "Copy",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCopied) SuccessGreen else TextSecondary
                    )
                }
            }
        }

        TactileActionChip(
            onClick = onRegenerate,
            testTag = "regenerate_message_button"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Regenerate response",
                    tint = TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Regenerate",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }

        TactileActionChip(
            onClick = onToggleReadAloud,
            isActive = isSpeaking,
            testTag = "read_aloud_button"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Rounded.StopCircle else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = if (isSpeaking) "Stop reading aloud" else "Read aloud",
                    tint = if (isSpeaking) CyanAccent else TextSecondary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = if (isSpeaking) "Stop" else "Read Aloud",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSpeaking) CyanAccent else TextSecondary
                )
            }
        }
    }
}

@Composable
private fun TactileActionChip(
    onClick: () -> Unit,
    isActive: Boolean = false,
    testTag: String,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "actionScale"
    )
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isActive) CyanAccent.copy(alpha = 0.14f) else ObsidianSurfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(
            1.dp,
            if (isActive) CyanAccent.copy(alpha = 0.45f) else ObsidianBorder
        ),
        modifier = Modifier
            .scale(scale)
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .testTag(testTag)
    ) {
        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            content()
        }
    }
}

@Composable
fun FollowUpSuggestionsRow(
    suggestions: List<String>,
    accentColor: Color,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (suggestions.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        suggestions.forEachIndexed { idx, prompt ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ObsidianElevated.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSuggestionClick(prompt) }
                    .testTag("followup_chip_$idx")
            ) {
                Text(
                    text = prompt,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AgentModeSelectorSheet(
    selectedMode: AgentMode,
    onSelectMode: (AgentMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ObsidianSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choose Agent Mode",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Text(
                text = "Switch persona, reasoning style, and specialized output tools anytime.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            AgentMode.entries.forEach { mode ->
                val isSelected = mode == selectedMode
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) mode.accentColor else ObsidianBorder,
                    label = "modeBorder"
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) mode.accentColor.copy(alpha = 0.12f) else ObsidianSurfaceVariant,
                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            onSelectMode(mode)
                            onDismiss()
                        }
                        .testTag("agent_mode_option_${mode.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = mode.accentColor.copy(alpha = 0.18f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = mode.icon,
                                        contentDescription = mode.displayName,
                                        tint = mode.accentColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = mode.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = mode.accentColor.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = mode.badgeLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = mode.accentColor,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = mode.shortDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected",
                                    tint = mode.accentColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mode.capabilities.forEach { cap ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = ObsidianElevated,
                                    border = BorderStroke(0.5.dp, ObsidianBorder)
                                ) {
                                    Text(
                                        text = cap,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
