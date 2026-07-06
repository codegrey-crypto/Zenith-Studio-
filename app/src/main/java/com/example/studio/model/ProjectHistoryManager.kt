package com.example.studio.model

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.studio.ui.ArtboardData
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@JsonClass(generateAdapter = true)
data class ProjectHistoryFrameDto(
    val artboards: List<ArtboardDto>,
    val selectedArtboardId: String,
    val selectedLayerId: String,
    val projectName: String,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val projectDpi: Int
)

@JsonClass(generateAdapter = true)
data class ProjectHistoryTreeDto(
    val undoStack: List<ProjectHistoryFrameDto>,
    val current: ProjectHistoryFrameDto?,
    val redoStack: List<ProjectHistoryFrameDto>
)

data class ProjectMemento(
    val artboards: List<ArtboardData>,
    val selectedArtboardId: String,
    val selectedLayerId: String,
    val projectName: String,
    val canvasWidth: Float,
    val canvasHeight: Float,
    val projectDpi: Int
) {
    fun toDto(): ProjectHistoryFrameDto {
        return ProjectHistoryFrameDto(
            artboards = artboards.map { art ->
                ArtboardDto(
                    id = art.id,
                    name = art.name,
                    width = art.width,
                    height = art.height,
                    offsetX = art.offsetX,
                    offsetY = art.offsetY,
                    layers = art.layers.map { LayerDto.fromLayer(it) }
                )
            },
            selectedArtboardId = selectedArtboardId,
            selectedLayerId = selectedLayerId,
            projectName = projectName,
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            projectDpi = projectDpi
        )
    }

    companion object {
        fun fromDto(dto: ProjectHistoryFrameDto): ProjectMemento {
            return ProjectMemento(
                artboards = dto.artboards.map { artDto ->
                    ArtboardData(
                        id = artDto.id,
                        name = artDto.name,
                        width = artDto.width,
                        height = artDto.height,
                        offsetX = artDto.offsetX,
                        offsetY = artDto.offsetY,
                        layers = artDto.layers.map { it.toLayer() }
                    )
                },
                selectedArtboardId = dto.selectedArtboardId,
                selectedLayerId = dto.selectedLayerId,
                projectName = dto.projectName,
                canvasWidth = dto.canvasWidth,
                canvasHeight = dto.canvasHeight,
                projectDpi = dto.projectDpi
            )
        }
    }
}

object ProjectHistoryPersistence {
    private val moshi: Moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(ProjectHistoryTreeDto::class.java)

