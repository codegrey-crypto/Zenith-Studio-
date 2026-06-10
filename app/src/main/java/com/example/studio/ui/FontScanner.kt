package com.example.studio.ui

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FontScanner {

    data class DiscoveredFont(
        val name: String,
        val filePath: String,
        val file: File
    )

    /**
     * Recursively traverses external and standard public storage directories
     * to find .ttf and .otf font files.
     */
    suspend fun scanLocalFonts(
        context: Context,
        foldersToScan: Set<String> = setOf("Download", "Fonts")
    ): List<DiscoveredFont> = withContext(Dispatchers.IO) {
        val discovered = mutableListOf<DiscoveredFont>()
        val visitedPaths = mutableSetOf<String>()

        // 1. MediaStore scan (highly efficient and permission-friendly way)
        if (foldersToScan.contains("MediaStore")) {
            try {
                val projection = arrayOf(
                    MediaStore.Files.FileColumns.DATA,
                    MediaStore.Files.FileColumns.DISPLAY_NAME
                )
                val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.ttf' OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE '%.otf'"
                val queryUri = MediaStore.Files.getContentUri("external")
                val cursor = context.contentResolver.query(queryUri, projection, selection, null, null)
                cursor?.use { c ->
                    val dataIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    val nameIndex = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    while (c.moveToNext()) {
                        val path = c.getString(dataIndex)
                        val name = c.getString(nameIndex)
                        if (!path.isNullOrEmpty() && !visitedPaths.contains(path)) {
                            val file = File(path)
                            if (file.exists() && file.isFile) {
                                visitedPaths.add(path)
                                val cleanName = file.nameWithoutExtension.replace("_", " ").replace("-", " ")
                                discovered.add(DiscoveredFont(cleanName, path, file))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Fallback recursive file system crawl of standard public storage paths
        val rootsToScan = mutableListOf<File>()
        
        if (foldersToScan.contains("Root")) {
            try {
                rootsToScan.add(Environment.getExternalStorageDirectory())
            } catch (e: Exception) {}
            rootsToScan.add(File("/storage/emulated/0"))
        }

        if (foldersToScan.contains("Download")) {
            try {
                rootsToScan.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
            } catch (e: Exception) {}
            rootsToScan.add(File("/storage/emulated/0/Download"))
        }

        if (foldersToScan.contains("Documents")) {
            try {
                rootsToScan.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS))
            } catch (e: Exception) {}
            rootsToScan.add(File("/storage/emulated/0/Documents"))
        }

        if (foldersToScan.contains("Fonts")) {
            rootsToScan.add(File("/storage/emulated/0/Fonts"))
        }

        val uniqueRoots = rootsToScan.filter { it.exists() && it.isDirectory }.distinct()

        for (root in uniqueRoots) {
            crawlDirectory(root, discovered, visitedPaths, maxDepth = 6)
        }

        discovered.sortedBy { it.name }
    }

    private fun crawlDirectory(
        dir: File,
        results: MutableList<DiscoveredFont>,
        visited: MutableSet<String>,
        currentDepth: Int = 0,
        maxDepth: Int = 5
    ) {
        if (currentDepth > maxDepth || !dir.exists() || !dir.isDirectory) return

        val files = try {
            dir.listFiles()
        } catch (e: SecurityException) {
            null
        } ?: return

        for (f in files) {
            if (f.isDirectory) {
                // Skip hidden folders or system caches to keep exploration fast
                val dName = f.name.lowercase()
                if (!dName.startsWith(".") && dName != "android" && dName != "cache") {
                    crawlDirectory(f, results, visited, currentDepth + 1, maxDepth)
                }
            } else if (f.isFile) {
                val path = f.absolutePath
                if (!visited.contains(path)) {
                    val ext = f.extension.lowercase()
                    if (ext == "ttf" || ext == "otf") {
                        visited.add(path)
                        val name = f.nameWithoutExtension.replace("_", " ").replace("-", " ")
                        results.add(DiscoveredFont(name, path, f))
                    }
                }
            }
        }
    }
}
