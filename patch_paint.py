import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

# Remove the hardcoded ROUND overrides in shapePaint
content = re.sub(
    r'asFrameworkPaint\(\)\.strokeCap = android\.graphics\.Paint\.Cap\.ROUND\s*asFrameworkPaint\(\)\.strokeJoin = android\.graphics\.Paint\.Join\.ROUND',
    r'',
    content
)

# And also for the bezier/path rendering
content = re.sub(
    r'shapePaint\.strokeCap = androidx\.compose\.ui\.graphics\.StrokeCap\.Round\s*shapePaint\.strokeJoin = androidx\.compose\.ui\.graphics\.StrokeJoin\.Round',
    r'shapePaint.strokeCap = if (styleToUse is androidx.compose.ui.graphics.drawscope.Stroke) styleToUse.cap else androidx.compose.ui.graphics.StrokeCap.Round\n                        shapePaint.strokeJoin = if (styleToUse is androidx.compose.ui.graphics.drawscope.Stroke) styleToUse.join else androidx.compose.ui.graphics.StrokeJoin.Round',
    content
)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)
