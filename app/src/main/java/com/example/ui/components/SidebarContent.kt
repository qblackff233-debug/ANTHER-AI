package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.Conversation
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRose
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

@Composable
fun SidebarContent(
    conversations: List<Conversation>,
    activeConversationId: String?,
    displayName: String,
    email: String?,
    photoUrl: String?,
    onNewChat: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = remember(conversations) { groupConversationsByDate(conversations) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(ObsidianSurface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(14.dp)
    ) {
        // Header Brand + New Chat Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Aether AI",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = EmeraldPrimary.copy(alpha = 0.16f)
            ) {
                Text(
                    text = "7 Modes",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onNewChat,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("new_chat_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "New Chat",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Chat",
                style = MaterialTheme.typography.titleSmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "Your saved chats will appear here",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                grouped.forEach { (sectionLabel, itemsInSection) ->
                    item(key = "header_$sectionLabel") {
                        Text(
                            text = sectionLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                    items(itemsInSection, key = { it.id }) { conv ->
                        val isActive = conv.id == activeConversationId
                        val mode = conv.resolvedMode()
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) ObsidianElevated else Color.Transparent,
                            border = if (isActive) BorderStroke(1.dp, ObsidianBorder) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectConversation(conv.id) }
                                .testTag("history_item_${conv.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = mode.icon,
                                    contentDescription = mode.displayName,
                                    tint = if (isActive) mode.accentColor else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = conv.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                                        ),
                                        color = if (isActive) TextPrimary else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (conv.lastMessagePreview.isNotBlank()) {
                                        Text(
                                            text = conv.lastMessagePreview,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { onDeleteConversation(conv.id) },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete conversation",
                                        tint = TextMuted,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // Authenticated Account Footer + Logout
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ObsidianSurfaceVariant,
            border = BorderStroke(1.dp, ObsidianBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = "User avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Person,
                                contentDescription = "User avatar",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!email.isNullOrBlank()) {
                        Text(
                            text = email,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = "Log out",
                        tint = ErrorRose,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun groupConversationsByDate(
    conversations: List<Conversation>
): List<Pair<String, List<Conversation>>> {
    val nowMs = System.currentTimeMillis()
    val oneDayMs = TimeUnit.DAYS.toMillis(1)
    val twoDaysMs = TimeUnit.DAYS.toMillis(2)
    val sevenDaysMs = TimeUnit.DAYS.toMillis(7)

    val today = mutableListOf<Conversation>()
    val yesterday = mutableListOf<Conversation>()
    val week = mutableListOf<Conversation>()
    val older = mutableListOf<Conversation>()

    conversations.forEach { conv ->
        val ts = (conv.updatedAt ?: conv.createdAt ?: Timestamp.now()).toDate().time
        val age = (nowMs - ts).coerceAtLeast(0L)
        when {
            age < oneDayMs -> today.add(conv)
            age < twoDaysMs -> yesterday.add(conv)
            age < sevenDaysMs -> week.add(conv)
            else -> older.add(conv)
        }
    }

    return buildList {
        if (today.isNotEmpty()) add("Today" to today)
        if (yesterday.isNotEmpty()) add("Yesterday" to yesterday)
        if (week.isNotEmpty()) add("Previous 7 Days" to week)
        if (older.isNotEmpty()) add("Earlier" to older)
    }
}
