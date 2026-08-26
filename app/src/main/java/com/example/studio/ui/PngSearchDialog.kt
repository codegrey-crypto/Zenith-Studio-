package com.example.studio.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.studio.model.LayerType
import com.example.studio.model.StudioLayer
import com.example.studio.viewmodel.WorkspaceViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Composable
fun PngSearchDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    coroutineScope: CoroutineScope,
    workspaceViewModel: WorkspaceViewModel,
    layers: List<StudioLayer>,
    onLayersChanged: (List<StudioLayer>) -> Unit,
    selectedLayerId: String,
    onSelectedLayerIdChanged: (String) -> Unit,
    undoStack: CappedHistoryStack,
    redoStack: CappedHistoryStack
) {
    if (!showDialog) return

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var currentTab by remember { mutableStateOf(0) } // 0: Search Web, 1: Downloaded PNGs
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
    var searchHistory by remember { mutableStateOf(getSearchHistory(context)) }
    var downloadingImageId by remember { mutableStateOf<String?> (null) }

    // Multi-source PNG search (DuckDuckGo Search Engine + Openclipart + Wikimedia)
    val executeSearch: (String) -> Unit = { query ->
        if (query.trim().isEmpty()) {
            searchResults = emptyList()
        } else {
            addSearchHistory(context, query)
            searchHistory = getSearchHistory(context)
            
            coroutineScope.launch(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    isSearching = true
                }
                
                val list = mutableListOf<Triple<String, String, String>>()
                val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"

                // --- 1. GENERAL WEB SEARCH VIA BING IMAGES (Rich, comprehensive general results) ---
                try {
                    var optimizedQuery = query.trim()
                    if (!optimizedQuery.lowercase().contains("png") && 
                        !optimizedQuery.lowercase().contains("transparent") && 
                        !optimizedQuery.lowercase().contains("clipart") && 
                        !optimizedQuery.lowercase().contains("icon")
                    ) {
                        optimizedQuery = "$optimizedQuery transparent png"
                    }
                    val encodedQuery = URLEncoder.encode(optimizedQuery, "UTF-8")
                    val urlString = "https://www.bing.com/images/async?q=$encodedQuery&async=content&first=1&count=50&cc=US&setmkt=en-US&setlang=en"
                    val connection = URL(urlString).openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", userAgent)
                    connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                    connection.setRequestProperty("Referer", "https://www.bing.com/")
                    connection.setRequestProperty("Cookie", "SRCHHPGUSR=ADLT=OFF&SRCHLANG=en; MUID=00000000000000000000000000000000; _EDGE_V=1;")
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 6000
                    connection.readTimeout = 6000
                    
                    if (connection.responseCode == 200) {
                        val htmlText = connection.inputStream.bufferedReader().use { it.readText() }
                        val iuscRegex = """class="iusc"[^>]+?m="([^"]+?)"""".toRegex()
                        val matches = iuscRegex.findAll(htmlText)
                        for (match in matches) {
                            val mAttr = match.groups[1]?.value ?: continue
                            val unescapedMAttr = mAttr.replace("&quot;", "\"")
                                                      .replace("&#39;", "'")
                                                      .replace("&#039;", "'")
                                                      .replace("&amp;", "&")
                            try {
                                val jsonObj = JSONObject(unescapedMAttr)
                                val originalUrl = jsonObj.optString("murl", "")
                                val thumbnailUrl = jsonObj.optString("turl", "")
                                var title = jsonObj.optString("t", "Asset")
                                if (title.contains(" - ")) {
                                    title = title.substringBeforeLast(" - ")
                                }
                                if (originalUrl.isNotEmpty()) {
                                    if (list.none { it.second == originalUrl }) {
                                        list.add(Triple(title.trim(), originalUrl, thumbnailUrl))
                                    }
                                }
                            } catch (jsonEx: Exception) {
                                jsonEx.printStackTrace()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // --- 2. SEARCH VIA OPENCLIPART WEB SCRAPER (100% transparent PNG vector assets) ---
                try {
                    val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
                    val urlString = "https://openclipart.org/search/?query=$encodedQuery"
                    val connection = URL(urlString).openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", userAgent)
                    connection.connectTimeout = 6000
                    connection.readTimeout = 6000
                    
                    if (connection.responseCode == 200) {
                        val htmlText = connection.inputStream.bufferedReader().use { it.readText() }
                        // Match <img src="/image/800px/ID" alt="TITLE" />
                        val imgRegex = """<img src="(/image/800px/(\d+))"\s+alt="([^"]+?)"""".toRegex()
                        val matches = imgRegex.findAll(htmlText)
                        for (match in matches) {
                            val src = match.groups[1]?.value ?: ""
                            val alt = match.groups[3]?.value ?: ""
                            if (src.isNotEmpty()) {
                                val fullPngUrl = "https://openclipart.org$src"
                                val cleanTitle = alt.replace("&#039;", "'")
                                    .replace("&quot;", "\"")
                                    .replace("&amp;", "&")
                                    .trim()
                                if (list.none { it.second == fullPngUrl }) {
                                    list.add(Triple(cleanTitle, fullPngUrl, fullPngUrl))
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // --- 3. SEARCH VIA WIKIMEDIA COMMONS API (Correct search syntax for PNG files) ---
                try {
                    val encodedQuery = URLEncoder.encode(query.trim() + " png", "UTF-8")
                    val urlString = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=$encodedQuery&gsrlimit=30&prop=imageinfo&iiprop=url&format=json&gsrnamespace=6"
                    val connection = URL(urlString).openConnection() as HttpURLConnection
                    connection.setRequestProperty("User-Agent", userAgent)
                    connection.connectTimeout = 6000
                    connection.readTimeout = 6000
                    
                    if (connection.responseCode == 200) {
                        val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                        val root = JSONObject(jsonText)
                        val queryObj = root.optJSONObject("query")
                        val pagesObj = queryObj?.optJSONObject("pages")
                        if (pagesObj != null) {
                            val keys = pagesObj.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                val pageItem = pagesObj.getJSONObject(key)
                                val filename = pageItem.optString("title", "Commons PNG").replace("File:", "")
                                val cleanTitle = filename.substringBeforeLast(".")
                                    .replace("_", " ")
                                    .replace("-", " ")
                                    .trim()
                                val imgInfoArr = pageItem.optJSONArray("imageinfo")
                                if (imgInfoArr != null && imgInfoArr.length() > 0) {
                                    val info = imgInfoArr.getJSONObject(0)
                                    val imgUrl = info.optString("url", "")
                                    if (imgUrl.isNotBlank() && imgUrl.lowercase().endsWith(".png")) {
                                        // Avoid duplicates
                                        if (list.none { it.second == imgUrl }) {
                                            list.add(Triple(cleanTitle, imgUrl, imgUrl))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                withContext(Dispatchers.Main) {
                    searchResults = list
                    isSearching = false
                }
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = SlatePanel,
            border = BorderStroke(1.2.dp, HighslateOutline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = EnergeticYellow,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                "Import PNGs from the Web",
                                style = Typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                                color = TextPrimary
                            )
                            Text(
                                "Always searching transparent backgrounds",
                                style = Typography.labelSmall.copy(fontSize = 11.sp),
                                color = EnergeticYellow
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Tab Selector (Search Web vs Downloaded PNGs)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { currentTab = 0 },
                        colors = ButtonDefaults.buttonColors(containerColor = if (currentTab == 0) EnergeticYellow else MidSlate),
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = if (currentTab == 0) DarkOnyx else TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Search Web", style = Typography.labelMedium, color = if (currentTab == 0) DarkOnyx else TextPrimary)
                    }
                    Button(
                        onClick = { 
                            currentTab = 1 
                            val webAssetsDir = File(context.filesDir, "web_assets")
                            val files = webAssetsDir.listFiles()?.filter { it.extension == "png" }?.sortedByDescending { it.lastModified() } ?: emptyList()
                            searchResults = files.map { file ->
                                val dateStr = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))
                                Triple("Downloaded PNG ($dateStr)", file.absolutePath, file.absolutePath)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (currentTab == 1) EnergeticYellow else MidSlate),
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, tint = if (currentTab == 1) DarkOnyx else TextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Downloaded PNGs", style = Typography.labelMedium, color = if (currentTab == 1) DarkOnyx else TextPrimary)
                    }
                }

                Spacer(Modifier.height(8.dp))

                if (currentTab == 0) {
                    // Search Bar Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                "Type search terms (e.g. 'like icon', 'star' ...)",
                                style = Typography.bodyMedium,
                                color = TextSecondary
                            )
                        },
                        textStyle = Typography.bodyMedium.copy(color = TextPrimary),
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    searchQuery = ""
                                    searchResults = emptyList()
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EnergeticYellow,
                            unfocusedBorderColor = HighslateOutline,
                            focusedContainerColor = Color(0xFF131317),
                            unfocusedContainerColor = Color(0xFF131317)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { executeSearch(searchQuery) }
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            imeAction = androidx.compose.ui.text.input.ImeAction.Search
                        )
                    )

                    // Search Action Button
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { executeSearch(searchQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = EnergeticYellow),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = DarkOnyx, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Search Web PNGs", color = DarkOnyx, fontWeight = FontWeight.Bold, style = Typography.labelMedium)
                    }

                    Spacer(Modifier.height(12.dp))

                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(color = EnergeticYellow, strokeWidth = 3.dp)
                                Text("Scanning web for transparent PNGs...", style = Typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    } else if (searchQuery.trim().isEmpty() && searchHistory.isNotEmpty()) {
                        // Search History Section
                        Text(
                            "Search History",
                            style = Typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(searchHistory) { historyItem ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF1E1E24))
                                        .clickable {
                                            searchQuery = historyItem
                                            executeSearch(historyItem)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                                        Text(historyItem, style = Typography.bodyMedium, color = TextPrimary)
                                    }
                                    IconButton(
                                        onClick = {
                                            deleteSearchHistory(context, historyItem)
                                            searchHistory = getSearchHistory(context)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFEF5350), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    } else if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (searchQuery.isEmpty()) "Enter a query to find PNG transparent assets." else "No transparent PNGs found. Try searching something else!",
                                style = Typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        // 3-column Checkerboard Image Grid
                        Text(
                            "Tap an asset to import into your workspace:",
                            style = Typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(searchResults) { img ->
                                val isThisDownloading = downloadingImageId == img.second
                                
                                Card(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.2.dp, if (isThisDownloading) EnergeticYellow else HighslateOutline), RoundedCornerShape(8.dp))
                                        .clickable(enabled = downloadingImageId == null) {
                                            downloadingImageId = img.second
                                            downloadAndImportPng(
                                                context = context,
                                                img = img,
                                                coroutineScope = coroutineScope,
                                                layers = layers,
                                                onLayersChanged = onLayersChanged,
                                                onSelectedLayerIdChanged = onSelectedLayerIdChanged,
                                                undoStack = undoStack,
                                                redoStack = redoStack,
                                                onFinished = {
                                                    downloadingImageId = null
                                                    onDismiss()
                                                }
                                            )
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .checkerboard(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(if (img.third.isNotEmpty()) img.third else img.second)
                                                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = img.first,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            contentScale = ContentScale.Fit
                                        )

                                        if (isThisDownloading) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.6f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator(
                                                    color = EnergeticYellow,
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.5.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Downloaded PNGs tab
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No previously downloaded PNGs found. Use 'Search Web' tab to find and import PNGs!",
                                style = Typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            "Tap a downloaded asset to reuse it instantly:",
                            style = Typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(searchResults) { img ->
                                Card(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.2.dp, HighslateOutline), RoundedCornerShape(8.dp))
                                        .clickable {
                                            downloadAndImportPng(
                                                context = context,
                                                img = img,
                                                coroutineScope = coroutineScope,
                                                layers = layers,
                                                onLayersChanged = onLayersChanged,
                                                onSelectedLayerIdChanged = onSelectedLayerIdChanged,
                                                undoStack = undoStack,
                                                redoStack = redoStack,
                                                onFinished = {
                                                    onDismiss()
                                                }
                                            )
                                        },
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .checkerboard(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(img.second)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = img.first,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            contentScale = ContentScale.Fit
                                        )

                                        // Delete Overlay
                                        IconButton(
                                            onClick = {
                                                try {
                                                    File(img.second).delete()
                                                    val webAssetsDir = File(context.filesDir, "web_assets")
                                                    val files = webAssetsDir.listFiles()?.filter { it.extension == "png" }?.sortedByDescending { it.lastModified() } ?: emptyList()
                                                    searchResults = files.map { file ->
                                                        val dateStr = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(file.lastModified()))
                                                        Triple("Downloaded PNG ($dateStr)", file.absolutePath, file.absolutePath)
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom High-Performance Checkerboard Background Modifier Draw behind
fun Modifier.checkerboard(
    squareSize: Float = 16f,
    color1: Color = Color(0xFF18181F),
    color2: Color = Color(0xFF262633)
) = this.drawBehind {
    val width = size.width
    val height = size.height
    val cols = (width / squareSize).toInt() + 1
    val rows = (height / squareSize).toInt() + 1
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val color = if ((r + c) % 2 == 0) color1 else color2
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(c * squareSize, r * squareSize),
                size = androidx.compose.ui.geometry.Size(squareSize, squareSize)
            )
        }
    }
}

private fun getSearchHistory(context: Context): List<String> {
    val prefs = context.getSharedPreferences("png_search_history", Context.MODE_PRIVATE)
    val historyString = prefs.getString("history", "") ?: ""
    return if (historyString.isBlank()) emptyList() else historyString.split(";;;")
}

private fun addSearchHistory(context: Context, query: String) {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return
    val prefs = context.getSharedPreferences("png_search_history", Context.MODE_PRIVATE)
    val current = getSearchHistory(context).toMutableList()
    current.remove(trimmed)
    current.add(0, trimmed)
    val updated = current.take(15)
    prefs.edit().putString("history", updated.joinToString(";;;")).apply()
}

private fun deleteSearchHistory(context: Context, query: String) {
    val prefs = context.getSharedPreferences("png_search_history", Context.MODE_PRIVATE)
    val current = getSearchHistory(context).toMutableList()
    current.remove(query)
    prefs.edit().putString("history", current.joinToString(";;;")).apply()
}

private fun getImageAspectRatioDimensions(filePath: String, targetMaxDim: Float = 500f): Pair<Float, Float> {
    var width = targetMaxDim
    var height = targetMaxDim
    try {
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeFile(filePath, options)
        val imageW = options.outWidth
        val imageH = options.outHeight
        if (imageW > 0 && imageH > 0) {
            val aspectRatio = imageW.toFloat() / imageH.toFloat()
            if (aspectRatio > 1.0f) {
                width = targetMaxDim
                height = targetMaxDim / aspectRatio
            } else {
                height = targetMaxDim
                width = targetMaxDim * aspectRatio
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return Pair(width, height)
}

private fun downloadAndImportPng(
    context: Context,
    img: Triple<String, String, String>,
    coroutineScope: CoroutineScope,
    layers: List<StudioLayer>,
    onLayersChanged: (List<StudioLayer>) -> Unit,
    onSelectedLayerIdChanged: (String) -> Unit,
    undoStack: CappedHistoryStack,
    redoStack: CappedHistoryStack,
    onFinished: () -> Unit
) {
    coroutineScope.launch(Dispatchers.IO) {
        val isLocal = img.second.startsWith("/") || File(img.second).exists()
        if (isLocal) {
            val localPath = img.second
            val (initW, initH) = getImageAspectRatioDimensions(localPath, 450f)
            withContext(Dispatchers.Main) {
                undoStack.add(layers)
                redoStack.clear()
                val newL = StudioLayer(
                    name = img.first,
                    type = LayerType.IMAGE_CARD,
                    positionX = 150f,
                    positionY = 200f,
                    width = initW,
                    height = initH,
                    baseColor = Color.Transparent,
                    imageUri = localPath,
                    isAspectLocked = true
                )
                onLayersChanged(listOf(newL) + layers)
                onSelectedLayerIdChanged(newL.id)
                onFinished()
                Toast.makeText(context, "Transparent layer imported successfully!", Toast.LENGTH_SHORT).show()
            }
            return@launch
        }

        var localFile: File? = null
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Downloading transparent PNG layer...", Toast.LENGTH_SHORT).show()
            }
            // Use desktop User-Agent to match search and bypass mobile/redirect blocks
            val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36"

            var connection = URL(img.second).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", userAgent)
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.instanceFollowRedirects = true

            var status = connection.responseCode
            var redirectCount = 0
            var finalUrl = img.second
            while (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                   status == HttpURLConnection.HTTP_MOVED_PERM || 
                   status == HttpURLConnection.HTTP_SEE_OTHER ||
                   status == 307 || status == 308) {
                if (redirectCount > 8) break
                val loc = connection.getHeaderField("Location") ?: break
                finalUrl = if (loc.startsWith("/")) {
                    val baseU = URL(finalUrl)
                    baseU.protocol + "://" + baseU.host + loc
                } else loc
                val nextConn = URL(finalUrl).openConnection() as HttpURLConnection
                nextConn.setRequestProperty("User-Agent", userAgent)
                nextConn.connectTimeout = 15000
                nextConn.readTimeout = 15000
                nextConn.instanceFollowRedirects = true
                connection = nextConn
                status = connection.responseCode
                redirectCount++
            }

            if (status !in 200..299) {
                throw java.io.IOException("Server returned status code $status")
            }

            val webAssetsDir = File(context.filesDir, "web_assets")
            if (!webAssetsDir.exists()) webAssetsDir.mkdirs()

            val downloadedFile = File(webAssetsDir, "transparent_${System.currentTimeMillis()}.png")
            localFile = downloadedFile
            connection.inputStream.use { input ->
                downloadedFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Validate that the downloaded file is a valid image
            val sizeOptions = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            android.graphics.BitmapFactory.decodeFile(downloadedFile.absolutePath, sizeOptions)
            if (sizeOptions.outWidth <= 0 || sizeOptions.outHeight <= 0) {
                throw java.io.IOException("Downloaded file is not a valid image format or is corrupted.")
            }

            // Correct size estimation with respect to image's aspect ratio
            val (initW, initH) = getImageAspectRatioDimensions(downloadedFile.absolutePath, 450f)

            withContext(Dispatchers.Main) {
                undoStack.add(layers)
                redoStack.clear()
                val newL = StudioLayer(
                    name = img.first,
                    type = LayerType.IMAGE_CARD,
                    positionX = 150f,
                    positionY = 200f,
                    width = initW,
                    height = initH,
                    baseColor = Color.Transparent,
                    imageUri = downloadedFile.absolutePath,
                    isAspectLocked = true
                )
                onLayersChanged(listOf(newL) + layers)
                onSelectedLayerIdChanged(newL.id)
                onFinished()
                Toast.makeText(context, "Transparent layer imported successfully!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Clean up the invalid file if it was created
            try {
                localFile?.let {
                    if (it.exists()) {
                        it.delete()
                    }
                }
            } catch (cleanupEx: Exception) {
                cleanupEx.printStackTrace()
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                onFinished()
            }
        }
    }
}
