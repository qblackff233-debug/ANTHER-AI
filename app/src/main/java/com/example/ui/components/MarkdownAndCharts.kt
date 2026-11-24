package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChartDataPoint
import com.example.model.ChartSpec
import com.example.ui.theme.CodeComment
import com.example.ui.theme.CodeHeaderSurface
import com.example.ui.theme.CodeKeyword
import com.example.ui.theme.CodeNumber
import com.example.ui.theme.CodeString
import com.example.ui.theme.CodeSurface
import com.example.ui.theme.CodeType
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ModeAnalystColor
import com.example.ui.theme.MonoFontFamily
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
import org.json.JSONObject

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletList(val items: List<String>, val ordered: Boolean) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data class ChartBlock(val spec: ChartSpec) : MarkdownBlock
}

data class ParsedAiOutput(
    val cleanDisplayText: String,
    val followUpSuggestions: List<String>
)

fun parseAiMetadata(rawText: String, isStreaming: Boolean): ParsedAiOutput {
    val followUpRegex = Regex("\\[FOLLOWUPS\\]([\\s\\S]*?)\\[/FOLLOWUPS\\]")
    val match = followUpRegex.find(rawText)
    val followUps = match?.groupValues?.getOrNull(1)
        ?.split("|")
        ?.map { it.trim() }
        ?.filter { it.isNotBlank() }
        ?.take(3)
        ?: emptyList()

    var cleaned = rawText.replace(followUpRegex, "")
    if (isStreaming) {
        // Hide incomplete trailing [FOLLOWUPS or [CHART block while streaming tokens
        val partialFollowupIdx = cleaned.indexOf("[FOLLOWUPS")
        if (partialFollowupIdx >= 0) {
            cleaned = cleaned.substring(0, partialFollowupIdx)
        }
        val openChartIdx = cleaned.lastIndexOf("[CHART]")
        val closeChartIdx = cleaned.lastIndexOf("[/CHART]")
        if (openChartIdx > closeChartIdx) {
            cleaned = cleaned.substring(0, openChartIdx)
        }
    }
    return ParsedAiOutput(cleanDisplayText = cleaned.trimEnd(), followUpSuggestions = followUps)
}

fun parseMarkdownBlocks(input: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = input.replace("\r\n", "\n").split("\n")
    var i = 0

    while (i < lines.size) {
        val line = lines[i]
        val trimmed = line.trim()

        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // 1. Chart block [CHART]{...}[/CHART]
        if (trimmed.startsWith("[CHART]")) {
            val sb = StringBuilder()
            while (i < lines.size) {
                sb.append(lines[i]).append("\n")
                if (lines[i].contains("[/CHART]")) {
                    i++
                    break
                }
                i++
            }
            val rawJson = sb.toString()
                .substringAfter("[CHART]")
                .substringBefore("[/CHART]")
                .trim()
            val spec = parseChartJson(rawJson)
            if (spec != null) {
                blocks.add(MarkdownBlock.ChartBlock(spec))
            }
            continue
        }

        // 2. Fenced code block ```lang
        if (trimmed.startsWith("```")) {
            val language = trimmed.removePrefix("```").trim().ifBlank { "code" }
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++
            blocks.add(MarkdownBlock.CodeBlock(language, codeLines.joinToString("\n")))
            continue
        }

        // 3. Markdown Table (header | col, followed by |---|)
        if (trimmed.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1])) {
            val headers = splitTableRow(trimmed)
            i += 2
            val rows = mutableListOf<List<String>>()
            while (i < lines.size) {
                val rowTrimmed = lines[i].trim()
                if (!rowTrimmed.contains("|") || rowTrimmed.isEmpty()) break
                rows.add(splitTableRow(rowTrimmed))
                i++
            }
            if (headers.isNotEmpty()) {
                blocks.add(MarkdownBlock.TableBlock(headers, rows))
            }
            continue
        }

        // 4. Headings #, ##, ###
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
            val title = trimmed.drop(level).trim()
            if (title.isNotEmpty()) {
                blocks.add(MarkdownBlock.Heading(level, title))
                i++
                continue
            }
        }

        // 5. Unordered or Ordered Lists
        if (isBulletLine(trimmed) || isNumberedLine(trimmed)) {
            val ordered = isNumberedLine(trimmed)
            val items = mutableListOf<String>()
            while (i < lines.size) {
                val cur = lines[i].trim()
                if (ordered && isNumberedLine(cur)) {
                    items.add(cur.replaceFirst(Regex("^\\d+[.)]\\s*"), ""))
                    i++
                } else if (!ordered && isBulletLine(cur)) {
                    items.add(cur.drop(1).trim())
                    i++
                } else {
                    break
                }
            }
            blocks.add(MarkdownBlock.BulletList(items, ordered))
            continue
        }

        // 6. Regular Paragraph (merge consecutive plain lines)
        val paraLines = mutableListOf<String>()
        while (i < lines.size) {
            val cur = lines[i].trim()
            if (cur.isEmpty() ||
                cur.startsWith("```") ||
                cur.startsWith("#") ||
                cur.startsWith("[CHART]") ||
                isBulletLine(cur) ||
                isNumberedLine(cur) ||
                (cur.contains("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1]))
            ) {
                break
            }
            paraLines.add(lines[i].trimEnd())
            i++
        }
        if (paraLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paraLines.joinToString("\n")))
        } else {
            i++
        }
    }
    return blocks
}

