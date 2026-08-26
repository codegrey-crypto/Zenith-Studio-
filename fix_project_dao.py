import re

with open('app/src/main/java/com/example/studio/database/ProjectDao.kt', 'r') as f:
    text = f.read()

target = """    @Query("SELECT * FROM projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>"""

replacement = """    @Query("SELECT id, name, width, height, timestamp, '' as layersJson, dpi FROM projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?"""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/studio/database/ProjectDao.kt', 'w') as f:
    f.write(text)
