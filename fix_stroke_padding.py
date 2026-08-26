import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

def replacer(match):
    return """        if (isHardware && blurRadius > 0.1f && layer.width in 1f..3000f && layer.height in 1f..3000f) {
            val strokePad = if (layer.strokeThickness > 0f) layer.strokeThickness * 2f else 0f
            val pad = ((blurRadius * 3) + strokePad).toInt().coerceIn(1, 400)
            val bmpW = (layer.width + pad * 2).toInt().coerceAtLeast(1)
            val bmpH = (layer.height + pad * 2).toInt().coerceAtLeast(1)"""

pattern = re.compile(r'        if \(isHardware && blurRadius > 0\.1f && layer\.width in 1f\.\.3000f && layer\.height in 1f\.\.3000f\) \{\n            val pad = \(blurRadius \* 3\)\.toInt\(\)\.coerceIn\(1, 400\)\n            val bmpW = \(layer\.width \+ pad \* 2\)\.toInt\(\)\.coerceAtLeast\(1\)\n            val bmpH = \(layer\.height \+ pad \* 2\)\.toInt\(\)\.coerceAtLeast\(1\)')
new_content = pattern.sub(replacer, content)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(new_content)

