import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

def replacer_oval(match):
    return """            com.example.studio.model.LayerType.VECTOR_OVAL -> {
                val cacheKey = getLayerGeometryHash(layer)
                var path = pathCache.get(cacheKey)
                if (path == null) {
                    path = androidx.compose.ui.graphics.Path().apply {
                        addOval(androidx.compose.ui.geometry.Rect(0f, 0f, layer.width, layer.height))
                    }
                    pathCache.put(cacheKey, path)
                }
                drawContext.canvas.drawPath(path, paint)
            }"""

pattern_oval = re.compile(r'            com\.example\.studio\.model\.LayerType\.VECTOR_OVAL -> \{\n                val path = androidx\.compose\.ui\.graphics\.Path\(\)\.apply \{\n                    addOval\(androidx\.compose\.ui\.geometry\.Rect\(0f, 0f, layer\.width, layer\.height\)\)\n                \}\n                drawContext\.canvas\.drawPath\(path, paint\)\n            \}')
content = pattern_oval.sub(replacer_oval, content)

def replacer_line(match):
    return """            com.example.studio.model.LayerType.VECTOR_LINE -> {
                val cacheKey = getLayerGeometryHash(layer)
                var path = pathCache.get(cacheKey)
                if (path == null) {
                    val px = layer.width
                    val py = layer.height
                    path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 0f)
                        lineTo(px, py)
                    }
                    pathCache.put(cacheKey, path)
                }
                drawContext.canvas.drawPath(path, paint)
            }"""

pattern_line = re.compile(r'            com\.example\.studio\.model\.LayerType\.VECTOR_LINE -> \{\n                val px = layer\.width\n                val py = layer\.height\n                val path = androidx\.compose\.ui\.graphics\.Path\(\)\.apply \{\n                    moveTo\(0f, 0f\)\n                    lineTo\(px, py\)\n                \}\n                drawContext\.canvas\.drawPath\(path, paint\)\n            \}')
content = pattern_line.sub(replacer_line, content)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)
