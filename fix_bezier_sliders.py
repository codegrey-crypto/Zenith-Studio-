with open('app/src/main/java/com/example/studio/ui/BezierVectorControlPane.kt', 'r') as f:
    content = f.read()

import re

old_slider_1 = """                                            if (currentNodeType == "SMOOTH" && keepBezierSymmetrical) {
                                                val otherIdx = if (currentIdx % 3 == 1) k * 3 + 2 else k * 3 + 1
                                                val otherPt = updated[otherIdx]
                                                val otherDist = Math.hypot((otherPt.x - anchor.x).toDouble(), (otherPt.y - anchor.y).toDouble()).toFloat()
                                                val oppAngle = angleRad + Math.PI
                                                updated[otherIdx] = anchor + Offset(Math.cos(oppAngle).toFloat() * otherDist, Math.sin(oppAngle).toFloat() * otherDist)
                                            }"""

new_slider_1 = """                                            if (currentNodeType != "CORNER" && keepBezierSymmetrical) {
                                                val otherIdx = if (currentIdx % 3 == 1) k * 3 + 2 else k * 3 + 1
                                                val oppAngle = angleRad + Math.PI
                                                updated[otherIdx] = anchor + Offset(Math.cos(oppAngle).toFloat() * newDist, Math.sin(oppAngle).toFloat() * newDist)
                                            }"""

old_slider_2 = """                                            if (currentNodeType == "SMOOTH" && keepBezierSymmetrical) {
                                                val otherIdx = if (currentIdx % 3 == 1) k * 3 + 2 else k * 3 + 1
                                                val otherPt = updated[otherIdx]
                                                val otherDist = Math.hypot((otherPt.x - anchor.x).toDouble(), (otherPt.y - anchor.y).toDouble()).toFloat()
                                                val oppAngle = angleRad + Math.PI
                                                updated[otherIdx] = anchor + Offset(Math.cos(oppAngle).toFloat() * otherDist, Math.sin(oppAngle).toFloat() * otherDist)
                                            }"""

new_slider_2 = """                                            if (currentNodeType != "CORNER" && keepBezierSymmetrical) {
                                                val otherIdx = if (currentIdx % 3 == 1) k * 3 + 2 else k * 3 + 1
                                                val otherPt = updated[otherIdx]
                                                val otherDist = Math.hypot((otherPt.x - anchor.x).toDouble(), (otherPt.y - anchor.y).toDouble()).toFloat()
                                                val oppAngle = angleRad + Math.PI
                                                // When rotating, keep symmetrical handle distance equal to current handle distance if fully symmetric
                                                updated[otherIdx] = anchor + Offset(Math.cos(oppAngle).toFloat() * currentDist, Math.sin(oppAngle).toFloat() * currentDist)
                                            }"""

if old_slider_1 in content:
    # Replace the first occurrence (Curve Amount)
    content = content.replace(old_slider_1, new_slider_1, 1)
    # Replace the second occurrence (Curve Angle)
    content = content.replace(old_slider_2, new_slider_2, 1)
    
    with open('app/src/main/java/com/example/studio/ui/BezierVectorControlPane.kt', 'w') as f:
        f.write(content)
    print("Sliders fixed")
else:
    print("Sliders not found")
