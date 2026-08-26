with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

import re

old_padding = """            val shelfBottomPadding = if (controlBarPosition == "Bottom") {
                if (workspaceLayoutMode == "Mobile" && isMobileBottomDockVisible) 84.dp
                else if (isBottomPanelVisible || activeFullScreenSheet != null) 0.dp
                else 12.dp
            } else 0.dp"""

new_padding = """            val shelfBottomPadding = if (controlBarPosition == "Bottom") {
                val hasLayerPanel = selectedLayer != null && activeFullScreenSheet == null
                val extraLayerPad = if (hasLayerPanel && !isLandscape) 64.dp else 0.dp
                if (workspaceLayoutMode == "Mobile" && isMobileBottomDockVisible) 84.dp + extraLayerPad
                else if (isBottomPanelVisible || activeFullScreenSheet != null) 0.dp
                else 12.dp + extraLayerPad
            } else 0.dp"""

if old_padding in content:
    content = content.replace(old_padding, new_padding)
    with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
        f.write(content)
    print("Padding fixed")
else:
    print("Padding not found")
