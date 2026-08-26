package com.aistudio.zenithstudio.rpxwtq

import android.view.Gravity
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.studio.model.StudioLayer
import com.example.ui.theme.DarkOnyx
import com.example.ui.theme.EnergeticYellow
import com.example.ui.theme.HighslateOutline
import com.example.ui.theme.IndustrialAmber
import com.example.ui.theme.MidSlate
import com.example.ui.theme.SlatePanel
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import kotlin.math.atan2
import kotlin.math.sqrt

enum class FormatTab {
    FONT_SIZE,
    COLOR_WHEEL,
    STYLES,
    SPANS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RichTextSelectionDialog(
    currentLayer: StudioLayer,
    onClose: () -> Unit,
    onUpdateLayer: (StudioLayer) -> Unit
) {
    val context = LocalContext.current

    val initialRawText = currentLayer.textContent.ifEmpty { "Text Layer" }
    var textValueState by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialRawText,
                selection = TextRange(0, initialRawText.length)
            )
        )
    }

    var spansList by remember {
        mutableStateOf(RichTextSpanHelper.parseSpans(currentLayer.richTextSpansJson))
    }

    var selectedTab by remember { mutableStateOf(FormatTab.FONT_SIZE) }

    val rawText = textValueState.text
    val liveSelectionStart = minOf(textValueState.selection.start, textValueState.selection.end)
    val liveSelectionEnd = maxOf(textValueState.selection.start, textValueState.selection.end)
    val isLiveTextSelected = liveSelectionStart != liveSelectionEnd && liveSelectionStart >= 0 && liveSelectionEnd <= rawText.length

    var lockedSelectionRange by remember { mutableStateOf<TextRange?>(if (isLiveTextSelected) TextRange(liveSelectionStart, liveSelectionEnd) else null) }

    androidx.compose.runtime.LaunchedEffect(textValueState.selection, rawText) {
        if (isLiveTextSelected) {
            lockedSelectionRange = TextRange(liveSelectionStart, liveSelectionEnd)
        }
    }

    val activeRange = if (isLiveTextSelected) TextRange(liveSelectionStart, liveSelectionEnd) else lockedSelectionRange
    val isTextSelected = activeRange != null && activeRange.start < activeRange.end && activeRange.end <= rawText.length
    val selectionStart = activeRange?.start ?: 0
    val selectionEnd = activeRange?.end ?: rawText.length
    val selectedSubstring = if (isTextSelected) rawText.substring(selectionStart, selectionEnd) else ""

    // Scan for fonts imported into app storage (context.filesDir/fonts)
    val importedFontsList = remember {
        try {
            val fontsDir = File(context.filesDir, "fonts")
            if (!fontsDir.exists()) fontsDir.mkdirs()
            val files = fontsDir.listFiles { file ->
                file.isFile && (file.extension.equals("ttf", ignoreCase = true) || file.extension.equals("otf", ignoreCase = true))
            } ?: emptyArray()
            files.map { file ->
                val cleanName = file.nameWithoutExtension.replace("_", " ").replace("-", " ")
                cleanName to file.absolutePath
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Dynamic Font Size State for Slider
    var currentFontSize by remember(isTextSelected, selectionStart, selectionEnd, spansList) {
        val activeSpan = spansList.firstOrNull {
            (if (isTextSelected) (it.start <= selectionStart && it.end >= selectionEnd) else true) && it.fontSize != null
        }
        mutableFloatStateOf(activeSpan?.fontSize ?: currentLayer.fontSize.coerceIn(8f, 2000f))
    }

    // Helper to immediately push changes to the main canvas live in real time
    fun syncWithCanvas(
        newText: String = textValueState.text,
        newSpans: List<RichTextSpan> = spansList,
        newBaseFontSize: Float? = null
    ) {
        spansList = newSpans
        val newJson = RichTextSpanHelper.toJson(newSpans)
        onUpdateLayer(
            currentLayer.copy(
                textContent = newText,
                richTextSpansJson = newJson,
                fontSize = newBaseFontSize ?: currentLayer.fontSize
            )
        )
    }

    // Curated Standard Font Families
    val fontOptions = listOf(
        "Sans-Serif" to "Clean Sans",
        "Serif" to "Editorial Serif",
        "Monospace" to "Code Mono",
        "Cursive" to "Handwriting",
        "Casual" to "Casual",
        "Roboto" to "Roboto",
        "Montserrat" to "Montserrat",
        "Playfair" to "Playfair",
        "Pacifico" to "Script Pacifico",
        "Caveat" to "Script Caveat"
    )

    val quickColors = listOf(
        Color(0xFFFFFFFF), // White
        Color(0xFFFFD700), // Gold
        Color(0xFF00FFFF), // Cyan
        Color(0xFFFF007F), // Neon Pink
        Color(0xFF00E676), // Green
        Color(0xFFFF9100), // Orange
        Color(0xFFD500F9), // Purple
        Color(0xFF29B6F6)  // Sky Blue
    )

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.85f)
                .testTag("rich_text_selection_dialog"),
            shape = RoundedCornerShape(18.dp),
            color = DarkOnyx.copy(alpha = 0.98f),
            border = BorderStroke(1.dp, IndustrialAmber.copy(alpha = 0.5f)),
            shadowElevation = 20.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Bar with Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(IndustrialAmber),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = null,
                                tint = DarkOnyx,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Text & Font Editor",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF00E676).copy(alpha = 0.2f),
                                    border = BorderStroke(0.5.dp, Color(0xFF00E676))
                                ) {
                                    Text(
                                        text = "LIVE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF00E676),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Changes apply live to canvas",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("close_rich_text_dialog")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = HighslateOutline, thickness = 1.dp)

                // Text Input Box & Selection Helper
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlatePanel, RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, HighslateOutline), RoundedCornerShape(10.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isTextSelected) "Selected: '$selectedSubstring' ($selectionStart..$selectionEnd)" else "Target: Full Layer Text",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = if (isTextSelected) IndustrialAmber else EnergeticYellow,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = HighslateOutline,
                                modifier = Modifier.clickable {
                                    textValueState = textValueState.copy(selection = TextRange(0, rawText.length))
                                }
                            ) {
                                Text(
                                    "Select All",
                                    fontSize = 9.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = HighslateOutline,
                                modifier = Modifier.clickable {
                                    if (rawText.isNotEmpty()) {
                                        val cursor = textValueState.selection.start
                                        val start = rawText.lastIndexOf(' ', maxOf(0, cursor - 1)).let { if (it == -1) 0 else it + 1 }
                                        val end = rawText.indexOf(' ', cursor).let { if (it == -1) rawText.length else it }
                                        if (start < end) {
                                            textValueState = textValueState.copy(selection = TextRange(start, end))
                                        }
                                    }
                                }
                            ) {
                                Text(
                                    "Word",
                                    fontSize = 9.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            if (isTextSelected) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Red.copy(alpha = 0.3f),
                                    modifier = Modifier.clickable {
                                        lockedSelectionRange = null
                                        textValueState = textValueState.copy(selection = TextRange(0, 0))
                                    }
                                ) {
                                    Text(
                                        "Clear",
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Main BasicTextField
                    BasicTextField(
                        value = textValueState,
                        onValueChange = { newValue ->
                            textValueState = newValue
                            syncWithCanvas(newText = newValue.text)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp, max = 76.dp)
                            .background(DarkOnyx, RoundedCornerShape(6.dp))
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isTextSelected) IndustrialAmber else HighslateOutline
                                ),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp)
                            .testTag("rich_text_input_field"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        singleLine = false,
                        maxLines = 6,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Default
                        )
                    )
                }

                // Real-Time Live Text Preview Screen
                LiveRichTextPreviewScreen(
                    rawText = rawText,
                    currentLayer = currentLayer,
                    spansList = spansList,
                    currentFontSize = currentFontSize
                )

                // Simplified Navigation Segment Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlatePanel, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf(
                        FormatTab.FONT_SIZE to "Font & Size",
                        FormatTab.COLOR_WHEEL to "Color Wheel",
                        FormatTab.STYLES to "Style & Glow",
                        FormatTab.SPANS to "Spans (${spansList.size})"
                    ).forEach { (tab, label) ->
                        val isSelected = selectedTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) IndustrialAmber else Color.Transparent)
                                .clickable { selectedTab = tab },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) DarkOnyx else TextPrimary
                            )
                        }
                    }
                }

                // Tab Content Panel
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(SlatePanel, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, HighslateOutline), RoundedCornerShape(12.dp))
                        .padding(10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (selectedTab) {
                        FormatTab.FONT_SIZE -> {
                            // FONT FAMILY & TEXT SIZE CONTROLS
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // FONT SIZE SLIDER
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isTextSelected) "Size for Selection: '$selectedSubstring'" else "Size for Entire Text",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EnergeticYellow
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = IndustrialAmber,
                                    ) {
                                        Text(
                                            text = "${currentFontSize.toInt()} pt",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DarkOnyx,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Slider(
                                    value = currentFontSize,
                                    onValueChange = { newSize ->
                                        currentFontSize = newSize
                                        if (isTextSelected && selectionEnd > selectionStart) {
                                            val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                currentSpans = spansList,
                                                rangeStart = selectionStart,
                                                rangeEnd = selectionEnd,
                                                fontSize = newSize
                                            )
                                            syncWithCanvas(newSpans = updatedSpans)
                                        } else {
                                            val updatedSpans = spansList.filterNot { it.start == 0 && it.end == rawText.length && it.fontSize != null }
                                            syncWithCanvas(newSpans = updatedSpans, newBaseFontSize = newSize)
                                        }
                                    },
                                    valueRange = 8f..2000f,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(28.dp)
                                        .testTag("font_size_slider"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = IndustrialAmber,
                                        activeTrackColor = IndustrialAmber,
                                        inactiveTrackColor = HighslateOutline
                                    )
                                )

                                // Quick Size Presets
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(16f, 32f, 64f, 100f, 200f, 500f, 1000f, 1500f, 2000f).forEach { sz ->
                                        val isCurrent = currentFontSize.toInt() == sz.toInt()
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isCurrent) IndustrialAmber else DarkOnyx,
                                            border = BorderStroke(1.dp, if (isCurrent) IndustrialAmber else HighslateOutline),
                                            modifier = Modifier.clickable {
                                                currentFontSize = sz
                                                if (isTextSelected && selectionEnd > selectionStart) {
                                                    val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                        currentSpans = spansList,
                                                        rangeStart = selectionStart,
                                                        rangeEnd = selectionEnd,
                                                        fontSize = sz
                                                    )
                                                    syncWithCanvas(newSpans = updatedSpans)
                                                } else {
                                                    val updatedSpans = spansList.filterNot { it.start == 0 && it.end == rawText.length && it.fontSize != null }
                                                    syncWithCanvas(newSpans = updatedSpans, newBaseFontSize = sz)
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = "${sz.toInt()}pt",
                                                fontSize = 10.sp,
                                                color = if (isCurrent) DarkOnyx else EnergeticYellow,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = HighslateOutline, thickness = 0.5.dp)

                                // FONT FAMILY SELECTION
                                Text(
                                    text = if (isTextSelected) "Font Family for: '$selectedSubstring'" else "Font Family for Entire Text",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EnergeticYellow
                                )

                                // Imported Custom Fonts Section
                                if (importedFontsList.isNotEmpty()) {
                                    Text(
                                        text = "IMPORTED CUSTOM FONTS (${importedFontsList.size})",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF00E676)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        importedFontsList.forEach { (displayName, path) ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Color(0xFF00E676).copy(alpha = 0.15f),
                                                border = BorderStroke(1.dp, Color(0xFF00E676)),
                                                modifier = Modifier.clickable {
                                                    val start = if (isTextSelected) selectionStart else 0
                                                    val end = if (isTextSelected) selectionEnd else rawText.length
                                                    if (end > start) {
                                                        val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                            currentSpans = spansList,
                                                            rangeStart = start,
                                                            rangeEnd = end,
                                                            fontFamilyName = path
                                                        )
                                                        syncWithCanvas(newSpans = updatedSpans)
                                                    }
                                                }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Folder,
                                                        contentDescription = null,
                                                        tint = Color(0xFF00E676),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = displayName,
                                                        fontSize = 10.sp,
                                                        color = TextPrimary,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                }

                                // System & Preset Fonts
                                Text(
                                    text = "STANDARD FONT FAMILIES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    fontOptions.forEach { (code, name) ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = DarkOnyx,
                                            border = BorderStroke(1.dp, HighslateOutline),
                                            modifier = Modifier.clickable {
                                                val start = if (isTextSelected) selectionStart else 0
                                                val end = if (isTextSelected) selectionEnd else rawText.length
                                                if (end > start) {
                                                    val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                        currentSpans = spansList,
                                                        rangeStart = start,
                                                        rangeEnd = end,
                                                        fontFamilyName = code
                                                    )
                                                    syncWithCanvas(newSpans = updatedSpans)
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = name,
                                                fontSize = 11.sp,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FormatTab.COLOR_WHEEL -> {
                            // COLOR WHEEL & PALETTE
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isTextSelected) "Text Color for Selection: '$selectedSubstring'" else "Text Color for Entire Text",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EnergeticYellow
                                )

                                // Quick Swatches Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    quickColors.forEach { color ->
                                        val hex = String.format("#%08X", color.toArgb())
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .border(BorderStroke(1.dp, HighslateOutline), CircleShape)
                                                .clickable {
                                                    val start = if (isTextSelected) selectionStart else 0
                                                    val end = if (isTextSelected) selectionEnd else rawText.length
                                                    if (end > start) {
                                                        val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                            currentSpans = spansList,
                                                            rangeStart = start,
                                                            rangeEnd = end,
                                                            colorHex = hex
                                                        )
                                                        syncWithCanvas(newSpans = updatedSpans)
                                                    }
                                                }
                                        )
                                    }
                                }

                                HorizontalDivider(color = HighslateOutline, thickness = 0.5.dp)

                                // Interactive HSV Color Wheel Composable
                                CompactColorWheel(
                                    onColorSelected = { color ->
                                        val hex = String.format("#%08X", color.toArgb())
                                        val start = if (isTextSelected) selectionStart else 0
                                        val end = if (isTextSelected) selectionEnd else rawText.length
                                        if (end > start) {
                                            val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                currentSpans = spansList,
                                                rangeStart = start,
                                                rangeEnd = end,
                                                colorHex = hex
                                            )
                                            syncWithCanvas(newSpans = updatedSpans)
                                        }
                                    }
                                )
                            }
                        }

                        FormatTab.STYLES -> {
                            // BOLD, ITALIC, UNDERLINE, STRIKETHROUGH, GLOW
                            val targetStart = if (isTextSelected) selectionStart else 0
                            val targetEnd = if (isTextSelected) selectionEnd else rawText.length

                            val activeSpansForTarget = remember(spansList, targetStart, targetEnd) {
                                if (targetEnd > targetStart) {
                                    spansList.filter { it.start < targetEnd && it.end > targetStart }
                                } else {
                                    emptyList()
                                }
                            }

                            val isBoldActive = remember(activeSpansForTarget, currentLayer.fontIsBold) {
                                val definedSpan = activeSpansForTarget.firstOrNull { it.isBold != null }
                                if (definedSpan != null) {
                                    definedSpan.isBold == true
                                } else {
                                    currentLayer.fontIsBold
                                }
                            }

                            val isItalicActive = remember(activeSpansForTarget, currentLayer.fontIsItalic) {
                                val definedSpan = activeSpansForTarget.firstOrNull { it.isItalic != null }
                                if (definedSpan != null) {
                                    definedSpan.isItalic == true
                                } else {
                                    currentLayer.fontIsItalic
                                }
                            }

                            val isUnderlineActive = remember(activeSpansForTarget) {
                                activeSpansForTarget.any { it.isUnderline == true }
                            }

                            val isStrikethroughActive = remember(activeSpansForTarget) {
                                activeSpansForTarget.any { it.isStrikethrough == true }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isTextSelected) "Text Formatting for Selection: '$selectedSubstring'" else "Text Formatting for Entire Text",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EnergeticYellow
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Bold
                                    Button(
                                        onClick = {
                                            if (targetEnd > targetStart) {
                                                val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                    currentSpans = spansList,
                                                    rangeStart = targetStart,
                                                    rangeEnd = targetEnd,
                                                    isBold = !isBoldActive
                                                )
                                                syncWithCanvas(newSpans = updatedSpans)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isBoldActive) IndustrialAmber else DarkOnyx,
                                            contentColor = if (isBoldActive) DarkOnyx else TextPrimary
                                        ),
                                        border = BorderStroke(1.dp, if (isBoldActive) IndustrialAmber else HighslateOutline),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(38.dp).testTag("span_bold_button")
                                    ) {
                                        Text(
                                            "B",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = if (isBoldActive) DarkOnyx else TextPrimary
                                        )
                                    }

                                    // Italic
                                    Button(
                                        onClick = {
                                            if (targetEnd > targetStart) {
                                                val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                    currentSpans = spansList,
                                                    rangeStart = targetStart,
                                                    rangeEnd = targetEnd,
                                                    isItalic = !isItalicActive
                                                )
                                                syncWithCanvas(newSpans = updatedSpans)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isItalicActive) IndustrialAmber else DarkOnyx,
                                            contentColor = if (isItalicActive) DarkOnyx else TextPrimary
                                        ),
                                        border = BorderStroke(1.dp, if (isItalicActive) IndustrialAmber else HighslateOutline),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(38.dp).testTag("span_italic_button")
                                    ) {
                                        Text(
                                            "I",
                                            fontStyle = FontStyle.Italic,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isItalicActive) DarkOnyx else TextPrimary
                                        )
                                    }

                                    // Underline
                                    Button(
                                        onClick = {
                                            if (targetEnd > targetStart) {
                                                val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                    currentSpans = spansList,
                                                    rangeStart = targetStart,
                                                    rangeEnd = targetEnd,
                                                    isUnderline = !isUnderlineActive
                                                )
                                                syncWithCanvas(newSpans = updatedSpans)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isUnderlineActive) IndustrialAmber else DarkOnyx,
                                            contentColor = if (isUnderlineActive) DarkOnyx else TextPrimary
                                        ),
                                        border = BorderStroke(1.dp, if (isUnderlineActive) IndustrialAmber else HighslateOutline),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(38.dp).testTag("span_underline_button")
                                    ) {
                                        Text(
                                            "U",
                                            textDecoration = TextDecoration.Underline,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isUnderlineActive) DarkOnyx else TextPrimary
                                        )
                                    }

                                    // Strikethrough
                                    Button(
                                        onClick = {
                                            if (targetEnd > targetStart) {
                                                val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                    currentSpans = spansList,
                                                    rangeStart = targetStart,
                                                    rangeEnd = targetEnd,
                                                    isStrikethrough = !isStrikethroughActive
                                                )
                                                syncWithCanvas(newSpans = updatedSpans)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isStrikethroughActive) IndustrialAmber else DarkOnyx,
                                            contentColor = if (isStrikethroughActive) DarkOnyx else TextPrimary
                                        ),
                                        border = BorderStroke(1.dp, if (isStrikethroughActive) IndustrialAmber else HighslateOutline),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).height(38.dp).testTag("span_strikethrough_button")
                                    ) {
                                        Text(
                                            "S",
                                            textDecoration = TextDecoration.LineThrough,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = if (isStrikethroughActive) DarkOnyx else TextPrimary
                                        )
                                    }
                                }

                                HorizontalDivider(color = HighslateOutline, thickness = 0.5.dp)

                                Text(
                                    text = "Text Highlight Glow",
                                    fontSize = 10.sp,
                                    color = TextSecondary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "Yellow" to "#60FFFF00",
                                        "Cyan" to "#6000FFFF",
                                        "Magenta" to "#60FF00FF",
                                        "Dark" to "#CC000000",
                                        "Clear" to ""
                                    ).forEach { (lbl, bg) ->
                                        val isGlowActive = if (bg.isEmpty()) {
                                            activeSpansForTarget.all { it.backgroundColorHex.isNullOrBlank() }
                                        } else {
                                            activeSpansForTarget.any { it.backgroundColorHex == bg }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isGlowActive) IndustrialAmber.copy(alpha = 0.3f) else DarkOnyx,
                                            border = BorderStroke(1.dp, if (isGlowActive) IndustrialAmber else HighslateOutline),
                                            modifier = Modifier.clickable {
                                                if (targetEnd > targetStart) {
                                                    val newBg = if (isGlowActive && bg.isNotEmpty()) null else (if (bg.isBlank()) null else bg)
                                                    val updatedSpans = RichTextSpanHelper.applyStyleToRange(
                                                        currentSpans = spansList,
                                                        rangeStart = targetStart,
                                                        rangeEnd = targetEnd,
                                                        backgroundColorHex = newBg
                                                    )
                                                    syncWithCanvas(newSpans = updatedSpans)
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = lbl,
                                                fontSize = 10.sp,
                                                color = if (isGlowActive) IndustrialAmber else TextPrimary,
                                                fontWeight = if (isGlowActive) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        FormatTab.SPANS -> {
                            // APPLIED SPANS OVERVIEW
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Active Substring Formatting Spans (${spansList.size})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )

                                    if (spansList.isNotEmpty()) {
                                        TextButton(
                                            onClick = { syncWithCanvas(newSpans = emptyList()) },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(24.dp)
                                        ) {
                                            Text("Clear All", fontSize = 10.sp, color = Color.Red)
                                        }
                                    }
                                }

                                if (spansList.isEmpty()) {
                                    Text(
                                        text = "No character spans applied yet. Select text above to format individual words.",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                } else {
                                    spansList.forEachIndexed { index, span ->
                                        val spanTxt = if (span.start < rawText.length && span.end <= rawText.length) {
                                            rawText.substring(span.start, span.end)
                                        } else ""

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(DarkOnyx, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "Chars ${span.start}–${span.end}: '$spanTxt'",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = EnergeticYellow
                                                )
                                                val details = mutableListOf<String>()
                                                if (span.colorHex != null) details.add("Color: ${span.colorHex}")
                                                if (span.fontSize != null) details.add("Size: ${span.fontSize}pt")
                                                if (span.fontFamilyName != null) {
                                                    val fontLabel = if (File(span.fontFamilyName).exists()) {
                                                        File(span.fontFamilyName).nameWithoutExtension.replace("_", " ")
                                                    } else span.fontFamilyName
                                                    details.add("Font: $fontLabel")
                                                }
                                                if (span.isBold == true) details.add("Bold")
                                                if (span.isItalic == true) details.add("Italic")

                                                Text(
                                                    text = details.joinToString(" • "),
                                                    fontSize = 9.sp,
                                                    color = TextSecondary
                                                )
                                            }

                                            IconButton(
                                                onClick = {
                                                    val filtered = spansList.filterIndexed { i, _ -> i != index }
                                                    syncWithCanvas(newSpans = filtered)
                                                },
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Done Button
                Button(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .testTag("apply_rich_text_spans_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = DarkOnyx, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("DONE", color = DarkOnyx, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

/**
 * Compact Interactive Color Wheel Composable
 */
@Composable
fun CompactColorWheel(
    onColorSelected: (Color) -> Unit
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(1f) }

    val activeColor = remember(hue, saturation, brightness) {
        Color.hsv(hue, saturation, brightness)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Wheel Canvas
        Box(
            modifier = Modifier
                .size(110.dp)
                .testTag("color_wheel_picker_box"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val dist = sqrt(dx * dx + dy * dy)
                            if (dist <= radius) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                hue = angle
                                saturation = (dist / radius).coerceIn(0f, 1f)
                                onColorSelected(Color.hsv(hue, saturation, brightness))
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.width / 2f
                            val dx = change.position.x - center.x
                            val dy = change.position.y - center.y
                            val dist = sqrt(dx * dx + dy * dy)
                            var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < 0) angle += 360f
                            hue = angle
                            saturation = (dist / radius).coerceIn(0f, 1f)
                            onColorSelected(Color.hsv(hue, saturation, brightness))
                        }
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f

                val hueColors = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
                drawCircle(
                    brush = Brush.sweepGradient(hueColors, center),
                    radius = radius,
                    center = center
                )

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = brightness), Color.Transparent),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Selection Indicator
                val angleRad = Math.toRadians(hue.toDouble())
                val indicatorDist = saturation * radius
                val indX = center.x + (indicatorDist * kotlin.math.cos(angleRad)).toFloat()
                val indY = center.y + (indicatorDist * kotlin.math.sin(angleRad)).toFloat()

                drawCircle(Color.Black, radius = 7.dp.toPx(), center = Offset(indX, indY))
                drawCircle(Color.White, radius = 5.dp.toPx(), center = Offset(indX, indY))
                drawCircle(activeColor, radius = 3.dp.toPx(), center = Offset(indX, indY))
            }
        }

        // Color Info & Brightness Slider
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(activeColor)
                        .border(BorderStroke(1.dp, HighslateOutline), CircleShape)
                )
                Text(
                    text = String.format("#%08X", activeColor.toArgb()),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Text("Brightness", fontSize = 9.sp, color = TextSecondary)

            Slider(
                value = brightness,
                onValueChange = {
                    brightness = it
                    onColorSelected(Color.hsv(hue, saturation, brightness))
                },
                valueRange = 0f..1f,
                modifier = Modifier.height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = IndustrialAmber,
                    activeTrackColor = IndustrialAmber
                )
            )
        }
    }
}

