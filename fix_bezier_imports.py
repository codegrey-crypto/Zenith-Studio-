with open('app/src/main/java/com/example/studio/ui/BezierVectorControlPane.kt', 'r') as f:
    content = f.read()

content = content.replace("private val MaterialTheme.colorScheme.primary = Color(0xFFFFB300)", "private val AccentYellow = Color(0xFFFFB300)")
content = content.replace("private val MaterialTheme.colorScheme.primary = Color(0xFFFFD54F)", "private val AccentYellowLight = Color(0xFFFFD54F)")

with open('app/src/main/java/com/example/studio/ui/BezierVectorControlPane.kt', 'w') as f:
    f.write(content)
print("Bezier constants fixed")
