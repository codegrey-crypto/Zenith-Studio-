import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

def replacer(match):
    return """                // VECTOR SHAPE WITH NO IMAGE: Render shape offscreen, then apply GPU filter
                var maxBlurRadius = 0f
                for (effect in activeList) {
                    if (effect is com.example.studio.model.StudioEffect.PhotoshopEffect) {
                        val r = effect.parameters["Radius"]?.value ?: effect.parameters["Blur Radius"]?.value ?: effect.parameters["Distance"]?.value ?: effect.parameters["Bokeh Radius"]?.value ?: 0f
                        maxBlurRadius = maxOf(maxBlurRadius, r)
                    } else if (effect is com.example.studio.model.StudioEffect.GaussianBlur) {
                        maxBlurRadius = maxOf(maxBlurRadius, effect.radius)
                    }
                }
                val pad = (60 + maxBlurRadius * 3).toInt().coerceIn(60, 800)
                val w = layer.width.toInt().coerceIn(1, 2048)"""

pattern = re.compile(r'                // VECTOR SHAPE WITH NO IMAGE: Render shape offscreen, then apply GPU filter\n                val pad = 60\n                val w = layer\.width\.toInt\(\)\.coerceIn\(1, 2048\)')
new_content = pattern.sub(replacer, content)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(new_content)