/**
 * Real-Time Live Text Preview Screen
 */
@Composable
fun LiveRichTextPreviewScreen(
    rawText: String,
    currentLayer: StudioLayer,
    spansList: List<RichTextSpan>,
    currentFontSize: Float,
    modifier: Modifier = Modifier
) {
    var previewBgMode by remember { mutableStateOf("DARK") } // "DARK", "LIGHT", "GRID"

    val layerWithSpans = remember(currentLayer, spansList) {
        currentLayer.copy(
            richTextSpansJson = RichTextSpanHelper.toJson(spansList)
        )
    }

    val spannableText = remember(rawText, spansList, layerWithSpans) {
        RichTextSpanHelper.buildSpannableText(rawText, spansList, layerWithSpans)
    }

    val containerBgColor = when (previewBgMode) {
        "LIGHT" -> Color(0xFFF8F9FA)
        "GRID" -> Color(0xFF10141D)
        else -> Color(0xFF0D0F14)
    }

    val containerTextColor = when (previewBgMode) {
        "LIGHT" -> android.graphics.Color.BLACK
        else -> android.graphics.Color.WHITE
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SlatePanel)
            .border(BorderStroke(1.dp, IndustrialAmber.copy(alpha = 0.5f)), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Preview Header Bar with Canvas Switcher Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(IndustrialAmber.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        tint = IndustrialAmber,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Text(
                    text = "REAL-TIME LIVE PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }

            // Canvas Background Switcher (Dark, Light, Grid)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Theme:",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                listOf(
                    "DARK" to Color(0xFF181A20),
                    "LIGHT" to Color(0xFFFFFFFF),
                    "GRID" to Color(0xFF00E676)
                ).forEach { (mode, color) ->
                    val isSel = previewBgMode == mode
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSel) IndustrialAmber else DarkOnyx,
                        border = BorderStroke(1.dp, if (isSel) IndustrialAmber else HighslateOutline),
                        modifier = Modifier.clickable { previewBgMode = mode }
                    ) {
                        Text(
                            text = mode,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) DarkOnyx else TextPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Preview Window Box with Scrollable Rendered Text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 70.dp, max = 150.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(containerBgColor)
                .border(BorderStroke(1.dp, HighslateOutline.copy(alpha = 0.6f)), RoundedCornerShape(8.dp))
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
                .horizontalScroll(rememberScrollState()),
            contentAlignment = Alignment.CenterStart
        ) {
            AndroidView(
                factory = { ctx ->
                    TextView(ctx).apply {
                        setTextColor(containerTextColor)
                        textSize = currentLayer.fontSize.coerceIn(12f, 160f)
                        setPadding(0, 0, 0, 0)
                        gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    }
                },
                update = { textView ->
                    textView.text = spannableText
                    textView.setTextColor(containerTextColor)
                    textView.textSize = currentLayer.fontSize.coerceIn(12f, 160f)
                },
                modifier = Modifier.testTag("realtime_text_preview_render")
            )
        }

        // Live Formatting Summary Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = DarkOnyx,
                border = BorderStroke(0.5.dp, HighslateOutline)
            ) {
                Text(
                    text = "Length: ${rawText.length} chars",
                    fontSize = 9.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = DarkOnyx,
                border = BorderStroke(0.5.dp, HighslateOutline)
            ) {
                Text(
                    text = "Spans: ${spansList.size}",
                    fontSize = 9.sp,
                    color = EnergeticYellow,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = DarkOnyx,
                border = BorderStroke(0.5.dp, HighslateOutline)
            ) {
                Text(
                    text = "Size: ${currentFontSize.toInt()}pt",
                    fontSize = 9.sp,
                    color = IndustrialAmber,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
