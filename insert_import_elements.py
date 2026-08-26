import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E24))
                                .clickable {
                                    showImportOptionMenu = false
                                    showPngSearchOverlay = true
                                }"""

replacement = """                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E24))
                                .clickable {
                                    showImportOptionMenu = false
                                    showElementsImportDialog = true
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = null, tint = IndustrialAmber, modifier = Modifier.size(24.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Import from Elements", style = Typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("Import saved vector elements into your current project.", style = Typography.labelSmall, color = TextSecondary)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E1E24))
                                .clickable {
                                    showImportOptionMenu = false
                                    showPngSearchOverlay = true
                                }"""

text = text.replace(target, replacement)
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
