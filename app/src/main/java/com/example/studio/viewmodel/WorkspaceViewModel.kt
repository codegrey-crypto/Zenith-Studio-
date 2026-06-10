package com.example.studio.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.studio.database.ProjectEntity
import com.example.studio.database.ProjectRepository
import com.example.studio.database.StudioDatabase
import com.example.studio.model.LayerSerializer
import com.example.studio.model.StudioLayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProjectRepository
    private val database = StudioDatabase.getDatabase(application)
    private val fontDao = database.customFontDao()

    val previousProjects: StateFlow<List<ProjectEntity>>
    private val _customFonts = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.studio.database.CustomFontEntity>>(emptyList())
    val customFonts: StateFlow<List<com.example.studio.database.CustomFontEntity>> = _customFonts

    private val _displayedCustomFonts = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.studio.database.CustomFontEntity>>(emptyList())
    val displayedCustomFonts: StateFlow<List<com.example.studio.database.CustomFontEntity>> = _displayedCustomFonts

    private var lastCategory = "All"
    private var lastQuery = ""

    init {
        repository = ProjectRepository(database.projectDao())
        previousProjects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        viewModelScope.launch {
            loadCustomFonts()
        }
    }

    suspend fun loadCustomFonts() {
        try {
            val fonts = withContext(Dispatchers.IO) {
                fontDao.getAllCustomFonts()
            }
            _customFonts.value = fonts
            refreshFontFilter()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateFontFilter(category: String, query: String) {
        lastCategory = category
        lastQuery = query
        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    fontDao.searchCustomFonts(category, query, limit = 150)
                }
                _displayedCustomFonts.value = results
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun refreshFontFilter() {
        updateFontFilter(lastCategory, lastQuery)
    }

    fun addCustomFont(name: String, path: String, category: String) {
        viewModelScope.launch {
            try {
                fontDao.insertCustomFont(com.example.studio.database.CustomFontEntity(path = path, name = name, category = category))
                loadCustomFonts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun batchImportFonts(
        context: android.content.Context,
        fontsToImport: List<com.example.studio.ui.FontScanner.DiscoveredFont>,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onComplete: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            var importCount = 0
            withContext(Dispatchers.IO) {
                val fontsDir = java.io.File(context.filesDir, "fonts")
                if (!fontsDir.exists()) {
                    fontsDir.mkdirs()
                }
                fontsToImport.forEachIndexed { index, discFont ->
                    try {
                        val destFile = java.io.File(fontsDir, discFont.file.name)
                        // Copy font file to secure filesDir directory
                        discFont.file.inputStream().use { input ->
                            destFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        val cleanName = discFont.name
                        val category = if (cleanName.contains("script", ignoreCase = true)) {
                            "Script"
                        } else if (cleanName.contains("hand", ignoreCase = true) || cleanName.contains("write", ignoreCase = true)) {
                            "Handwritten"
                        } else if (cleanName.contains("mono", ignoreCase = true)) {
                            "Monospace"
                        } else if (cleanName.contains("serif", ignoreCase = true)) {
                            "Serif"
                        } else {
                            "Display"
                        }
                        fontDao.insertCustomFont(
                            com.example.studio.database.CustomFontEntity(
                                path = destFile.absolutePath,
                                name = cleanName,
                                category = category
                            )
                        )
                        importCount++
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    withContext(Dispatchers.Main) {
                        onProgress(index + 1, fontsToImport.size)
                    }
                }
            }
            loadCustomFonts()
            onComplete(importCount)
        }
    }

    fun deleteCustomFont(path: String) {
        viewModelScope.launch {
            try {
                fontDao.deleteCustomFont(path)
                withContext(Dispatchers.IO) {
                    val file = java.io.File(path)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                loadCustomFonts()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveProject(
        id: String,
        name: String,
        width: Float,
        height: Float,
        layers: List<StudioLayer>,
        dpi: Int = 300,
        onComplete: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            val actualId = id.ifBlank { UUID.randomUUID().toString() }
            val actualName = name.ifBlank { "Artwork ${width.toInt()}x${height.toInt()}" }
            val entity = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                val json = LayerSerializer.serialize(layers)
                ProjectEntity(
                    id = actualId,
                    name = actualName,
                    width = width,
                    height = height,
                    timestamp = System.currentTimeMillis(),
                    layersJson = json,
                    dpi = dpi
                )
            }
            repository.saveProject(entity)
            onComplete(actualId)
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            repository.deleteProject(id)
        }
    }

    fun renameProject(entity: ProjectEntity, newName: String) {
        viewModelScope.launch {
            val updated = entity.copy(
                name = newName,
                timestamp = System.currentTimeMillis()
            )
            repository.saveProject(updated)
        }
    }
}
