with open('app/src/main/java/com/example/studio/ui/BezierVectorControlPane.kt', 'r') as f:
    content = f.read()

import re

# Replace IndustrialAmber with MaterialTheme.colorScheme.primary
# Replace EnergeticYellow with MaterialTheme.colorScheme.primary
content = content.replace("IndustrialAmber", "MaterialTheme.colorScheme.primary")
content = content.replace("EnergeticYellow", "MaterialTheme.colorScheme.primary")

with open('app/src/main/java/com/example/studio/ui/BezierVectorControlPane.kt', 'w') as f:
    f.write(content)
print("Bezier theme updated")
