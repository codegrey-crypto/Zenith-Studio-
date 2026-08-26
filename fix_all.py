import re

with open('app/src/main/java/com/aistudio/zenithstudio/rpxwtq/ZenithFilterEngine.kt', 'r') as f:
    content = f.read()

def replacer(match):
    return """                "blur_gaussian", "blur_box", "image_toolbox_gaussian_blur", "image_toolbox_box_blur" -> {
                    val r = parameters.firstOrNull()?.currentValue ?: 15f
                    applyBoxBlur(source, r.toInt().coerceAtLeast(1))
                }"""

pattern = re.compile(r'                "blur_gaussian", "blur_box", "image_toolbox_gaussian_blur", "image_toolbox_box_blur" -> \{[\s\S]*?applySingleGPUImageFilter\(context, source, filter\)\n                \}')
new_content = pattern.sub(replacer, content)

with open('app/src/main/java/com/aistudio/zenithstudio/rpxwtq/ZenithFilterEngine.kt', 'w') as f:
    f.write(new_content)
