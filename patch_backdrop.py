import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

# Let's add backdropBitmap parameter to drawLayerWithFullPipeline
def add_backdrop_param(content):
    target = "isRasterizing: Boolean = false\n) {"
    replacement = "isRasterizing: Boolean = false,\n    backdropBitmap: android.graphics.Bitmap? = null\n) {"
    return content.replace(target, replacement)

content = add_backdrop_param(content)

# Now, fix the currentBackdrop logic inside drawLayerWithFullPipeline
target_logic = """    val currentBackdrop = if (needsBackdrop && !isRasterizing) {
        // Backdrops are expensive during rasterize unless necessary, but we can pass null for now
        // since we didn't pass reusableBackdropBitmap here. 
        // For accurate rasterization, we might need a backdrop, but for now we skip.
        null
    } else { null }"""

replacement_logic = """    val currentBackdrop = if (needsBackdrop && !isRasterizing) {
        backdropBitmap
    } else { null }"""

content = content.replace(target_logic, replacement_logic)

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
print("Patched backdrop")
