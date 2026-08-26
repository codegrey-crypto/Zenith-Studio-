import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    lines = f.readlines()

in_draw_geometry = False
for i, line in enumerate(lines):
    if "fun drawGeometry(" in line or "fun drawGeometryWithBlurAndOffset" in line or "fun drawGeometryWithBlur" in line:
        in_draw_geometry = True
    # wait, drawGeometry and others are huge. How do we know when they end?
    # better to just fix the specific lines we know are bad.
    
    # 10030 and 20234 and 28758 are bad.
    # Actually, all instances in 10000-26000 are definitely bad.
    if i < 26000:
        lines[i] = lines[i].replace("cornerRadiusOverride ?: layer.cornerRadius", "layer.cornerRadius")
    
    # Let's also check 28758 - that's in drawPathOutline? Let's check where it is.

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.writelines(lines)
