with open('app/src/main/java/com/example/studio/ui/BottomPanelDetailViews.kt', 'r') as f:
    content = f.read()

import re

old_header = """        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Layer FX Stack",
                    color = TextPrimary,
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${selectedLayer.effects.size} active effects on [${selectedLayer.name}]",
                    color = TextSecondary,
                    style = Typography.labelSmall,
                    fontSize = 10.sp
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {"""

new_header = """        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {"""

content = content.replace(old_header, new_header)

with open('app/src/main/java/com/example/studio/ui/BottomPanelDetailViews.kt', 'w') as f:
    f.write(content)
print("FiltersAndFxDetailView fixed")
