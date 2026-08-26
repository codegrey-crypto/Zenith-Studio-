import re
with open('app/src/main/java/com/example/studio/ui/LayersPanel.kt', 'r') as f:
    text = f.read()

target = """                                            onClick = {
                                                kotlinx.coroutines.GlobalScope.launch {
                                                    com.example.studio.model.ElementsManager.saveElement(context, item.copy(id = java.util.UUID.randomUUID().toString()))
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        android.widget.Toast.makeText(context, "Saved to Elements", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                showContextMenu = false
                                            }"""

replacement = """                                            onClick = {
                                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                                    com.example.studio.model.ElementsManager.saveElement(context, item.copy(id = java.util.UUID.randomUUID().toString()))
                                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                        android.widget.Toast.makeText(context, "Saved to Elements", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                                showContextMenu = false
                                            }"""
# Wait, context is not defined. We need to get context.
# Let's insert `val context = androidx.compose.ui.platform.LocalContext.current` at the top of the composable, or just inside `LayersPanel`.
