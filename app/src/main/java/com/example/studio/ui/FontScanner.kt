package com.example.studio.ui

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FontScanner {

    private const val MAX_DISCOVERED_FONTS = 500

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
                    while (c.moveToNext() && discovered.size < MAX_DISCOVERED_FONTS) {
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

        if (foldersToScan.contains("DCIM")) {
            try {
                rootsToScan.add(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM))
            } catch (e: Exception) {}
            rootsToScan.add(File("/storage/emulated/0/DCIM"))
        }

        // Process any custom specified folders or subpaths
        foldersToScan.forEach { folder ->
            if (folder !in setOf("Download", "Documents", "Fonts", "Root", "MediaStore", "DCIM")) {
                val directFile = File(folder)
                if (directFile.exists() && directFile.isDirectory) {
                    rootsToScan.add(directFile)
                } else {
                    val emulatedFile = File("/storage/emulated/0", folder)
                    if (emulatedFile.exists() && emulatedFile.isDirectory) {
                        rootsToScan.add(emulatedFile)
                    } else {
                        val dlFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), folder)
                        if (dlFile.exists() && dlFile.isDirectory) {
                            rootsToScan.add(dlFile)
                        }
                    }
                }
            }
        }

        val uniqueRoots = rootsToScan.filter { it.exists() && it.isDirectory }.distinct()

        for (root in uniqueRoots) {
            if (discovered.size >= MAX_DISCOVERED_FONTS) break
            crawlDirectory(root, discovered, visitedPaths, maxDepth = 5)
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
        if (results.size >= MAX_DISCOVERED_FONTS || currentDepth > maxDepth || !dir.exists() || !dir.isDirectory) return

        val files = try {
            dir.listFiles()
        } catch (e: SecurityException) {
            null
        } ?: return

        for (f in files) {
            if (results.size >= MAX_DISCOVERED_FONTS) break
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
