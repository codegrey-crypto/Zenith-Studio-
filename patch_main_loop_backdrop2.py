import re

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "r") as f:
    content = f.read()

target = """                                        isRasterizing = false
                                    )"""

replacement = """                                        isRasterizing = false,
                                        backdropProvider = {
                                            val cWidth = canvasWidth.toInt().coerceAtLeast(1)
                                            val cHeight = canvasHeight.toInt().coerceAtLeast(1)
                                            if (reusableBackdropBitmap == null || reusableBackdropBitmap!!.width != cWidth || reusableBackdropBitmap!!.height != cHeight) {
                                                reusableBackdropBitmap = android.graphics.Bitmap.createBitmap(cWidth, cHeight, android.graphics.Bitmap.Config.ARGB_8888)
                                            }
                                            com.example.studio.ui.GraphicsUtils.extractBackdrop(this, reusableBackdropBitmap!!)
                                        }
                                    )"""

# Only replace the FIRST occurrence which is in the main loop!
content = content.replace(target, replacement, 1)

with open("app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt", "w") as f:
    f.write(content)
print("Patched main loop backdrop provider")
