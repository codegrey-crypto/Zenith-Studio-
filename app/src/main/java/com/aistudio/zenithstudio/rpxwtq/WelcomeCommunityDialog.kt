package com.aistudio.zenithstudio.rpxwtq

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DarkOnyx
import com.example.ui.theme.EnergeticYellow
import com.example.ui.theme.HighslateOutline
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.MidSlate
import com.example.ui.theme.SlatePanel
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeCommunityDialog(
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .testTag("welcome_community_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = DarkOnyx.copy(alpha = 0.98f),
            border = BorderStroke(1.5.dp, IndustrialAmber),
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row with Title and Close Button 'X'
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(IndustrialAmber),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = DarkOnyx,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "ZENITH STUDIO",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = IndustrialAmber,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "By Harold / Zenith Creator",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Top-right Close Button ('X')
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MidSlate)
                            .testTag("close_welcome_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close welcome popup",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = HighslateOutline, thickness = 1.dp)

                // Main Greeting Content
                Text(
                    text = "Welcome to Zenith Studio!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Unlock your full creative potential with advanced multi-layer motion graphics, vector rasterization, and custom typography.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // Hyperlink Action Cards
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. YouTube Channel Hyperlink
                    WelcomeHyperlinkCard(
                        icon = Icons.Default.PlayCircle,
                        iconTint = Color(0xFFFF0000), // YouTube Red
                        title = "Tutorials & Guides",
                        subtitle = "Watch step-by-step video tutorials on YouTube",
                        tagText = "YOUTUBE",
                        onClick = {
                            try {
                                uriHandler.openUri("https://www.youtube.com/@Haroldkreative")
                            } catch (e: Exception) {}
                        }
                    )

                    // 2. WhatsApp Community Hyperlink
                    WelcomeHyperlinkCard(
                        icon = Icons.Default.Chat,
                        iconTint = Color(0xFF25D366), // WhatsApp Green
                        title = "WhatsApp Community",
                        subtitle = "Join our active creator group & discussion",
                        tagText = "COMMUNITY",
                        onClick = {
                            try {
                                uriHandler.openUri("https://chat.whatsapp.com/ClnAvolDFbCBByvGM5tIvO")
                            } catch (e: Exception) {}
                        }
                    )

                    // 3. Resources & Presets Hyperlink
                    WelcomeHyperlinkCard(
                        icon = Icons.Default.FolderZip,
                        iconTint = Color(0xFF00E5FF), // Cyan
                        title = "Resources & Presets",
                        subtitle = "Download premium fonts, vector assets & overlays",
                        tagText = "FREE ASSETS",
                        onClick = {
                            try {
                                uriHandler.openUri("https://chat.whatsapp.com/ClnAvolDFbCBByvGM5tIvO")
                            } catch (e: Exception) {}
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Bottom Dismiss / Got It Button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IndustrialAmber,
                        contentColor = DarkOnyx
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("dismiss_welcome_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DarkOnyx,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "START CREATING",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = DarkOnyx
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeHyperlinkCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    tagText: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SlatePanel,
        border = BorderStroke(1.dp, HighslateOutline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = iconTint.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = tagText,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = iconTint,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open Link",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