    suspend fun saveHistoryTree(
        context: Context,
        projectId: String,
        tree: ProjectHistoryTreeDto
    ) {
        if (projectId.isBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val directory = File(context.filesDir, "history")
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                val tempFile = File(directory, "history_${projectId}.tmp")
                val finalFile = File(directory, "history_${projectId}.json")
                
                val json = adapter.toJson(tree)
                if (json != null) {
                    tempFile.writeText(json)
                    if (tempFile.exists()) {
                        // Atomic/Safe write swap to prevent partial writes if crash occurs
                        val success = tempFile.renameTo(finalFile)
                        if (!success) {
                            tempFile.copyTo(finalFile, overwrite = true)
                            tempFile.delete()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun loadHistoryTree(
        context: Context,
        projectId: String
    ): ProjectHistoryTreeDto? {
        if (projectId.isBlank()) return null
        return withContext(Dispatchers.IO) {
            try {
                val finalFile = File(File(context.filesDir, "history"), "history_${projectId}.json")
                if (finalFile.exists()) {
                    val json = finalFile.readText()
                    adapter.fromJson(json)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    suspend fun deleteHistoryTree(context: Context, projectId: String) {
        if (projectId.isBlank()) return
        withContext(Dispatchers.IO) {
            try {
                val finalFile = File(File(context.filesDir, "history"), "history_${projectId}.json")
                if (finalFile.exists()) {
                    finalFile.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

class ProjectHistoryManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val maxLimit: Int = 40
) {
    val undoStack = mutableStateListOf<ProjectMemento>()
    val redoStack = mutableStateListOf<ProjectMemento>()

    var canUndo by mutableStateOf(false)
    var canRedo by mutableStateOf(false)

    private fun updateStates() {
        canUndo = undoStack.isNotEmpty()
        canRedo = redoStack.isNotEmpty()
    }

    fun pushState(memento: ProjectMemento) {
        val lastState = undoStack.lastOrNull()
        if (lastState != null && areMementosEqual(lastState, memento)) {
            return
        }
        undoStack.add(memento)
        while (undoStack.size > maxLimit) {
            undoStack.removeAt(0)
        }
        redoStack.clear()
        updateStates()
    }

    fun performUndo(currentState: ProjectMemento): ProjectMemento? {
        if (undoStack.isEmpty()) return null
        val prev = undoStack.removeAt(undoStack.size - 1)
        redoStack.add(currentState)
        updateStates()
        return prev
    }

    fun performRedo(currentState: ProjectMemento): ProjectMemento? {
        if (redoStack.isEmpty()) return null
        val next = redoStack.removeAt(redoStack.size - 1)
        undoStack.add(currentState)
        updateStates()
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        updateStates()
    }

    fun serializeAndSaveHistory(projectId: String, currentState: ProjectMemento) {
        if (projectId.isBlank()) return
        
        val undoSnapshot = undoStack.toList()
        val redoSnapshot = redoStack.toList()

        scope.launch(Dispatchers.IO) {
            try {
                val treeDto = ProjectHistoryTreeDto(
                    undoStack = undoSnapshot.map { it.toDto() },
                    current = currentState.toDto(),
                    redoStack = redoSnapshot.map { it.toDto() }
                )
                ProjectHistoryPersistence.saveHistoryTree(context, projectId, treeDto)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadHistoryAndRestore(
        projectId: String,
        onRestore: (ProjectMemento, Boolean) -> Unit
    ) {
        if (projectId.isBlank()) {
            clear()
            return
        }
        scope.launch(Dispatchers.Main) {
            val restoredTree = withContext(Dispatchers.IO) {
                ProjectHistoryPersistence.loadHistoryTree(context, projectId)
            }
            if (restoredTree != null) {
                undoStack.clear()
                undoStack.addAll(restoredTree.undoStack.map { ProjectMemento.fromDto(it) })
                
                redoStack.clear()
                redoStack.addAll(restoredTree.redoStack.map { ProjectMemento.fromDto(it) })
                
                updateStates()

                val currentFrame = restoredTree.current
                if (currentFrame != null) {
                    val memento = ProjectMemento.fromDto(currentFrame)
                    onRestore(memento, true)
                } else {
                    onRestore(ProjectMemento(emptyList(), "default", "", "Artwork", 1080f, 1350f, 300), false)
                }
            } else {
                clear()
                onRestore(ProjectMemento(emptyList(), "default", "", "Artwork", 1080f, 1350f, 300), false)
            }
        }
    }

    private fun areMementosEqual(a: ProjectMemento, b: ProjectMemento): Boolean {
        if (a.selectedArtboardId != b.selectedArtboardId) return false
        if (a.projectName != b.projectName) return false
        if (a.canvasWidth != b.canvasWidth || a.canvasHeight != b.canvasHeight) return false
        if (a.projectDpi != b.projectDpi) return false
        if (a.artboards.size != b.artboards.size) return false
        
        for (i in a.artboards.indices) {
            val artA = a.artboards[i]
            val artB = b.artboards[i]
            if (artA.id != artB.id || artA.name != artB.name || artA.width != artB.width || artA.height != artB.height) return false
            if (artA.layers.size != artB.layers.size) return false
            for (j in artA.layers.indices) {
                val layA = artA.layers[j]
                val layB = artB.layers[j]
                if (layA.id != layB.id || layA.name != layB.name || layA.isVisible != layB.isVisible || layA.opacity != layB.opacity) return false
                if (layA.positionX != layB.positionX || layA.positionY != layB.positionY) return false
                if (layA.width != layB.width || layA.height != layB.height) return false
                if (layA.scaleX != layB.scaleX || layA.scaleY != layB.scaleY) return false
                if (layA.rotation != layB.rotation) return false
                if (layA.brushPoints.size != layB.brushPoints.size) return false
            }
        }
        return true
    }
}
