#!/bin/bash
sed -i 's/"Twirl", "Spherize", "GaussianBlur", "SmartSharpen"/"Twirl", "Spherize", "SmartSharpen"/g' app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt

sed -i 's/"Stroke", "GlassMorphism", "ReededGlass", "PixelStretch"/"Stroke", "GlassMorphism", "ReededGlass", "PixelStretch", "GaussianBlur", "RadialBlur", "LensBlur", "MotionBlur"/g' app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt
