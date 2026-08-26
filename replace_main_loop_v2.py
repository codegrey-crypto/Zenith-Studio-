import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

start_marker = "val clippedLayers = mutableListOf<com.example.studio.model.StudioLayer>()"
start_idx = content.find(start_marker)

if start_idx == -1:
    print("Start marker not found")
    exit(1)

# Before we replace, let's look at what is above start_idx
print(content[start_idx-200:start_idx])

# The main loop has `val hasAdjustmentEffect` between clippedLayers block and if (clippedLayers.isNotEmpty())? No, it's AFTER the clippedLayers block.
# Let's see what is after the clippedLayers block.
idx2 = content.find("val hasAdjustmentEffect", start_idx)
print(content[start_idx:idx2])
