package com.example.studio.database

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProjectById(id: String): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    suspend fun saveProject(project: ProjectEntity) {
        projectDao.insertProject(project)
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteProjectById(id)
    }
}
// I need to patch ProjectRepository.kt properly.
