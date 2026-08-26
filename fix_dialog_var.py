import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """    var showImportOptionMenu by remember { mutableStateOf(false) }"""
replacement = """    var showImportOptionMenu by remember { mutableStateOf(false) }
    var showElementsImportDialog by remember { mutableStateOf(false) }"""

text = text.replace(target, replacement)
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
