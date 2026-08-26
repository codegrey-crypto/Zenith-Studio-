import re

with open('app/src/main/java/com/example/studio/ui/BottomPanelDetailViews.kt', 'r') as f:
    code = f.read()

# Replace PrecisionJogWheel Slider logic
new_slider = """
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    val validatedValue = if (label.contains("Persp")) {
                        mapSliderToPersp(newValue)
                    } else {
                        val rawQuantized = (newValue * 100f).roundToInt() / 100f
                        if (isInt) kotlin.math.round(rawQuantized) else rawQuantized
                    }
                    
                    SlidersHighFreqState.set(highFreqKey ?: label, validatedValue)
                    localValue = validatedValue
                    val now = System.currentTimeMillis()
                    if (now - lastUpdateTime > 100) {
                        onValueChange(validatedValue)
                        lastUpdateTime = now
                    }
                },
                onValueChangeFinished = { onValueChange(localValue) },
                valueRange = range.start..range.endInclusive,"""

code = re.sub(
    r"""Slider\(\s*value = sliderValue,\s*onValueChange = \{ newValue ->\s*val validatedValue = if \(label.contains\("Persp"\)\) \{\s*mapSliderToPersp\(newValue\)\s*\} else \{\s*val rawQuantized = \(newValue \* 100f\).roundToInt\(\) / 100f\s*if \(isInt\) kotlin.math.round\(rawQuantized\) else rawQuantized\s*\}\s*SlidersHighFreqState.set\(highFreqKey \?: label, validatedValue\)\s*onValueChange\(validatedValue\)\s*\},.*?valueRange = range.start\.\.range.endInclusive,""",
    new_slider.strip(),
    code,
    flags=re.DOTALL
)

# EffectParamSlider
effect_slider = """
fun EffectParamSlider(
    effectId: String,
    paramName: String,
    value: Float,
    rangeMin: Float,
    rangeMax: Float,
    onUpdateEffectParam: (String, String, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val isColorGradingFilter = paramName == "Preset" || paramName == "Exposure" || paramName == "Contrast" || paramName == "Highlights" || paramName == "Shadows" || paramName == "Whites" || paramName == "Blacks" || paramName == "Temp" || paramName == "Tint" || paramName == "Vibrance" || paramName == "Saturation" || paramName == "Clarity" || paramName == "Dehaze" || paramName == "Brightness" || paramName == "Sat" || paramName == "Bright" || paramName == "Opacity"
    val activeColor = if (isColorGradingFilter) Color(0xFFA855F7) else IndustrialAmber
    
    var localValue by remember(value) { mutableFloatStateOf(value) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }
    
    Slider(
        value = localValue.coerceIn(rangeMin..rangeMax),
        onValueChange = { newValue ->
            val rawQuantized = (newValue * 100f).roundToInt() / 100f
            SlidersHighFreqState.set("effect_${effectId}_${paramName}", rawQuantized)
            localValue = rawQuantized
            val now = System.currentTimeMillis()
            if (now - lastUpdateTime > 100) {
                onUpdateEffectParam(effectId, paramName, rawQuantized)
                lastUpdateTime = now
            }
        },
        onValueChangeFinished = { onUpdateEffectParam(effectId, paramName, localValue) },
        valueRange = rangeMin..rangeMax,"""

code = re.sub(
    r"""fun EffectParamSlider\(\s*effectId: String,\s*paramName: String,\s*value: Float,\s*rangeMin: Float,\s*rangeMax: Float,\s*onUpdateEffectParam: \(String, String, Float\) -> Unit,\s*modifier: Modifier = Modifier\s*\) \{.*?Slider\(\s*value = value.coerceIn\(rangeMin\.\.rangeMax\),\s*onValueChange = \{ newValue ->\s*val rawQuantized = \(newValue \* 100f\).roundToInt\(\) / 100f\s*SlidersHighFreqState.set\("effect_\$\{effectId\}_\$\{paramName\}", rawQuantized\)\s*onUpdateEffectParam\(effectId, paramName, rawQuantized\)\s*\},\s*valueRange = rangeMin\.\.rangeMax,""",
    effect_slider.strip(),
    code,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/studio/ui/BottomPanelDetailViews.kt', 'w') as f:
    f.write(code)