private fun isTableSeparator(line: String): Boolean {
    val t = line.trim()
    return t.contains("|") && t.contains("-") && t.all { it == '|' || it == '-' || it == ':' || it == ' ' }
}

private fun splitTableRow(line: String): List<String> =
    line.trim().trim('|').split("|").map { it.trim() }

private fun isBulletLine(trimmed: String): Boolean =
    (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• "))

private fun isNumberedLine(trimmed: String): Boolean =
    Regex("^\\d+[.)]\\s+.*").matches(trimmed)

private fun parseChartJson(rawJson: String): ChartSpec? {
    return try {
        val obj = JSONObject(rawJson)
        val title = obj.optString("title", "Data Visualization")
        val type = obj.optString("type", "bar").lowercase()
        val unit = obj.optString("unit", "")
        val arr = obj.getJSONArray("items")
        val items = (0 until arr.length()).mapNotNull { idx ->
            val item = arr.optJSONObject(idx) ?: return@mapNotNull null
            val label = item.optString("label", "")
            val value = item.optDouble("value", Double.NaN)
            if (label.isNotBlank() && !value.isNaN()) ChartDataPoint(label, value) else null
        }
        if (items.size >= 2) ChartSpec(title, type, unit, items) else null
    } catch (_: Exception) {
        null
    }
}

@Composable
fun MarkdownMessageContent(
    text: String,
    isStreaming: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    val cursorAlpha = if (isStreaming) {
        val infiniteTransition = rememberInfiniteTransition(label = "streamCursor")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(550),
                repeatMode = RepeatMode.Reverse
            ),
            label = "cursorBlink"
        )
        alpha
    } else 0f

    SelectionContainer(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            blocks.forEachIndexed { index, block ->
                val isLast = index == blocks.lastIndex
                when (block) {
                    is MarkdownBlock.Heading -> {
                        val style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        }
                        Text(
                            text = formatInlineMarkdown(block.text),
                            style = style,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = if (index > 0) 4.dp else 0.dp)
                        )
                    }

                    is MarkdownBlock.Paragraph -> {
                        val annotated = buildAnnotatedString {
                            append(formatInlineMarkdown(block.text))
                            if (isStreaming && isLast) {
                                withStyle(
                                    SpanStyle(
                                        color = accentColor.copy(alpha = cursorAlpha),
                                        fontWeight = FontWeight.Bold
                                    )
                                ) {
                                    append(" ▍")
                                }
                            }
                        }
                        Text(
                            text = annotated,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }

                    is MarkdownBlock.BulletList -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            block.items.forEachIndexed { itemIdx, itemText ->
                                val isVeryLast = isStreaming && isLast && itemIdx == block.items.lastIndex
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = if (block.ordered) "${itemIdx + 1}." else "•",
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = accentColor
                                    )
                                    val annotatedItem = buildAnnotatedString {
                                        append(formatInlineMarkdown(itemText))
                                        if (isVeryLast) {
                                            withStyle(
                                                SpanStyle(color = accentColor.copy(alpha = cursorAlpha))
                                            ) {
                                                append(" ▍")
                                            }
                                        }
                                    }
                                    Text(
                                        text = annotatedItem,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    is MarkdownBlock.CodeBlock -> {
                        SyntaxHighlightedCodeBlock(
                            language = block.language,
                            code = block.code
                        )
                    }

                    is MarkdownBlock.TableBlock -> {
                        MarkdownTable(
                            headers = block.headers,
                            rows = block.rows,
                            accentColor = accentColor
                        )
                    }

                    is MarkdownBlock.ChartBlock -> {
                        DataChartCard(spec = block.spec)
                    }
                }
            }

            if (isStreaming && blocks.isEmpty()) {
                Text(
                    text = "▍",
                    style = MaterialTheme.typography.bodyLarge,
                    color = accentColor.copy(alpha = cursorAlpha)
                )
            }
        }
    }
}

