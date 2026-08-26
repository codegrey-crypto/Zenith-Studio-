import sys

file_path = "app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt"

with open(file_path, "r") as f:
    content = f.read()

old_toggle = """                                            onToggleNodeType = {
                                                if (selectedLayer != null && selectedLayer.type == com.example.studio.model.LayerType.VECTOR_BEZIER && activeBezierPointIndex != -1) {
                                                    val k = activeBezierPointIndex / 3
                                                    val currentTypes = selectedLayer.bezierNodeTypes.toMutableList()
                                                    while (currentTypes.size <= k) {
                                                        currentTypes.add("SMOOTH")
                                                    }
                                                    val oldType = currentTypes[k]
                                                    val newType = if (oldType == "CORNER") "SMOOTH" else "CORNER"
                                                    currentTypes[k] = newType
                                                    
                                                    undoStack.add(layers)
                                                    redoStack.clear()
                                                    layers = layers.map {
                                                        if (it.id == selectedLayer.id) {
                                                            it.copy(bezierNodeTypes = currentTypes)
                                                        } else it
                                                    }
                                                    com.example.studio.ui.ParametricLayerCache.invalidate(selectedLayer.id)
                                                }
                                            },"""

new_toggle = """                                            onToggleNodeType = {
                                                if (selectedLayer != null && selectedLayer.type == com.example.studio.model.LayerType.VECTOR_BEZIER && activeBezierPointIndex != -1) {
                                                    val k = activeBezierPointIndex / 3
                                                    val currentTypes = selectedLayer.bezierNodeTypes.toMutableList()
                                                    while (currentTypes.size <= k) {
                                                        currentTypes.add("SMOOTH")
                                                    }
                                                    val oldType = currentTypes[k]
                                                    val newType = if (oldType == "CORNER") "SMOOTH" else "CORNER"
                                                    currentTypes[k] = newType
                                                    
                                                    val updatedPoints = selectedLayer.brushPoints.toMutableList()
                                                    if (newType == "SMOOTH" && (k * 3 + 2) < updatedPoints.size) {
                                                        val anchor = updatedPoints[k * 3]
                                                        val handleIn = updatedPoints[k * 3 + 1]
                                                        val handleOut = updatedPoints[k * 3 + 2]
                                                        val distIn = (handleIn - anchor).getDistance()
                                                        val distOut = (handleOut - anchor).getDistance()
                                                        
                                                        if (distOut > 0.001f) {
                                                            val vOut = handleOut - anchor
                                                            val normIn = androidx.compose.ui.geometry.Offset(-vOut.x / distOut, -vOut.y / distOut)
                                                            updatedPoints[k * 3 + 1] = anchor + (normIn * distIn)
                                                        } else if (distIn > 0.001f) {
                                                            val vIn = handleIn - anchor
                                                            val normOut = androidx.compose.ui.geometry.Offset(-vIn.x / distIn, -vIn.y / distIn)
                                                            updatedPoints[k * 3 + 2] = anchor + (normOut * distOut)
                                                        }
                                                    }
                                                    
                                                    undoStack.add(layers)
                                                    redoStack.clear()
                                                    layers = layers.map {
                                                        if (it.id == selectedLayer.id) {
                                                            it.copy(bezierNodeTypes = currentTypes, brushPoints = updatedPoints)
                                                        } else it
                                                    }
                                                    com.example.studio.ui.ParametricLayerCache.invalidate(selectedLayer.id)
                                                }
                                            },"""

if old_toggle in content:
    content = content.replace(old_toggle, new_toggle)
    print("Patched onToggleNodeType successfully")
else:
    print("Could not find onToggleNodeType block to patch")

with open(file_path, "w") as f:
    f.write(content)
