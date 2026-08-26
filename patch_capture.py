import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """                                        val newElement = element.copy(id = java.util.UUID.randomUUID().toString())
                                        layers = layers + newElement
                                        projectHistoryManager.captureState(projectId, createMemento())
                                        showElementsImportDialog = false"""

replacement = """                                        undoStack.add(layers)
                                        redoStack.clear()
                                        val newElement = element.copy(id = java.util.UUID.randomUUID().toString())
                                        layers = layers + newElement
                                        showElementsImportDialog = false"""

text = text.replace(target, replacement)
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
