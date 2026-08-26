import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """                    val localContext = androidx.compose.ui.platform.LocalContext.current
                var savedElements by remember { mutableStateOf<List<com.example.studio.model.StudioLayer>>(emptyList()) }"""

replacement = """                val localContext = androidx.compose.ui.platform.LocalContext.current
                var savedElements by remember { mutableStateOf<List<com.example.studio.model.StudioLayer>>(emptyList()) }"""

text = text.replace(target, replacement)
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)

