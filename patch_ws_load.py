import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """                        // Safe sandbox fallback to primary Room records if the transaction log doesn't exist yet
                        scope.launch {
                            val (arts, activeId) = withContext(Dispatchers.Default) {
                                LayerSerializer.deserializeWorkspace(proj.layersJson, proj.width, proj.height)
                            }"""

replacement = """                        // Safe sandbox fallback to primary Room records if the transaction log doesn't exist yet
                        scope.launch {
                            val fullProj = workspaceViewModel.getProjectById(proj.id) ?: proj
                            val (arts, activeId) = withContext(Dispatchers.Default) {
                                LayerSerializer.deserializeWorkspace(fullProj.layersJson, fullProj.width, fullProj.height)
                            }"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
