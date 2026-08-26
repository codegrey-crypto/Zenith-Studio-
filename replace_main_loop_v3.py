import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

start_marker = "val clippedLayers = mutableListOf<com.example.studio.model.StudioLayer>()"
start_idx = content.find(start_marker)
print(f"Start idx: {start_idx}")

# Also replace the duplicate block in generateExportBitmap!
