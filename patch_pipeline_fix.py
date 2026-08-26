import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

content = content.replace("warpInterpolation: String = \"Linear\",", "warpInterpolation: Boolean = true,")
content = content.replace("warpTarget = warpTarget, warpMeshDivisionX = warpMeshDivisionX, warpMeshDivisionY = warpMeshDivisionY,",
                          "warpTarget = warpTarget, warpMeshDivisionX = warpMeshDivisionX, warpMeshDivisionY = warpMeshDivisionY, isExporting = isRasterizing,")

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
print("Fixed compile errors in drawLayerWithFullPipeline")
