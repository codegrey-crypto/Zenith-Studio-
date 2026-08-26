import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

def replacer(match):
    return """        if (needsRestore) {
            originalCanvas.restore()
            if (offsetX != 0f || offsetY != 0f) {
                originalCanvas.restore()
            }
        } else if (offsetX != 0f || offsetY != 0f) {
            originalCanvas.restore()
        }
    }"""

pattern = re.compile(r'        if \(needsRestore\) \{.*?    \}', re.DOTALL)
new_content = pattern.sub(replacer, content, count=1)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(new_content)

