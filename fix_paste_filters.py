import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """                                    val pastedLayer = copied.deepCloneForClipboard().copy(
                                        id = UUID.randomUUID().toString(),
                                        name = "${copied.name} (Pasted)",
                                        positionX = newPosRefX,
                                        positionY = newPosRefY
                                    )
                                    layers = listOf(pastedLayer) + layers"""

replacement = """                                    val newPastedId = UUID.randomUUID().toString()
                                    val pastedLayer = copied.deepCloneForClipboard().copy(
                                        id = newPastedId,
                                        name = "${copied.name} (Pasted)",
                                        positionX = newPosRefX,
                                        positionY = newPosRefY
                                    )
                                    com.aistudio.zenithstudio.rpxwtq.EffectStackManager.duplicateFiltersForLayer(copied.id, newPastedId)
                                    layers = listOf(pastedLayer) + layers"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
