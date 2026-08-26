with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    if "val isOpaque =" in line and "image/jpeg" in line:
        if i > 4700 and i < 4800:
            new_lines.append(line.replace("options.outMimeType", "sizeOptions.outMimeType"))
        elif i > 5200 and i < 5300:
            new_lines.append(line.replace("options.outMimeType", "sizeOptions.outMimeType")) # wait, at 5260 it IS sizeOptions!
        else:
            new_lines.append(line)
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.writelines(new_lines)
print("Success")
