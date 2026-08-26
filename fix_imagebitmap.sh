#!/bin/bash
sed -i 's/val imgBmp = androidx.compose.ui.graphics.asImageBitmap(adjustedBmp)/val imgBmp = adjustedBmp!!.asImageBitmap()/g' app/src/main/java/com/example/studio/ui/WorkspaceScreen.kt
