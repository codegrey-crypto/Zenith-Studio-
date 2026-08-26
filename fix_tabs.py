import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

# Replace the second 'activeMenuTab == 3' with 'activeMenuTab == 4'
target = """            } else if (activeMenuTab == 3) {
                // Settings Tab (activeMenuTab == 3)"""

replacement = """            } else if (activeMenuTab == 4) {
                // Settings Tab (activeMenuTab == 4)"""

if target in text:
    text = text.replace(target, replacement)
    print("Fixed Settings tab")
else:
    print("Target not found for Settings tab")

# Insert Elements tab (activeMenuTab == 2)
target_insert = """            } else if (activeMenuTab == 3) {
                // Compositions / Import Services Tab"""

elements_code = """            } else if (activeMenuTab == 2) {
                // Elements Tab
                var savedElements by remember { mutableStateOf<List<com.example.studio.model.StudioLayer>>(emptyList()) }
                LaunchedEffect(activeMenuTab) {
                    savedElements = com.example.studio.model.ElementsManager.loadElements(context)
                }
                if (savedElements.isEmpty()) {
                    Text(
                        text = "No saved elements found.",
                        color = TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(savedElements) { layer ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = SlatePanel.copy(alpha = 0.85f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(layer.name, style = Typography.titleMedium, color = TextPrimary)
                                        Text(layer.type.name, style = Typography.labelSmall, color = TextSecondary)
                                    }
                                    IconButton(onClick = {
                                        kotlinx.coroutines.GlobalScope.launch {
                                            com.example.studio.model.ElementsManager.removeElement(context, layer.id)
                                            savedElements = com.example.studio.model.ElementsManager.loadElements(context)
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red)
                                    }
                                }
                            }
                        }
                    }
                }
"""

if target_insert in text:
    text = text.replace(target_insert, elements_code + target_insert)
    print("Inserted Elements tab")
else:
    print("Target not found for Elements tab")

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
