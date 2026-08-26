import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

target = r"val nativeTextPath = android\.graphics\.Path\(\)\s*for \(line in 0 until staticLayout\.lineCount\) \{\s*val lineStart = staticLayout\.getLineStart\(line\)\s*val lineEnd = staticLayout\.getLineEnd\(line\)\s*val lineX = staticLayout\.getLineLeft\(line\)\s*val lineY = startY \+ staticLayout\.getLineBaseline\(line\)\s*val linePath = android\.graphics\.Path\(\)\s*val str = textFormatted\.subSequence\(lineStart, lineEnd\)\.toString\(\)\s*if \(str\.isNotEmpty\(\)\) \{\s*textPaint\.getTextPath\(str, 0, str\.length, lineX, lineY, linePath\)\s*nativeTextPath\.addPath\(linePath\)\s*\}\s*\}\s*this\.asAndroidPath\(\)\.addPath\(nativeTextPath\)"

replacement = """val nativeTextPath = com.example.studio.ui.TextLayoutCache.getPath(cacheKey) {
                            val path = android.graphics.Path()
                            for (line in 0 until staticLayout.lineCount) {
                                val lineStart = staticLayout.getLineStart(line)
                                val lineEnd = staticLayout.getLineEnd(line)
                                val lineX = staticLayout.getLineLeft(line)
                                val lineY = startY + staticLayout.getLineBaseline(line)
                                val linePath = android.graphics.Path()
                                val str = textFormatted.subSequence(lineStart, lineEnd).toString()
                                if (str.isNotEmpty()) {
                                    textPaint.getTextPath(str, 0, str.length, lineX, lineY, linePath)
                                    path.addPath(linePath)
                                }
                            }
                            path
                        }
                        this.asAndroidPath().addPath(nativeTextPath)"""

match = re.search(target, content)
if match:
    content = content[:match.start()] + replacement + content[match.end():]
    print("Success Regex")
    with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
        f.write(content)
else:
    print("Target not found with regex")
