import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """            } else if (activeMenuTab == 2) {
                // Elements Tab
                var savedElements by remember { mutableStateOf<List<com.example.studio.model.StudioLayer>>(emptyList()) }
                LaunchedEffect(activeMenuTab) {
                    savedElements = com.example.studio.model.ElementsManager.loadElements(context)
                }"""

replacement = """            } else if (activeMenuTab == 2) {
                // Elements Tab
                val localContext = androidx.compose.ui.platform.LocalContext.current
                var savedElements by remember { mutableStateOf<List<com.example.studio.model.StudioLayer>>(emptyList()) }
                LaunchedEffect(activeMenuTab) {
                    savedElements = com.example.studio.model.ElementsManager.loadElements(localContext)
                }"""

text = text.replace(target, replacement)

target2 = """                                    IconButton(onClick = {
                                        kotlinx.coroutines.GlobalScope.launch {
                                            com.example.studio.model.ElementsManager.removeElement(context, layer.id)
                                            savedElements = com.example.studio.model.ElementsManager.loadElements(context)
                                        }
                                    }) {"""

replacement2 = """                                    IconButton(onClick = {
                                        kotlinx.coroutines.GlobalScope.launch {
                                            com.example.studio.model.ElementsManager.removeElement(localContext, layer.id)
                                            savedElements = com.example.studio.model.ElementsManager.loadElements(localContext)
                                        }
                                    }) {"""

text = text.replace(target2, replacement2)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
