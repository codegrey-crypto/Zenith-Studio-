import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """                                    val copy = orig.copy(
                                        id = UUID.randomUUID().toString(),
                                        name = "${orig.name} (Copy)",
                                        positionX = orig.positionX + 40f,
                                        positionY = orig.positionY + 40f
                                    )"""

if target in text:
    print("Target found")
else:
    print("Target not found")
