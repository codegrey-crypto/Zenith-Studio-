import sys

file_path = "app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt"

with open(file_path, "r") as f:
    lines = f.readlines()

new_lines = []
i = 0
while i < len(lines):
    if "if (!isCorner && keepBezierSymmetrical) {" in lines[i]:
        # skip until closing brace
        indent = lines[i].find("if")
        
        new_logic = f"""{' ' * indent}if (!isCorner) {{
{' ' * indent}    val anchorPos = updatedPoints[k * 3]
{' ' * indent}    val otherHandleIndex = if (idx % 3 == 1) k * 3 + 2 else k * 3 + 1
{' ' * indent}    if (otherHandleIndex in updatedPoints.indices) {{
{' ' * indent}        if (keepBezierSymmetrical) {{
{' ' * indent}            updatedPoints[otherHandleIndex] = anchorPos * 2f - localCtrl
{' ' * indent}        }} else {{
{' ' * indent}            val otherHandleCurrent = updatedPoints[otherHandleIndex]
{' ' * indent}            val otherHandleDist = (otherHandleCurrent - anchorPos).getDistance()
{' ' * indent}            val draggedHandleVector = localCtrl - anchorPos
{' ' * indent}            val draggedHandleDist = draggedHandleVector.getDistance()
{' ' * indent}            if (draggedHandleDist > 0.001f) {{
{' ' * indent}                val normalizedOpposite = androidx.compose.ui.geometry.Offset(-draggedHandleVector.x / draggedHandleDist, -draggedHandleVector.y / draggedHandleDist)
{' ' * indent}                updatedPoints[otherHandleIndex] = anchorPos + (normalizedOpposite * otherHandleDist)
{' ' * indent}            }}
{' ' * indent}        }}
{' ' * indent}    }}
{' ' * indent}}}\n"""
        new_lines.append(new_logic)
        
        # skip lines until the matching closing brace
        brace_count = 1
        i += 1
        while brace_count > 0 and i < len(lines):
            if "{" in lines[i]:
                brace_count += 1
            if "}" in lines[i]:
                brace_count -= 1
            i += 1
        continue
        
    new_lines.append(lines[i])
    i += 1

with open(file_path, "w") as f:
    f.writelines(new_lines)
print("Patched bezier successfully")
