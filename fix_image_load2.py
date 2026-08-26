with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
i = 0
while i < len(lines):
    line = lines[i]
    if "val maxDimension = 4096" in line and "var inSampleSize = 1" in lines[i-1]:
        new_lines.append(line.replace("4096", "1080"))
    elif "inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888" in line and "this.inSampleSize = inSampleSize" in lines[i-1]:
        new_lines.append('                                inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 // Opaque fallback\n')
    else:
        new_lines.append(line)
    i += 1

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.writelines(new_lines)
print("Success")

