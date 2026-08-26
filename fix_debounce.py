with open('app/src/main/java/com/example/studio/ui/BottomPanelDetailViews.kt', 'r') as f:
    content = f.read()

target = """                    androidx.compose.runtime.LaunchedEffect(txtInputBuf) {
                        if (txtInputBuf != selectedLayer.textContent) {
                            kotlinx.coroutines.delay(150)
                            onUpdateLayer(selectedLayer.copy(textContent = txtInputBuf))
                        }
                    }"""

replacement = """                    androidx.compose.runtime.LaunchedEffect(txtInputBuf) {
                        if (txtInputBuf != selectedLayer.textContent) {
                            kotlinx.coroutines.delay(800) // Increased debounce to prevent ANRs on low-end devices
                            onUpdateLayer(selectedLayer.copy(textContent = txtInputBuf))
                        }
                    }"""

if target in content:
    content = content.replace(target, replacement)
    print("Success")
    with open('app/src/main/java/com/example/studio/ui/BottomPanelDetailViews.kt', 'w') as f:
        f.write(content)
else:
    print("Target not found")
