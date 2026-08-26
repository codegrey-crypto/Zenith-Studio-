import sys

file_path = "app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

lines = content.split('\n')
for i, line in enumerate(lines):
    if 'val isCorner = layer.bezierNodeTypes.getOrNull(k) == "CORNER"' in line:
        if "updatedPoints[idx] = localCtrl" in lines[i+1]:
            # This is the bezier drag one
            start_i = i - 1
            # find end of block
            end_i = i + 1
            while "}" not in lines[end_i] or "if (!isCorner" in lines[end_i-1] or "updatedPoints" in lines[end_i]:
                if lines[end_i].strip() == "}" and lines[end_i+1].strip() == "}":
                    end_i += 1
                    break
                end_i += 1
            print(f"Found bezier block from {start_i} to {end_i}")
            break
