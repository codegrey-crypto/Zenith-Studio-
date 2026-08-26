import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

target = "backdropBitmap: android.graphics.Bitmap? = null\n) {"
replacement = "backdropProvider: (() -> android.graphics.Bitmap?)? = null\n) {"
content = content.replace(target, replacement)

target_logic = """    val currentBackdrop = if (needsBackdrop && !isRasterizing) {
        backdropBitmap
    } else { null }"""

replacement_logic = """    val currentBackdrop = if (needsBackdrop && !isRasterizing) {
        backdropProvider?.invoke()
    } else { null }"""

content = content.replace(target_logic, replacement_logic)

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
print("Patched backdrop provider")
