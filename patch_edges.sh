sed -i '7188a\
                                                                // If no handle was directly clicked, check if they clicked on the edges\
                                                                if (matchedResizeHandle == null) {\
                                                                    fun distToSegmentSq(p: androidx.compose.ui.geometry.Offset, v: androidx.compose.ui.geometry.Offset, w: androidx.compose.ui.geometry.Offset): Float {\
                                                                        val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)\
                                                                        if (l2 == 0f) return (p.x - v.x) * (p.x - v.x) + (p.y - v.y) * (p.y - v.y)\
                                                                        var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2\
                                                                        t = Math.max(0f, Math.min(1f, t))\
                                                                        val proj = androidx.compose.ui.geometry.Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))\
                                                                        return (p.x - proj.x) * (p.x - proj.x) + (p.y - proj.y) * (p.y - proj.y)\
                                                                    }\
                                                                    val pt = androidx.compose.ui.geometry.Offset(artStartX, artStartY)\
                                                                    val distEdgeT = distToSegmentSq(pt, handleTL, handleTR)\
                                                                    val distEdgeB = distToSegmentSq(pt, handleBL, handleBR)\
                                                                    val distEdgeL = distToSegmentSq(pt, handleTL, handleBL)\
                                                                    val distEdgeR = distToSegmentSq(pt, handleTR, handleBR)\
                                                                    val edgeDistances = listOf(\
                                                                        Pair("resize_t", distEdgeT),\
                                                                        Pair("resize_b", distEdgeB),\
                                                                        Pair("resize_l", distEdgeL),\
                                                                        Pair("resize_r", distEdgeR)\
                                                                    )\
                                                                    for (item in edgeDistances) {\
                                                                        if (item.second < resThresholdSq && item.second < minResizeDistSq) {\
                                                                            minResizeDistSq = item.second\
                                                                            matchedResizeHandle = item.first\
                                                                        }\
                                                                    }\
                                                                }
' app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt
