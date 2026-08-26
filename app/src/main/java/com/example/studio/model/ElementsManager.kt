package com.example.studio.model

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object ElementsManager {
    suspend fun saveElement(context: Context, layer: StudioLayer) {
        withContext(Dispatchers.IO) {
            val elements = loadElementsSync(context).toMutableList()
            elements.add(layer)
            saveElementsSync(context, elements)
        }
    }
        
    suspend fun removeElement(context: Context, layerId: String) {
        withContext(Dispatchers.IO) {
            val elements = loadElementsSync(context).toMutableList()
            elements.removeAll { it.id == layerId }
            saveElementsSync(context, elements)
        }
    }

    suspend fun loadElements(context: Context): List<StudioLayer> {
        return withContext(Dispatchers.IO) {
            loadElementsSync(context)
        }
    }

    private fun loadElementsSync(context: Context): List<StudioLayer> {
        return try {
            val file = File(context.filesDir, "saved_elements.json")
            if (file.exists()) {
                val json = file.readText()
                LayerSerializer.deserialize(json)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveElementsSync(context: Context, elements: List<StudioLayer>) {
        try {
            val file = File(context.filesDir, "saved_elements.json")
            val json = LayerSerializer.serialize(elements)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
