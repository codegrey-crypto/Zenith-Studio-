with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
in_block = False
i = 0
while i < len(lines):
    line = lines[i]
    if "val maxDimension = 2048 // Downscale up to 2048 max" in line:
        # replace the block
        new_lines.append(line.replace("2048", "1080").replace("up to 2048 max to maintain high original detail and ultra-fast performance", "to match device screen resolution for live editing"))
    elif "inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888" in line and "this.inSampleSize = inSampleSize" in lines[i-1]:
        new_lines.append('                                            val isOpaque = sizeOptions.outMimeType == "image/jpeg" || sizeOptions.outMimeType == "image/heic"\n')
        new_lines.append('                                            inPreferredConfig = if (isOpaque) android.graphics.Bitmap.Config.RGB_565 else android.graphics.Bitmap.Config.ARGB_8888\n')
    else:
        new_lines.append(line)
    i += 1

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.writelines(new_lines)
print("Success")

