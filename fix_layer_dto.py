with open('app/src/main/java/com/example/studio/model/LayerDto.kt', 'r', encoding='utf-8') as f:
    text = f.read()

# Fields to add to data class LayerDto
# cropLeft, cropTop, cropRight, cropBottom
# adjBrightness, adjContrast, adjSaturation, adjColorTintValue, adjTintColorIntensity
# threeDStateJson

import re

# Insert into LayerDto
new_fields = """
    val warpMeshDivisionY: Int? = 3,
    val cropLeft: Float? = 0f,
    val cropTop: Float? = 0f,
    val cropRight: Float? = 1.0f,
    val cropBottom: Float? = 1.0f,
    val adjBrightness: Float? = 0f,
    val adjContrast: Float? = 1f,
    val adjSaturation: Float? = 1f,
    val adjColorTintValue: Long? = 0L,
    val adjTintColorIntensity: Float? = 0f,
    val threeDStateJson: String? = null
) {
"""
text = re.sub(r'val warpMeshDivisionY: Int\? = 3\s*\)\s*\{', new_fields.strip() + " {", text)

# Insert into toLayer
new_to_layer_fields = """
            warpMeshDivisionX = warpMeshDivisionX ?: 3,
            warpMeshDivisionY = warpMeshDivisionY ?: 3,
            cropLeft = cropLeft ?: 0f,
            cropTop = cropTop ?: 0f,
            cropRight = cropRight ?: 1.0f,
            cropBottom = cropBottom ?: 1.0f,
            adjBrightness = adjBrightness ?: 0f,
            adjContrast = adjContrast ?: 1f,
            adjSaturation = adjSaturation ?: 1f,
            adjColorTint = androidx.compose.ui.graphics.Color((adjColorTintValue ?: 0L).toULong()),
            adjTintColorIntensity = adjTintColorIntensity ?: 0f,
            threeDStateJson = threeDStateJson
        )
"""
text = re.sub(r'warpMeshDivisionX = warpMeshDivisionX \?: 3,\s*warpMeshDivisionY = warpMeshDivisionY \?: 3\s*\)', new_to_layer_fields.strip() + ")", text)

# Insert into fromLayer
new_from_layer_fields = """
                warpMeshDivisionX = layer.warpMeshDivisionX,
                warpMeshDivisionY = layer.warpMeshDivisionY,
                cropLeft = layer.cropLeft.sanitize(0f),
                cropTop = layer.cropTop.sanitize(0f),
                cropRight = layer.cropRight.sanitize(1.0f),
                cropBottom = layer.cropBottom.sanitize(1.0f),
                adjBrightness = layer.adjBrightness.sanitize(0f),
                adjContrast = layer.adjContrast.sanitize(1f),
                adjSaturation = layer.adjSaturation.sanitize(1f),
                adjColorTintValue = layer.adjColorTint.value.toLong(),
                adjTintColorIntensity = layer.adjTintColorIntensity.sanitize(0f),
                threeDStateJson = layer.threeDStateJson
            )
"""
text = re.sub(r'warpMeshDivisionX = layer\.warpMeshDivisionX,\s*warpMeshDivisionY = layer\.warpMeshDivisionY\s*\)', new_from_layer_fields.strip() + ")", text)

with open('app/src/main/java/com/example/studio/model/LayerDto.kt', 'w', encoding='utf-8') as f:
    f.write(text)
