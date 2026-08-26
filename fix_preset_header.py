with open('app/src/main/java/com/aistudio/zenithstudio/rpxwtq/EffectPresetLibraryUI.kt', 'r') as f:
    content = f.read()

import re

old_header = """        // Top Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {"""

new_header = """        // Top Header Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onClose != null) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = IndustrialAmber)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }"""

if old_header in content:
    content = content.replace(old_header, new_header)
    with open('app/src/main/java/com/aistudio/zenithstudio/rpxwtq/EffectPresetLibraryUI.kt', 'w') as f:
        f.write(content)
    print("Preset library fixed")
else:
    print("Not found")
