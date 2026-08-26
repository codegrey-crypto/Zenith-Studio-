import re

with open('app/src/main/java/com/example/studio/database/ProjectRepository.kt', 'r') as f:
    text = f.read()

target = """    suspend fun saveProject(project: ProjectEntity) {"""
replacement = """    suspend fun getProjectById(id: String): ProjectEntity? {
        return projectDao.getProjectById(id)
    }

    suspend fun saveProject(project: ProjectEntity) {"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/studio/database/ProjectRepository.kt', 'w') as f:
    f.write(text)
