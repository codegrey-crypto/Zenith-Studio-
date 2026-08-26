import os

filepath = 'app/src/main/java/com/example/studio/model/ElementsManager.kt'
with open(filepath, 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('private val listType = Types.newParameterizedType(List::class.java, StudioLayer::class.java)',
                    'private val listType = Types.newParameterizedType(List::class.java, LayerDto::class.java)')
text = text.replace('private val adapter = moshi.adapter<List<StudioLayer>>(listType)',
                    'private val adapter = moshi.adapter<List<LayerDto>>(listType)')

# Replace loadElementsSync body
old_load = """    private fun loadElementsSync(context: Context): List<StudioLayer> {
        return try {
            val file = File(context.filesDir, "saved_elements.json")
            if (file.exists()) {
                file.source().buffer().use { source ->
                    adapter.fromJson(source) ?: emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }"""
    
new_load = """    private fun loadElementsSync(context: Context): List<StudioLayer> {
        return try {
            val file = File(context.filesDir, "saved_elements.json")
            if (file.exists()) {
                file.source().buffer().use { source ->
                    val dtoList = adapter.fromJson(source) ?: emptyList()
                    dtoList.map { it.toLayer() }
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }"""
text = text.replace(old_load, new_load)

old_save = """    private fun saveElementsSync(context: Context, elements: List<StudioLayer>) {
        try {
            val file = File(context.filesDir, "saved_elements.json")
            file.sink().buffer().use { sink ->
                adapter.toJson(sink, elements)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }"""
    
new_save = """    private fun saveElementsSync(context: Context, elements: List<StudioLayer>) {
        try {
            val file = File(context.filesDir, "saved_elements.json")
            val dtoList = elements.map { LayerDto.fromLayer(it) }
            file.sink().buffer().use { sink ->
                adapter.toJson(sink, dtoList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }"""
text = text.replace(old_save, new_save)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(text)
