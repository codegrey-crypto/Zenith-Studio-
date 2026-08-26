with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

target = """                        val totalHeight = staticLayout.height
                        val startY = maxOf(0f, (layer.height - totalHeight) / 2f)

                        val nativeTextPath = android.graphics.Path()
                        for (line in 0 until staticLayout.lineCount) {
                            val lineStart = staticLayout.getLineStart(line)
                            val lineEnd = staticLayout.getLineEnd(line)
                            val lineX = staticLayout.getLineLeft(line)
                            val lineY = startY + staticLayout.getLineBaseline(line)
                            val linePath = android.graphics.Path()
                            val str = textFormatted.subSequence(lineStart, lineEnd).toString()
                            if (str.isNotEmpty()) {
                                textPaint.getTextPath(str, 0, str.length, lineX, lineY, linePath)
                                nativeTextPath.addPath(linePath)
                            }
                        }
                        this.asAndroidPath().addPath(nativeTextPath)"""

replacement = """                        val totalHeight = staticLayout.height
                        val startY = maxOf(0f, (layer.height - totalHeight) / 2f)

                        val nativeTextPath = com.example.studio.ui.TextLayoutCache.getPath(cacheKey) {
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

if target in content:
    content = content.replace(target, replacement)
    print("Success")
    with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
        f.write(content)
else:
    print("Target not found")