@Composable
private fun SyntaxHighlightedCodeBlock(
    language: String,
    code: String
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CodeSurface,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CodeHeaderSurface)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = language.lowercase(),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = MonoFontFamily),
                    color = TextSecondary
                )
                IconButton(
                    onClick = {
                        clipboard.setText(AnnotatedString(code))
                        copied = true
                        scope.launch {
                            delay(1800)
                            copied = false
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    AnimatedContent(
                        targetState = copied,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                        label = "copyCodeAnim"
                    ) { isCopied ->
                        Icon(
                            imageVector = if (isCopied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            contentDescription = if (isCopied) "Copied code" else "Copy code",
                            tint = if (isCopied) SuccessGreen else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(14.dp)
            ) {
                Text(
                    text = highlightCode(code),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = MonoFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun MarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ObsidianBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            // Header Row
            Row(
                modifier = Modifier
                    .background(ObsidianElevated)
                    .padding(vertical = 10.dp, horizontal = 8.dp)
            ) {
                headers.forEach { header ->
                    Text(
                        text = formatInlineMarkdown(header),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = accentColor,
                        modifier = Modifier
                            .width(140.dp)
                            .padding(horizontal = 8.dp)
                    )
                }
            }
            HorizontalDivider(color = ObsidianBorder, thickness = 1.dp)
            rows.forEachIndexed { idx, row ->
                Row(
                    modifier = Modifier
                        .background(
                            if (idx % 2 == 1) ObsidianSurfaceVariant.copy(alpha = 0.45f)
                            else Color.Transparent
                        )
                        .padding(vertical = 9.dp, horizontal = 8.dp)
                ) {
                    headers.indices.forEach { colIdx ->
                        val cell = row.getOrNull(colIdx).orEmpty()
                        Text(
                            text = formatInlineMarkdown(cell),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                            modifier = Modifier
                                .width(140.dp)
                                .padding(horizontal = 8.dp)
                        )
                    }
                }
                if (idx < rows.lastIndex) {
                    HorizontalDivider(color = ObsidianBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
private fun DataChartCard(spec: ChartSpec) {
    val progress = remember(spec) { Animatable(0f) }
    LaunchedEffect(spec) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(durationMillis = 750))
    }

    val maxVal = (spec.items.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(0.0001)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = ObsidianSurface,
        border = BorderStroke(1.dp, ModeAnalystColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = "Data chart",
                    tint = ModeAnalystColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                val count = spec.items.size
                if (count == 0) return@Canvas
                val leftPad = 8.dp.toPx()
                val rightPad = 8.dp.toPx()
                val topPad = 12.dp.toPx()
                val bottomPad = 10.dp.toPx()
                val chartW = (size.width - leftPad - rightPad).coerceAtLeast(1f)
                val chartH = (size.height - topPad - bottomPad).coerceAtLeast(1f)

                // Subtle horizontal gridlines
                for (g in 0..3) {
                    val y = topPad + chartH * (g / 3f)
                    drawLine(
                        color = ObsidianBorder.copy(alpha = 0.6f),
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                if (spec.type == "line") {
                    val stepX = if (count > 1) chartW / (count - 1) else chartW
                    val points = spec.items.mapIndexed { idx, pt ->
                        val norm = (pt.value / maxVal).toFloat().coerceIn(0f, 1f)
                        Offset(
                            x = leftPad + idx * stepX,
                            y = topPad + chartH * (1f - norm * progress.value)
                        )
                    }
                    val path = Path().apply {
                        points.forEachIndexed { idx, pt ->
                            if (idx == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                        }
                    }
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(listOf(ModeAnalystColor, CyanAccent)),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    points.forEach { pt ->
                        drawCircle(color = ObsidianSurface, radius = 6.dp.toPx(), center = pt)
                        drawCircle(color = ModeAnalystColor, radius = 4.dp.toPx(), center = pt)
                    }
                } else {
                    val slotW = chartW / count
                    val barW = (slotW * 0.54f).coerceAtMost(42.dp.toPx())
                    spec.items.forEachIndexed { idx, pt ->
                        val norm = (pt.value / maxVal).toFloat().coerceIn(0f, 1f)
                        val barH = (chartH * norm * progress.value).coerceAtLeast(3.dp.toPx())
                        val x = leftPad + idx * slotW + (slotW - barW) / 2f
                        val y = topPad + chartH - barH
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                listOf(CyanAccent, EmeraldPrimary)
                            ),
                            topLeft = Offset(x, y),
                            size = Size(barW, barH),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend / Axis Labels Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                spec.items.forEach { pt ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val valLabel = if (pt.value % 1.0 == 0.0) {
                            "${pt.value.toLong()}${spec.unit}"
                        } else {
                            "${String.format("%.1f", pt.value)}${spec.unit}"
                        }
                        Text(
                            text = valLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = CyanAccent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = pt.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private fun formatInlineMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        while (idx < text.length) {
            when {
                text.startsWith("**", idx) -> {
                    val end = text.indexOf("**", idx + 2)
                    if (end > idx) {
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = TextPrimary)) {
                            append(text.substring(idx + 2, end))
                        }
                        idx = end + 2
                    } else {
                        append("**")
                        idx += 2
                    }
                }
                text.startsWith("`", idx) -> {
                    val end = text.indexOf("`", idx + 1)
                    if (end > idx) {
                        withStyle(
                            SpanStyle(
                                fontFamily = MonoFontFamily,
                                color = CyanAccent,
                                background = ObsidianElevated
                            )
                        ) {
                            append(" ${text.substring(idx + 1, end)} ")
                        }
                        idx = end + 1
                    } else {
                        append("`")
                        idx += 1
                    }
                }
                text.startsWith("*", idx) && !text.startsWith("**", idx) -> {
                    val end = text.indexOf("*", idx + 1)
                    if (end > idx) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = TextPrimary)) {
                            append(text.substring(idx + 1, end))
                        }
                        idx = end + 1
                    } else {
                        append("*")
                        idx += 1
                    }
                }
                else -> {
                    append(text[idx])
                    idx++
                }
            }
        }
    }
}

private val codeKeywords = setOf(
    "fun", "val", "var", "class", "object", "interface", "data", "sealed", "enum",
    "return", "if", "else", "when", "for", "while", "try", "catch", "throw", "import",
    "package", "suspend", "private", "public", "override", "def", "async", "await",
    "const", "let", "function", "export", "SELECT", "FROM", "WHERE", "JOIN", "GROUP",
    "ORDER", "BY", "INSERT", "UPDATE", "CREATE", "TABLE", "INDEX", "true", "false", "null"
)

private fun highlightCode(code: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = code.split("\n")
        lines.forEachIndexed { lineIdx, line ->
            val trimmed = line.trimStart()
            if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("--")) {
                withStyle(SpanStyle(color = CodeComment, fontStyle = FontStyle.Italic)) {
                    append(line)
                }
            } else {
                var i = 0
                while (i < line.length) {
                    val ch = line[i]
                    if (ch == '"' || ch == '\'') {
                        val quote = ch
                        var j = i + 1
                        while (j < line.length && line[j] != quote) {
                            if (line[j] == '\\' && j + 1 < line.length) j++
                            j++
                        }
                        val end = (j + 1).coerceAtMost(line.length)
                        withStyle(SpanStyle(color = CodeString)) {
                            append(line.substring(i, end))
                        }
                        i = end
                    } else if (ch.isLetter() || ch == '_') {
                        var j = i + 1
                        while (j < line.length && (line[j].isLetterOrDigit() || line[j] == '_')) j++
                        val token = line.substring(i, j)
                        val color = when {
                            token in codeKeywords -> CodeKeyword
                            token.first().isUpperCase() -> CodeType
                            else -> TextPrimary
                        }
                        withStyle(SpanStyle(color = color)) {
                            append(token)
                        }
                        i = j
                    } else if (ch.isDigit()) {
                        var j = i + 1
                        while (j < line.length && (line[j].isDigit() || line[j] == '.')) j++
                        withStyle(SpanStyle(color = CodeNumber)) {
                            append(line.substring(i, j))
                        }
                        i = j
                    } else {
                        withStyle(SpanStyle(color = TextSecondary)) {
                            append(ch)
                        }
                        i++
                    }
                }
            }
            if (lineIdx < lines.lastIndex) append("\n")
        }
    }
}
