import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """        val clonedLayers = sourceLayers.map { originalLayer ->
            originalLayer.copy(
                id = java.util.UUID.randomUUID().toString()
            )
        }"""

replacement = """        val clonedLayers = sourceLayers.map { originalLayer ->
            originalLayer.copy(
                id = java.util.UUID.randomUUID().toString(),
                effects = originalLayer.effects.map { eff -> eff.duplicate() }
            )
        }"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
