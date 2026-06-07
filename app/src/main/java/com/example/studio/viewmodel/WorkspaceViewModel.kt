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
import java.util.UUID

class WorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProjectRepository
    private val database = StudioDatabase.getDatabase(application)
    private val fontDao = database.customFontDao()

    val previousProjects: StateFlow<List<ProjectEntity>>
    private val _customFonts = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.studio.database.CustomFontEntity>>(emptyList())
    val customFonts: StateFlow<List<com.example.studio.database.CustomFontEntity>> = _customFonts

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
            val fonts = fontDao.getAllCustomFonts()
            _customFonts.value = fonts
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

    fun deleteCustomFont(path: String) {
        viewModelScope.launch {
            try {
                fontDao.deleteCustomFont(path)
                // Also delete actual file
                val file = java.io.File(path)
                if (file.exists()) {
                    file.delete()
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
                    layersJson = json
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
