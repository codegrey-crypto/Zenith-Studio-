import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """        val clonedLayers = sourceLayers.map { originalLayer ->
            originalLayer.copy(
                id = java.util.UUID.randomUUID().toString(),
                effects = originalLayer.effects.map { eff -> eff.duplicate() }
            )
        }"""

replacement = """        val clonedLayers = sourceLayers.map { originalLayer ->
            val newLayerId = java.util.UUID.randomUUID().toString()
            com.aistudio.zenithstudio.rpxwtq.EffectStackManager.duplicateFiltersForLayer(originalLayer.id, newLayerId)
            originalLayer.copy(
                id = newLayerId,
                effects = originalLayer.effects.map { eff -> eff.duplicate() }
            )
        }"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
