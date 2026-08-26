with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

import re

old_pad = """            val shelfBottomPadding = if (controlBarPosition == "Bottom") {
                val hasLayerPanel = selectedLayer != null && activeFullScreenSheet == null
                val extraLayerPad = if (hasLayerPanel && !isLandscape) 64.dp else 0.dp
                if (workspaceLayoutMode == "Mobile" && isMobileBottomDockVisible) 84.dp + extraLayerPad
                else if (isBottomPanelVisible || activeFullScreenSheet != null) 0.dp
                else 12.dp + extraLayerPad
            } else 0.dp"""

new_pad = """            val shelfBottomPadding = if (controlBarPosition == "Bottom") {
                var pad = 12.dp
                if (workspaceLayoutMode == "Mobile" && isMobileBottomDockVisible) pad += 80.dp
                if (isBottomPanelVisible && activeFullScreenSheet == null && selectedLayer != null && !isLandscape) pad += 68.dp
                if (activeFullScreenSheet != null && !isLandscape) {
                    pad += if (activeFullScreenSheet == "Ruler" || activeFullScreenSheet == "Grid") 260.dp else 400.dp
                }
                pad
            } else 0.dp"""

content = content.replace(old_pad, new_pad)

old_header = """                                        "Brush" -> "Paint Brush Studio Settings"
                                        "Grid" -> "Grid Alignment Calibration"
                                        "Ruler" -> "Ruler Guideline Calibration"
                                        else -> "Border & Shadows Studio"
                                    }
                                    Text(headerText, style = Typography.titleMedium, color = EnergeticYellow, fontWeight = FontWeight.Bold)"""

new_header = """                                        "Brush" -> if (activeTool == "Pen") "Bezier Pen Studio" else "Paint Brush Studio Settings"
                                        "Grid" -> "Grid Alignment Calibration"
                                        "Ruler" -> "Ruler Guideline Calibration"
                                        else -> "Border & Shadows Studio"
                                    }
                                    Text(headerText, style = Typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)"""

content = content.replace(old_header, new_header)

content = content.replace('Icon(Icons.Default.ArrowBack, "Back", tint = EnergeticYellow', 'Icon(Icons.Default.ArrowBack, "Back", tint = MaterialTheme.colorScheme.primary')

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)
print("Workspace fixed")
