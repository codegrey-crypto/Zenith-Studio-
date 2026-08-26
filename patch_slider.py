import re

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    content = f.read()

replacement = """                }

                // Corner Radius
                val cRadius = bordersShadowsEff.parameters["${borderStrokeTypeToEdit}_CornerRadius"]?.value ?: 0f
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Corner Radius", style = Typography.labelSmall, fontSize = 9.sp, color = TextSecondary)
                        Text("${cRadius.toInt()} px", style = Typography.labelSmall, fontSize = 9.sp, color = IndustrialAmber)
                    }
                    Slider(
                        value = cRadius,
                        onValueChange = { onUpdateEffectParam(bordersShadowsEff.id, "${borderStrokeTypeToEdit}_CornerRadius", it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(activeTrackColor = IndustrialAmber, thumbColor = IndustrialAmber),
                        modifier = Modifier.height(24.dp)
                    )
                }

                // Opacity / Alpha"""

content = content.replace('                }\n\n                // Opacity / Alpha', replacement)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(content)
