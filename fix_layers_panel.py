with open('app/src/main/java/com/example/studio/ui/LayersPanel.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# It currently has `package com.example.studio.uiimport ...`
# Let's insert newlines before all `import ` or `import` that follows a word without space
import re

text = text.replace("package com.example.studio.uiimport", "package com.example.studio.ui\nimport")
text = text.replace("launchimport", "launch\nimport")
text = text.replace(".*import", ".*\nimport")
text = text.replace("detectDragGesturesimport", "detectDragGestures\nimport")
text = text.replace("detectDragGesturesAfterLongPressimport", "detectDragGesturesAfterLongPress\nimport")
text = text.replace("LazyColumnimport", "LazyColumn\nimport")
text = text.replace("itemsIndexedimport", "itemsIndexed\nimport")
text = text.replace("RoundedCornerShapeimport", "RoundedCornerShape\nimport")
text = text.replace("Iconsimport", "Icons\nimport")
text = text.replace("Alignmentimport", "Alignment\nimport")
text = text.replace("Modifierimport", "Modifier\nimport")
text = text.replace("clipimport", "clip\nimport")
text = text.replace("scaleimport", "scale\nimport")
text = text.replace("Colorimport", "Color\nimport")
text = text.replace("graphicsLayerimport", "graphicsLayer\nimport")
text = text.replace("pointerInputimport", "pointerInput\nimport")
text = text.replace("testTagimport", "testTag\nimport")
text = text.replace("FontWeightimport", "FontWeight\nimport")
text = text.replace("dpimport", "dp\nimport")
text = text.replace("spimport", "sp\nimport")
text = text.replace("zIndeximport", "zIndex\nimport")
text = text.replace("LayerTypeimport", "LayerType\nimport")
text = text.replace("StudioLayerimport", "StudioLayer\nimport")
text = text.replace("ZenithBlendModeimport", "ZenithBlendMode\nimport")
text = text.replace("UUID@Composablefun", "UUID\n\n@Composable\nfun")
text = text.replace("UUID@Composable", "UUID\n\n@Composable\n")

# To be safe, let's just use regex: replace `import` with `\nimport ` ONLY if it's immediately after a lowercase letter or `*` or `)` or `}`
text = re.sub(r'([a-zA-Z0-9\*\)\>])import', r'\1\nimport', text)
text = re.sub(r'([a-zA-Z0-9\*\)\>])@Composable', r'\1\n@Composable', text)

with open('app/src/main/java/com/example/studio/ui/LayersPanel.kt', 'w', encoding='utf-8') as f:
    f.write(text)
