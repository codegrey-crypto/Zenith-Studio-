import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

target = r"val nativeTextPath = android\.graphics\.Path\(\)\s*for \(line in 0 until staticLayout\.lineCount\) \{\s*val lineStart = staticLayout\.getLineStart\(line\)\s*val lineEnd = staticLayout\.getLineEnd\(line\)\s*val lineX = staticLayout\.getLineLeft\(line\)\s*val lineY = startY \+ staticLayout\.getLineBaseline\(line\)\s*val linePath = android\.graphics\.Path\(\)\s*val str = textFormatted\.subSequence\(lineStart, lineEnd\)\.toString\(\)\s*if \(str\.isNotEmpty\(\)\) \{\s*textPaint\.getTextPath\(str, 0, str\.length, lineX, lineY, linePath\)\s*nativeTextPath\.addPath\(linePath\)\s*\}\s*\}\s*this\.asAndroidPath\(\)\.addPath\(nativeTextPath\)"

match = re.search(target, content)
if match:
    print("Found another occurrence!")
else:
    print("No other occurrences found")
