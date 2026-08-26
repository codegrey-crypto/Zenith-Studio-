with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
for line in lines:
    if "val isOpaque = sizeOptions.outMimeType" in line and "image/jpeg" in line:
        if "sizeOptions.outMimeType" in line:
            new_lines.append(line.replace("sizeOptions.outMimeType", "options.outMimeType"))
        else:
            new_lines.append(line)
    else:
        new_lines.append(line)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.writelines(new_lines)
print("Success")
