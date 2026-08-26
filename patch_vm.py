import re

with open('app/src/main/java/com/example/studio/viewmodel/WorkspaceViewModel.kt', 'r') as f:
    text = f.read()

target = """    fun saveWorkspace("""
replacement = """    suspend fun getProjectById(id: String): ProjectEntity? {
        return repository.getProjectById(id)
    }

    fun saveWorkspace("""

text = text.replace(target, replacement)

with open('app/src/main/java/com/example/studio/viewmodel/WorkspaceViewModel.kt', 'w') as f:
    f.write(text)
