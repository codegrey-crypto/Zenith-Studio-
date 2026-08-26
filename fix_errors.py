import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('androidx.compose.ui.graphics.asImageBitmap(tempBmp)', 'tempBmp.asImageBitmap()')

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)
