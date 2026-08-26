import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    lines = f.readlines()

for i in range(len(lines)):
    if "(cornerRadiusOverride ?: layer.cornerRadius)" in lines[i]:
        # check if cornerRadiusOverride is in scope
        # by checking backwards for fun drawGeometry
        scope_found = False
        # this is just a global replace back to layer.cornerRadius for any ones outside of drawGeometry* functions
        # Wait, the easiest way is to add cornerRadiusOverride to drawGeometryWithBrush too.
