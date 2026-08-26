import re
with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'r') as f:
    text = f.read()

target = """@Composable
fun ProjectThumbnail(
    layers: List<StudioLayer> = emptyList(),
    projectWidth: Float = 1920f,
    projectHeight: Float = 1080f,
    title: String = "",
    thumbnailUrl: String? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFF22222B), RoundedCornerShape(8.dp))
            .border(1.dp, HighslateOutline, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = IndustrialAmber,
            modifier = Modifier.size(24.dp)
        )
    }
}"""

replacement = """@Composable
fun ProjectThumbnail(
    projectId: String,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val thumbFile = java.io.File(context.filesDir, "thumb_${projectId}.png")
    
    Box(
        modifier = modifier
            .background(Color(0xFF22222B), RoundedCornerShape(8.dp))
            .border(1.dp, HighslateOutline, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (thumbFile.exists()) {
            coil.compose.AsyncImage(
                model = thumbFile,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit
            )
        } else {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = IndustrialAmber,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}"""

text = text.replace(target, replacement)

# Update references to ProjectThumbnail
target_ref = """                                        ProjectThumbnail(
                                            layers = currentLayers,
                                            projectWidth = proj.width,
                                            projectHeight = proj.height,
                                            modifier = Modifier
                                                .width(90.dp)
                                                .height(115.dp)
                                        )"""
replacement_ref = """                                        ProjectThumbnail(
                                            projectId = proj.id,
                                            modifier = Modifier
                                                .width(90.dp)
                                                .height(115.dp)
                                        )"""

text = text.replace(target_ref, replacement_ref)

with open('app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt', 'w') as f:
    f.write(text)
