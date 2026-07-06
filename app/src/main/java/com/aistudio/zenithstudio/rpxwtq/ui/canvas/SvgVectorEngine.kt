package com.aistudio.zenithstudio.rpxwtq.ui.canvas

import android.content.Context
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

enum class SvgNodeType { MOVE, LINE, CUBIC_BEZIER, CLOSE }

data class ParsedPathCommand(
    val type: SvgNodeType,
    val points: List<Offset>,
    val fillColor: Color? = null,
    val strokeColor: Color? = null,
    val strokeWidth: Float? = null
)

data class SvgStyle(
    val fill: String?,
    val stroke: String?,
    val strokeWidth: String?
)

class SvgVectorEngine(private val context: Context) {

    private fun extractAttribute(attributes: String, attrName: String): String? {
        val regex = "$attrName=\"([^\"]+)\"".toRegex(RegexOption.IGNORE_CASE)
        val match = regex.find(attributes)
        if (match != null) return match.groupValues[1]
        
        val singleQuoteRegex = "$attrName='([^']+)'".toRegex(RegexOption.IGNORE_CASE)
        val singleMatch = singleQuoteRegex.find(attributes)
        if (singleMatch != null) return singleMatch.groupValues[1]
        
        return null
    }

    private fun extractFromStyle(style: String, key: String): String? {
        val regex = "$key\\s*:\\s*([^;\\s]+)".toRegex(RegexOption.IGNORE_CASE)
        return regex.find(style)?.groupValues?.get(1)
    }

