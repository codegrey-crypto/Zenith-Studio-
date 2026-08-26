import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """    if (showImportOptionMenu) {"""

dialog_code = """    if (showElementsImportDialog) {
        var elementsList by remember { mutableStateOf<List<com.example.studio.model.StudioLayer>>(emptyList()) }
        LaunchedEffect(Unit) {
            elementsList = com.example.studio.model.ElementsManager.loadElements(context)
        }
        AlertDialog(
            onDismissRequest = { showElementsImportDialog = false },
            containerColor = Color(0xFF1E1E24),
            title = { Text("Import Element", color = TextPrimary, style = Typography.titleMedium) },
            text = {
                if (elementsList.isEmpty()) {
                    Text("No saved elements found.", color = TextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(elementsList) { element ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val newElement = element.copy(id = java.util.UUID.randomUUID().toString())
                                        layers = layers + newElement
                                        projectHistoryManager.captureState(projectId, createMemento())
                                        showElementsImportDialog = false
                                    },
                                colors = CardDefaults.cardColors(containerColor = SlatePanel.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Extension, contentDescription = null, tint = IndustrialAmber)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(element.name, color = TextPrimary, style = Typography.bodyMedium)
                                        Text(element.type.name, color = TextSecondary, style = Typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showElementsImportDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showImportOptionMenu) {"""

text = text.replace(target, dialog_code)
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
