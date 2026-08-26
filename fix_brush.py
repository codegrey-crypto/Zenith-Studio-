import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

target = """            com.example.studio.model.LayerType.IMAGE_CARD -> {
                if (layer.cornerRadius > 0f) {
                    val effR = layer.cornerRadius
                    drawContext.canvas.drawRoundRect(0f, 0f, layer.width, layer.height, effR, effR, paint)
                } else {
                    drawContext.canvas.drawRect(0f, 0f, layer.width, layer.height, paint)
                }
            }"""

replacement = """            com.example.studio.model.LayerType.IMAGE_CARD -> {
                if (layer.cornerRadius > 0f) {
                    val effR = layer.cornerRadius
                    drawRoundRect(
                        brush = finalBrush,
                        topLeft = androidx.compose.ui.geometry.Offset.Zero,
                        size = androidx.compose.ui.geometry.Size(layer.width, layer.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(effR, effR),
                        style = fillStyle,
                        alpha = brushAlpha,
                        blendMode = composeBlendMode
                    )
                } else {
                    drawRect(
                        brush = finalBrush,
                        topLeft = androidx.compose.ui.geometry.Offset.Zero,
                        size = androidx.compose.ui.geometry.Size(layer.width, layer.height),
                        style = fillStyle,
                        alpha = brushAlpha,
                        blendMode = composeBlendMode
                    )
                }
            }"""

parts = content.split(target)
if len(parts) >= 3:
    # the second occurrence is in drawGeometryWithBrush
    new_content = parts[0] + target + parts[1] + replacement + parts[2]
    # In case there are more occurrences
    for i in range(3, len(parts)):
        new_content += target + parts[i]
    with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
        f.write(new_content)
else:
    print("Could not find 2 occurrences!")

