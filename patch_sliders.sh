#!/bin/bash
sed -i 's/var isEditing by remember { mutableStateOf(false) }/var isEditing by remember { mutableStateOf(false) }\n    var localValue by remember(value) { mutableFloatStateOf(value) }\n    var lastUpdateTime by remember { mutableLongStateOf(0L) }/g' app/src/main/java/com/example/studio/ui/BottomPanelDetailViews.kt

sed -i 's/val currentValue = value.coerceIn(valueRange)/val currentValue = localValue.coerceIn(valueRange)/g' app/src/main/java/com/example/studio/ui/BottomPanelDetailViews.kt

# First Slider replacement
sed -i 's/SlidersHighFreqState.set(highFreqKey ?: label, validatedValue)\n                    onValueChange(validatedValue)\n                },\n                valueRange = range.start..range.endInclusive,/SlidersHighFreqState.set(highFreqKey ?: label, validatedValue)\n                    localValue = validatedValue\n                    val now = System.currentTimeMillis()\n                    if (now - lastUpdateTime > 80) {\n                        onValueChange(validatedValue)\n                        lastUpdateTime = now\n                    }\n                },\n                onValueChangeFinished = { onValueChange(localValue) },\n                valueRange = range.start..range.endInclusive,/g' app/src/main/java/com/example/studio/ui/BottomPanelDetailViews.kt

# EffectParamSlider replacement
sed -i 's/fun EffectParamSlider(/fun EffectParamSlider(\n    isDebounced: Boolean = true,/g' app/src/main/java/com/example/studio/ui/BottomPanelDetailViews.kt

