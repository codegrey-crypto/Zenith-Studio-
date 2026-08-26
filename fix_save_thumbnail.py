import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """                        // Force save of workspace immediately on exit
                        workspaceViewModel.saveWorkspace(
                            id = projectId,
                            name = projectName,
                            width = canvasWidth,
                            height = canvasHeight,
                            artboards = currentMemento.artboards,
                            selectedArtboardId = selectedArtboardId,
                            dpi = projectDpi
                        )"""

replacement = """                        // Force save of workspace immediately on exit
                        workspaceViewModel.saveWorkspace(
                            id = projectId,
                            name = projectName,
                            width = canvasWidth,
                            height = canvasHeight,
                            artboards = currentMemento.artboards,
                            selectedArtboardId = selectedArtboardId,
                            dpi = projectDpi
                        )
                        // Save a thumbnail
                        try {
                            val bitmap = exportCanvasToBitmap(
                                context = context,
                                canvasWidth = canvasWidth,
                                canvasHeight = canvasHeight,
                                layers = currentMemento.artboards.find { it.id == selectedArtboardId }?.layers ?: emptyList(),
                                imageBitmapCache = imageBitmapCache,
                                targetWidth = 1080f,
                                targetHeight = 1350f,
                                isBackgroundTransparent = false
                            )
                            val thumbFile = java.io.File(context.filesDir, "thumb_${projectId}.png")
                            java.io.FileOutputStream(thumbFile).use { out ->
                                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 80, out)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
