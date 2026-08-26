import re

with open('app/src/main/java/com/example/studio/ui/LayersPanel.kt', 'r') as f:
    text = f.read()

target = """                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,"""

replacement = """                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Extension,
                                                    contentDescription = "Save to Elements",
                                                    tint = TextSecondary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            },
                                            text = {
                                                Text(
                                                    text = "Save to Elements",
                                                    color = TextPrimary,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            },
                                            onClick = {
                                                kotlinx.coroutines.GlobalScope.launch {
                                                    com.example.studio.model.ElementsManager.saveElement(context, item.copy(id = java.util.UUID.randomUUID().toString()))
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        android.widget.Toast.makeText(context, "Saved to Elements", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                showContextMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,"""

if target in text:
    text = text.replace(target, replacement)
    with open('app/src/main/java/com/example/studio/ui/LayersPanel.kt', 'w') as f:
        f.write(text)
    print("Patched LayersPanel.kt")
else:
    print("Target not found in LayersPanel.kt")
