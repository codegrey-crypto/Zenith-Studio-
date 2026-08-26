import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target_list = 'listOf("Create Canvas", "Previous Projects", "Import Layout", "Settings")'
replacement_list = 'listOf("Create Canvas", "Previous Projects", "Elements", "Import Layout", "Settings")'

target_icons = """                                imageVector = when (tabIdx) {
                                    0 -> Icons.Default.AddCircleOutline
                                    1 -> Icons.Default.FolderSpecial
                                    2 -> Icons.Default.FileOpen
                                    else -> Icons.Default.Settings
                                }"""

replacement_icons = """                                imageVector = when (tabIdx) {
                                    0 -> Icons.Default.AddCircleOutline
                                    1 -> Icons.Default.FolderSpecial
                                    2 -> Icons.Default.Extension
                                    3 -> Icons.Default.FileOpen
                                    else -> Icons.Default.Settings
                                }"""

text = text.replace(target_list, replacement_list)
text = text.replace(target_icons, replacement_icons)

text = text.replace('} else if (activeMenuTab == 2) {', '} else if (activeMenuTab == 3) {')
text = text.replace('} else if (activeMenuTab == 3) { // This will now match Settings if I replaced correctly but let\'s be careful', '')

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
