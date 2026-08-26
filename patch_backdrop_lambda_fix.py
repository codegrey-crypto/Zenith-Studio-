import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

target = """                                                existingBitmap = reusableBackdropBitmap,
                                                totalScale = totalScale,
                                                warpMeshDivisionX = warpMeshDivisionX,"""

replacement = """                                                existingBitmap = reusableBackdropBitmap,
                                                warpMeshDivisionX = warpMeshDivisionX,"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
print("Patched backdrop lambda (removed totalScale)")