    private fun parseSvgColor(colorStr: String?): Color? {
        if (colorStr == null) return null
        val clean = colorStr.trim().lowercase()
        if (clean == "none" || clean == "transparent") return Color.Transparent
        
        if (clean.startsWith("#")) {
            val hex = clean.substring(1)
            return try {
                when (hex.length) {
                    3 -> {
                        val r = hex[0].toString().repeat(2).toInt(16)
                        val g = hex[1].toString().repeat(2).toInt(16)
                        val b = hex[2].toString().repeat(2).toInt(16)
                        Color(r, g, b, 255)
                    }
                    4 -> {
                        val r = hex[0].toString().repeat(2).toInt(16)
                        val g = hex[1].toString().repeat(2).toInt(16)
                        val b = hex[2].toString().repeat(2).toInt(16)
                        val a = hex[3].toString().repeat(2).toInt(16)
                        Color(r, g, b, a)
                    }
                    6 -> {
                        val r = hex.substring(0, 2).toInt(16)
                        val g = hex.substring(2, 4).toInt(16)
                        val b = hex.substring(4, 6).toInt(16)
                        Color(r, g, b, 255)
                    }
                    8 -> {
                        val r = hex.substring(0, 2).toInt(16)
                        val g = hex.substring(2, 4).toInt(16)
                        val b = hex.substring(4, 6).toInt(16)
                        val a = hex.substring(6, 8).toInt(16)
                        Color(r, g, b, a)
                    }
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
        
        if (clean.startsWith("rgb")) {
            try {
                val content = clean.substringAfter("(").substringBefore(")")
                val parts = content.split(",").map { it.trim() }
                if (parts.size >= 3) {
                    val r = parts[0].toIntOrNull() ?: 0
                    val g = parts[1].toIntOrNull() ?: 0
                    val b = parts[2].toIntOrNull() ?: 0
                    val alpha = if (parts.size >= 4) {
                        val aVal = parts[3].toFloatOrNull() ?: 1.0f
                        (aVal * 255).toInt().coerceIn(0, 255)
                    } else {
                        255
                    }
                    return Color(r, g, b, alpha)
                }
            } catch (e: Exception) {
                // ignore and fallback
            }
        }
        
        return when (clean) {
            "red" -> Color.Red
            "green" -> Color.Green
            "blue" -> Color.Blue
            "black" -> Color.Black
            "white" -> Color.White
            "yellow" -> Color.Yellow
            "cyan" -> Color.Cyan
            "magenta" -> Color.Magenta
            "gray", "grey" -> Color.Gray
            "darkgray", "darkgrey" -> Color.DarkGray
            "lightgray", "lightgrey" -> Color.LightGray
            "orange" -> Color(0xFFFF9800)
            "pink" -> Color(0xFFE91E63)
            "purple" -> Color(0xFF9C27B0)
            "brown" -> Color(0xFF795548)
            "gold" -> Color(0xFFFFD700)
            "silver" -> Color(0xFFC0C0C0)
            "lime" -> Color(0xFF00FF00)
            "maroon" -> Color(0xFF800000)
            "navy" -> Color(0xFF000080)
            "olive" -> Color(0xFF808000)
            "teal" -> Color(0xFF008080)
            else -> null
        }
    }

    /**
     * Non-destructively parses an incoming SVG stream into structural command tokens.
     */
    suspend fun importSvgFromUri(uri: Uri): List<ParsedPathCommand> = withContext(Dispatchers.IO) {
        val commandList = mutableListOf<ParsedPathCommand>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val fullSvgText = reader.readText()
                    
                    // Match g tags, closing g tags, or path tags sequentially
                    val tagRegex = "<(g|path|/g)\\b([^>]*)>".toRegex(RegexOption.IGNORE_CASE)
                    val styleStack = java.util.Stack<SvgStyle>()
                    
                    val matches = tagRegex.findAll(fullSvgText)
                    for (match in matches) {
                        val tagName = match.groupValues[1].lowercase()
                        val attributes = match.groupValues[2]
                        
                        when (tagName) {
                            "g" -> {
                                val fill = extractAttribute(attributes, "fill") ?: extractFromStyle(attributes, "style")?.let { extractFromStyle(it, "fill") }
                                val stroke = extractAttribute(attributes, "stroke") ?: extractFromStyle(attributes, "style")?.let { extractFromStyle(it, "stroke") }
                                val strokeWidth = extractAttribute(attributes, "stroke-width") ?: extractFromStyle(attributes, "style")?.let { extractFromStyle(it, "stroke-width") }
                                
                                val parent = if (styleStack.isNotEmpty()) styleStack.peek() else null
                                val resolvedFill = fill ?: parent?.fill
                                val resolvedStroke = stroke ?: parent?.stroke
                                val resolvedStrokeWidth = strokeWidth ?: parent?.strokeWidth
                                
                                styleStack.push(SvgStyle(resolvedFill, resolvedStroke, resolvedStrokeWidth))
                            }
                            "/g" -> {
                                if (styleStack.isNotEmpty()) {
                                    styleStack.pop()
                                }
                            }
                            "path" -> {
                                val d = extractAttribute(attributes, "d") ?: extractFromStyle(attributes, "style")?.let { extractFromStyle(it, "d") }
                                if (d != null) {
                                    val fill = extractAttribute(attributes, "fill") ?: extractFromStyle(attributes, "style")?.let { extractFromStyle(it, "fill") }
                                    val stroke = extractAttribute(attributes, "stroke") ?: extractFromStyle(attributes, "style")?.let { extractFromStyle(it, "stroke") }
                                    val strokeWidth = extractAttribute(attributes, "stroke-width") ?: extractFromStyle(attributes, "style")?.let { extractFromStyle(it, "stroke-width") }
                                    
                                    val activeGroup = if (styleStack.isNotEmpty()) styleStack.peek() else null
                                    val resolvedFill = fill ?: activeGroup?.fill
                                    val resolvedStroke = stroke ?: activeGroup?.stroke
                                    val resolvedStrokeWidth = strokeWidth ?: activeGroup?.strokeWidth
                                    
                                    val fillColor = parseSvgColor(resolvedFill)
                                    val strokeColor = parseSvgColor(resolvedStroke)
                                    val parsedStrokeWidth = resolvedStrokeWidth?.replace("px", "")?.trim()?.toFloatOrNull()
                                    
                                    val pathCommands = parsePathDataString(d)
                                    for (cmd in pathCommands) {
                                        commandList.add(
                                            cmd.copy(
                                                fillColor = fillColor,
                                                strokeColor = strokeColor,
                                                strokeWidth = parsedStrokeWidth
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (commandList.isEmpty()) {
                        // Fallback parsing d="..." globally
                        val pathDataRegex = "d=\"([^\"]+)\"".toRegex()
                        val plainMatches = pathDataRegex.findAll(fullSvgText)
                        
                        for (match in plainMatches) {
                            val pathString = match.groupValues[1]
                            commandList.addAll(parsePathDataString(pathString))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Graceful error isolation fallback loop to prevent rendering thread crashes
            e.printStackTrace()
        }
        
        return@withContext commandList
    }

    /**
     * Serializes current vector node states into a production-ready SVG text string.
     */
    fun exportCanvasToSvgString(width: Int, height: Int, commands: List<ParsedPathCommand>): String {
        val sb = StringBuilder()
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$width\" height=\"$height\" viewBox=\"0 0 $width $height\">\n")
        sb.append("  <path d=\"")
        
        for (cmd in commands) {
            when (cmd.type) {
                SvgNodeType.MOVE -> sb.append("M ${cmd.points[0].x} ${cmd.points[0].y} ")
                SvgNodeType.LINE -> sb.append("L ${cmd.points[0].x} ${cmd.points[0].y} ")
                SvgNodeType.CUBIC_BEZIER -> {
                    sb.append("C ${cmd.points[0].x} ${cmd.points[0].y}, ${cmd.points[1].x} ${cmd.points[1].y}, ${cmd.points[2].x} ${cmd.points[2].y} ")
                }
                SvgNodeType.CLOSE -> sb.append("Z ")
            }
        }
        
        sb.append("\" fill=\"none\" stroke=\"#FFFFFF\" stroke-width=\"3\"/>\n")
        sb.append("</svg>")
        return sb.toString()
    }

    private fun parsePathDataString(pathData: String): List<ParsedPathCommand> {
        val commands = mutableListOf<ParsedPathCommand>()
        
        // Regex to find commands and numbers
        val tokenRegex = "([MmLlCcZz])|(-?[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?)".toRegex()
        val tokens = tokenRegex.findAll(pathData).map { it.value }.toList()
        
        var currentX = 0f
        var currentY = 0f
        var i = 0
        
        while (i < tokens.size) {
            val token = tokens[i]
            if (token.length == 1 && token[0].isLetter()) {
                val cmdChar = token[0]
                i++
                
                when (cmdChar) {
                    'M', 'm' -> {
                        while (i + 1 < tokens.size && !tokens[i][0].isLetter()) {
                            val xVal = tokens[i].toFloatOrNull() ?: 0f
                            val yVal = tokens[i+1].toFloatOrNull() ?: 0f
                            i += 2
                            
                            if (cmdChar == 'm') {
                                currentX += xVal
                                currentY += yVal
                            } else {
                                currentX = xVal
                                currentY = yVal
                            }
                            commands.add(ParsedPathCommand(SvgNodeType.MOVE, listOf(Offset(currentX, currentY))))
                        }
                    }
                    'L', 'l' -> {
                        while (i + 1 < tokens.size && !tokens[i][0].isLetter()) {
                            val xVal = tokens[i].toFloatOrNull() ?: 0f
                            val yVal = tokens[i+1].toFloatOrNull() ?: 0f
                            i += 2
                            
                            if (cmdChar == 'l') {
                                currentX += xVal
                                currentY += yVal
                            } else {
                                currentX = xVal
                                currentY = yVal
                            }
                            commands.add(ParsedPathCommand(SvgNodeType.LINE, listOf(Offset(currentX, currentY))))
                        }
                    }
                    'C', 'c' -> {
                        while (i + 5 < tokens.size && !tokens[i][0].isLetter()) {
                            val cp1x = tokens[i].toFloatOrNull() ?: 0f
                            val cp1y = tokens[i+1].toFloatOrNull() ?: 0f
                            val cp2x = tokens[i+2].toFloatOrNull() ?: 0f
                            val cp2y = tokens[i+3].toFloatOrNull() ?: 0f
                            val destx = tokens[i+4].toFloatOrNull() ?: 0f
                            val desty = tokens[i+5].toFloatOrNull() ?: 0f
                            i += 6
                            
                            val pt1: Offset
                            val pt2: Offset
                            val pt3: Offset
                            
                            if (cmdChar == 'c') {
                                pt1 = Offset(currentX + cp1x, currentY + cp1y)
                                pt2 = Offset(currentX + cp2x, currentY + cp2y)
                                pt3 = Offset(currentX + destx, currentY + desty)
                                currentX += destx
                                currentY += desty
                            } else {
                                pt1 = Offset(cp1x, cp1y)
                                pt2 = Offset(cp2x, cp2y)
                                pt3 = Offset(destx, desty)
                                currentX = destx
                                currentY = desty
                            }
                            commands.add(ParsedPathCommand(SvgNodeType.CUBIC_BEZIER, listOf(pt1, pt2, pt3)))
                        }
                    }
                    'Z', 'z' -> {
                        commands.add(ParsedPathCommand(SvgNodeType.CLOSE, emptyList()))
                    }
                }
            } else {
                i++
            }
        }
        
        return commands
    }
}
