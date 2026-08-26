import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

target = """                                        backdropProvider = {
                                            val cWidth = canvasWidth.toInt().coerceAtLeast(1)
                                            val cHeight = canvasHeight.toInt().coerceAtLeast(1)
                                            if (reusableBackdropBitmap == null || reusableBackdropBitmap!!.width != cWidth || reusableBackdropBitmap!!.height != cHeight) {
                                                reusableBackdropBitmap = android.graphics.Bitmap.createBitmap(cWidth, cHeight, android.graphics.Bitmap.Config.ARGB_8888)
                                            }
                                            com.example.studio.ui.GraphicsUtils.extractBackdrop(this, reusableBackdropBitmap!!)
                                        }"""

replacement = """                                        backdropProvider = {
                                            val computedBackdrop = generateBackdropForLayer(
                                                layers = layers,
                                                currentIndex = layers.indexOfFirst { it.id == layer.id },
                                                canvasWidth = canvasWidth,
                                                canvasHeight = canvasHeight,
                                                imageBitmapCache = imageBitmapCache,
                                                pathCache = pathCache,
                                                pathPointsCountCache = pathPointsCountCache,
                                                existingBitmap = reusableBackdropBitmap,
                                                totalScale = totalScale,
                                                warpMeshDivisionX = warpMeshDivisionX,
                                                warpMeshDivisionY = warpMeshDivisionY,
                                                warpRepeatMode = warpRepeatMode,
                                                warpRepeatX = warpRepeatX,
                                                warpRepeatY = warpRepeatY,
                                                warpPhaseX = warpPhaseX,
                                                warpPhaseY = warpPhaseY,
                                                warpInterpolation = warpInterpolation,
                                                warpTarget = warpTarget
                                            )
                                            if (computedBackdrop != null) {
                                                reusableBackdropBitmap = computedBackdrop
                                            }
                                            computedBackdrop
                                        }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
print("Patched backdrop lambda")
