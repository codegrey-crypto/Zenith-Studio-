import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

# Replace paint allocation in drawGeometryWithBlurAndOffset
def replacer(match):
    return """        val paint = ComposePaintCache.getCleanPaint().apply {
            color = finalComposeColor"""

pattern = re.compile(r'        val paint = androidx\.compose\.ui\.graphics\.Paint\(\)\.apply \{\n            color = finalComposeColor')

new_content = pattern.sub(replacer, content)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(new_content)
