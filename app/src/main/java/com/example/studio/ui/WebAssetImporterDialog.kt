package com.example.studio.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.horizontalScroll

@Composable
fun WebAssetImporterDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    coroutineScope: CoroutineScope,
    workspaceViewModel: WorkspaceViewModel,
    layers: List<StudioLayer>,
    onLayersChanged: (List<StudioLayer>) -> Unit,
    selectedLayerId: String,
    onSelectedLayerIdChanged: (String) -> Unit,
    undoStack: CappedHistoryStack,
    redoStack: CappedHistoryStack,
    onImportPdfAsArtboards: (List<ArtboardData>) -> Unit,
    onShowAdvancedImport: (() -> Unit)? = null,
    onLaunchLocalPhotoPicker: (() -> Unit)? = null
) {
    if (!showDialog) return

    val context = LocalContext.current

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = SlatePanel,
            border = BorderStroke(1.2.dp, HighslateOutline)
        ) {
            var browserTab by remember { mutableStateOf(0) }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = EnergeticYellow)
                        Text("🌐 Web Asset Importer", style = Typography.titleMedium, color = TextPrimary)
                        
                        if (onShowAdvancedImport != null) {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = onShowAdvancedImport,
                                colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(12.dp), tint = EnergeticYellow)
                                Spacer(Modifier.width(4.dp))
                                Text("Local/PSD/PDF", style = Typography.labelSmall.copy(fontSize = 10.sp), color = EnergeticYellow)
                            }
                        }

                        if (onLaunchLocalPhotoPicker != null) {
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onDismiss()
                                    onLaunchLocalPhotoPicker()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Image, null, modifier = Modifier.size(12.dp), tint = EnergeticYellow)
                                Spacer(Modifier.width(4.dp))
                                Text("Import Gallery Images", style = Typography.labelSmall.copy(fontSize = 10.sp), color = EnergeticYellow)
                            }
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF131317))
                        .padding(2.dp)
                ) {
                    val tabs = listOf("Web Explorer (Browser)", "Instant Stockroom", "Direct Link Grabber")
                    tabs.forEachIndexed { idx, title ->
                        val selected = browserTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Color(0xFF1E1E24) else Color.Transparent)
                                .clickable { browserTab = idx }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) EnergeticYellow else TextSecondary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                when (browserTab) {
                    1 -> {
                        var imageSearchQuery by remember { mutableStateOf("") }
                        var isSearchingImages by remember { mutableStateOf(false) }
                        var stockPhotosList by remember { mutableStateOf<List<Triple<String, String, String>>>(emptyList()) }
                        val quickCategories = listOf("Abstract", "Neon", "Nature", "Wallpaper", "Texture", "Space")

                        // Dynamic fetch logic
                        val executeImageSearch: (String) -> Unit = { query ->
                            coroutineScope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) {
                                    isSearchingImages = true
                                }
                                val finalQuery = if (query.isBlank()) "aesthetic minimal gradient" else query
                                val photos = mutableListOf<Triple<String, String, String>>()

                                // 1. Match from our curated stock database for instantaneous HQ results
                                val localCuratedMatches = searchCuratedPhotos(finalQuery)
                                photos.addAll(localCuratedMatches)

                                // 2. Robust fetch via Unsplash public NAPI (graceful recovery on auth limits / 401/403)
                                try {
                                    val encodedQuery = URLEncoder.encode(finalQuery, "UTF-8")
                                    val urlString = "https://unsplash.com/napi/search/photos?query=$encodedQuery&per_page=24"
                                    val connection = URL(urlString).openConnection() as HttpURLConnection
                                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                                    connection.connectTimeout = 5000
                                    connection.readTimeout = 5000

                                    if (connection.responseCode == 200) {
                                        val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                                        val root = JSONObject(jsonText)
                                        val resultsArr = root.optJSONArray("results")
                                        if (resultsArr != null) {
                                            for (i in 0 until resultsArr.length()) {
                                                val item = resultsArr.getJSONObject(i)
                                                val desc = item.optString("alt_description", "Web Stock Photo")
                                                val urls = item.optJSONObject("urls")
                                                val regular = urls?.optString("regular", "") ?: ""
                                                val authorObj = item.optJSONObject("user")
                                                val authorName = authorObj?.optString("name", "Unsplash Author") ?: "Unsplash Creator"
                                                if (regular.isNotBlank()) {
                                                    val cleanDesc = desc.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                    if (photos.none { it.second == regular }) {
                                                        photos.add(Triple(cleanDesc, regular, authorName))
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // On non-200 (including 401), query Wikimedia Commons for dynamic live searches
                                        val wikiResults = searchWikimediaCommons(finalQuery)
                                        for (wp in wikiResults) {
                                            if (photos.none { it.second == wp.second }) {
                                                photos.add(wp)
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // Fallback to Wikimedia Commons on connection issues
                                    val wikiResults = searchWikimediaCommons(finalQuery)
                                    for (wp in wikiResults) {
                                        if (photos.none { it.second == wp.second }) {
                                            photos.add(wp)
                                        }
                                    }
                                }

                                // 3. Ensure some beautiful results are loaded
                                if (photos.isEmpty()) {
                                    photos.addAll(CURATED_PHOTOS.shuffled().take(8))
                                }

                                withContext(Dispatchers.Main) {
                                    stockPhotosList = photos
                                    isSearchingImages = false
                                }
                            }
                        }

                        // Load initial aesthetic photos on dialog entry
                        LaunchedEffect(Unit) {
                            if (stockPhotosList.isEmpty()) {
                                executeImageSearch("aesthetic minimal gradient")
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = imageSearchQuery,
                                onValueChange = { 
                                    imageSearchQuery = it
                                    if (it.length > 2) {
                                        executeImageSearch(it)
                                    }
                                },
                                placeholder = { Text("Search online stock images...", style = Typography.bodyMedium, color = TextSecondary) },
                                textStyle = Typography.bodyMedium.copy(color = TextPrimary),
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                                trailingIcon = {
                                    if (imageSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            imageSearchQuery = ""
                                            executeImageSearch("")
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EnergeticYellow,
                                    unfocusedBorderColor = HighslateOutline
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(Modifier.height(8.dp))

                            androidx.compose.foundation.lazy.LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(quickCategories) { cat ->
                                    val isCurrent = imageSearchQuery == cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isCurrent) EnergeticYellow.copy(0.2f) else Color(0xFF1E1E24))
                                            .border(BorderStroke(1.dp, if (isCurrent) EnergeticYellow else Color.Transparent), RoundedCornerShape(12.dp))
                                            .clickable { 
                                                val nextQuery = if (isCurrent) "" else cat
                                                imageSearchQuery = nextQuery
                                                executeImageSearch(nextQuery)
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(cat, style = Typography.labelSmall, color = if (isCurrent) EnergeticYellow else TextPrimary)
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Online Stock Discoveries:", style = Typography.labelSmall, color = TextSecondary)
                                if (isSearchingImages) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = EnergeticYellow, strokeWidth = 1.6.dp)
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            if (stockPhotosList.isEmpty() && !isSearchingImages) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No images found. Adjust search query.", style = Typography.bodyMedium, color = TextSecondary)
                                }
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(stockPhotosList) { img ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    downloadAndSpawnImage(context, img, coroutineScope, layers, onLayersChanged, onSelectedLayerIdChanged, undoStack, redoStack, onDismiss)
                                                },
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                                            border = BorderStroke(1.dp, HighslateOutline.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(54.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF131317)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = EnergeticYellow)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(img.first, style = Typography.bodyMedium.copy(fontSize = 13.sp), color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                                                    Text("By ${img.third} via Unsplash", style = Typography.labelSmall, color = TextSecondary)
                                                }
                                                Button(
                                                    onClick = {
                                                        downloadAndSpawnImage(context, img, coroutineScope, layers, onLayersChanged, onSelectedLayerIdChanged, undoStack, redoStack, onDismiss)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = EnergeticYellow),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                                ) {
                                                    Text("IMPORT", style = Typography.labelSmall, color = DarkOnyx, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    0 -> {
                        val browserContext = LocalContext.current
                        var webUrlInput by remember { mutableStateOf("https://unsplash.com") }
                        var webUrlTargetToLoad by remember { mutableStateOf("https://unsplash.com") }
                        var isWebLoading by remember { mutableStateOf(false) }
                        var webViewInstance by remember { mutableStateOf<WebView?>(null) }

                        LaunchedEffect(webUrlTargetToLoad) {
                            webViewInstance?.let { view ->
                                if (view.url != webUrlTargetToLoad) {
                                    view.loadUrl(webUrlTargetToLoad)
                                }
                            }
                        }

                        Column(modifier = Modifier.fillMaxSize()) {
                            // Immersive Control Row
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { 
                                        webViewInstance?.let {
                                            if (it.canGoBack()) it.goBack()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp).background(Color(0xFF1E1E24), RoundedCornerShape(18.dp))
                                ) {
                                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = EnergeticYellow, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { 
                                        webViewInstance?.let {
                                            if (it.canGoForward()) it.goForward()
                                        }
                                    },
                                    modifier = Modifier.size(36.dp).background(Color(0xFF1E1E24), RoundedCornerShape(18.dp))
                                ) {
                                    Icon(Icons.Default.ArrowForward, contentDescription = "Forward", tint = EnergeticYellow, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { webViewInstance?.reload() },
                                    modifier = Modifier.size(36.dp).background(Color(0xFF1E1E24), RoundedCornerShape(18.dp))
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = EnergeticYellow, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = { 
                                        webUrlInput = "https://unsplash.com"
                                        webUrlTargetToLoad = "https://unsplash.com"
                                    },
                                    modifier = Modifier.size(36.dp).background(Color(0xFF1E1E24), RoundedCornerShape(18.dp))
                                ) {
                                    Icon(Icons.Default.Home, contentDescription = "Home", tint = EnergeticYellow, modifier = Modifier.size(16.dp))
                                }

                                OutlinedTextField(
                                    value = webUrlInput,
                                    onValueChange = { webUrlInput = it },
                                    placeholder = { Text("Type URL or Search...", style = Typography.bodyMedium, color = TextSecondary) },
                                    textStyle = Typography.bodyMedium.copy(color = TextPrimary, fontSize = 11.sp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EnergeticYellow,
                                        unfocusedBorderColor = HighslateOutline
                                    ),
                                    modifier = Modifier.weight(1f).height(40.dp),
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            var cleanUri = webUrlInput.trim()
                                            if (!cleanUri.startsWith("http://") && !cleanUri.startsWith("https://")) {
                                                cleanUri = "https://www.google.com/search?q=" + URLEncoder.encode(cleanUri, "UTF-8")
                                            }
                                            webUrlInput = cleanUri
                                            webUrlTargetToLoad = cleanUri
                                        }) {
                                            Icon(Icons.Default.Search, contentDescription = "Go", tint = EnergeticYellow, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                )
                            }

                            // Bookmarks Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Bookmarks:", style = Typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
                                
                                val bookmarks = listOf(
                                    Pair("Pixabay 🎨", "https://pixabay.com"),
                                    Pair("Freepik 📐", "https://www.freepik.com"),
                                    Pair("Unsplash 📷", "https://unsplash.com"),
                                    Pair("Pexels 🖼️", "https://www.pexels.com")
                                )
                                
                                bookmarks.forEach { bmk ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF1E1E24))
                                            .clickable {
                                                webUrlInput = bmk.second
                                                webUrlTargetToLoad = bmk.second
                                            }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(bmk.first, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextPrimary)
                                    }
                                }
                            }

                            if (isWebLoading) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().height(2.dp),
                                    color = EnergeticYellow,
                                    trackColor = Color.Transparent
                                )
                            } else {
                                Spacer(Modifier.height(2.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .border(1.dp, HighslateOutline, RoundedCornerShape(8.dp))
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { context ->
                                        WebView(context).apply {
                                            settings.apply {
                                                javaScriptEnabled = true
                                                domStorageEnabled = true
                                                databaseEnabled = true
                                                useWideViewPort = true
                                                loadWithOverviewMode = true
                                                setSupportZoom(true)
                                                builtInZoomControls = true
                                                displayZoomControls = false
                                                userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"
                                            }
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                                    isWebLoading = true
                                                    if (url != null) {
                                                        webUrlInput = url
                                                    }
                                                }
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    isWebLoading = false
                                                    if (url != null) {
                                                        webUrlInput = url
                                                     }
                                                }
                                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                                    return false
                                                }
                                            }
                                            webChromeClient = object : WebChromeClient() {
                                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                }
                                            }
                                            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    handleWebDownload(
                                                        context = context,
                                                        url = url,
                                                        userAgent = userAgent,
                                                        contentDisposition = contentDisposition,
                                                        mimetype = mimetype,
                                                        contentLength = contentLength,
                                                        workspaceViewModel = workspaceViewModel,
                                                        layers = layers,
                                                        onLayersChanged = onLayersChanged,
                                                        selectedLayerId = selectedLayerId,
                                                        undoStack = undoStack,
                                                        redoStack = redoStack,
                                                        onDismiss = onDismiss
                                                    )
                                                }
                                            }
                                            loadUrl(webUrlTargetToLoad)
                                            webViewInstance = this
                                        }
                                    },
                                    update = { webView ->
                                        webViewInstance = webView
                                    }
                                )
                            }
                        }
                    }
                    2 -> {
                        var userRawUrl by remember { mutableStateOf("") }
                        var linkType by remember { mutableStateOf("Image") }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Import any custom image, font typography, or PDF documents directly using direct HTTP/HTTPS web links:", style = Typography.bodyMedium, color = TextSecondary)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val optTypes = listOf("Image", "Font", "PDF")
                                optTypes.forEach { type ->
                                    val isSel = linkType == type
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { linkType = type },
                                        colors = CardDefaults.cardColors(containerColor = if (isSel) Color(0xFF2E2E38) else Color(0xFF1E1E24)),
                                        border = BorderStroke(1.2.dp, if (isSel) EnergeticYellow else Color.Transparent)
                                    ) {
                                        Box(modifier = Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                            Text(type.uppercase(), style = Typography.labelSmall, color = if (isSel) EnergeticYellow else TextPrimary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = userRawUrl,
                                onValueChange = { userRawUrl = it },
                                placeholder = { Text("Paste direct URL here (e.g. https://.../image.png)", style = Typography.bodyMedium, color = TextSecondary) },
                                textStyle = Typography.bodyMedium.copy(color = TextPrimary),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EnergeticYellow,
                                    unfocusedBorderColor = HighslateOutline
                                ),
                                singleLine = false,
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (userRawUrl.isBlank()) {
                                        Toast.makeText(context, "Please paste an active URL link first.", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val liveUrl = userRawUrl.trim()
                                    if (linkType == "Image") {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Streaming custom live image...", Toast.LENGTH_SHORT).show()
                                                }
                                                val connection = URL(liveUrl).openConnection() as HttpURLConnection
                                                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                                                connection.connectTimeout = 15000
                                                connection.readTimeout = 15000

                                                val webAssetsDir = File(context.filesDir, "web_assets")
                                                if (!webAssetsDir.exists()) webAssetsDir.mkdirs()

                                                val localFile = File(webAssetsDir, "live_img_${System.currentTimeMillis()}.jpg")
                                                connection.inputStream.use { input ->
                                                    localFile.outputStream().use { output ->
                                                        input.copyTo(output)
                                                    }
                                                }

                                                val (initW, initH) = getImageAspectRatioDimensions(localFile.absolutePath, 450f)

                                                withContext(Dispatchers.Main) {
                                                    undoStack.add(layers)
                                                    redoStack.clear()
                                                    val newL = StudioLayer(
                                                        name = "Web Fetch",
                                                        type = LayerType.IMAGE_CARD,
                                                        positionX = 150f,
                                                        positionY = 200f,
                                                        width = initW,
                                                        height = initH,
                                                        baseColor = Color.Transparent,
                                                        imageUri = localFile.absolutePath,
                                                        isAspectLocked = true
                                                    )
                                                    onLayersChanged(listOf(newL) + layers)
                                                    onSelectedLayerIdChanged(newL.id)
                                                    onDismiss()
                                                    Toast.makeText(context, "Added web image layer!", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Failed to download image: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    } else if (linkType == "Font") {
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Downloading custom font link...", Toast.LENGTH_SHORT).show()
                                                }
                                                val connection = URL(liveUrl).openConnection()
                                                val stream = connection.getInputStream()
                                                val fontsDir = File(context.filesDir, "fonts")
                                                if (!fontsDir.exists()) fontsDir.mkdirs()
                                                
                                                val fontFileName = "web_" + System.currentTimeMillis() + ".ttf"
                                                val outFile = File(fontsDir, fontFileName)
                                                outFile.outputStream().use { out -> stream.copyTo(out) }
                                                
                                                val displayName = "Web Font " + System.currentTimeMillis().toString().takeLast(4)
                                                workspaceViewModel.addCustomFont(displayName, outFile.absolutePath, "Display")
                                                
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Font registered successfully: $displayName", Toast.LENGTH_LONG).show()
                                                    
                                                    if (selectedLayerId.isNotEmpty()) {
                                                        val targetL = layers.find { it.id == selectedLayerId }
                                                        if (targetL?.type == LayerType.TEXT) {
                                                            undoStack.add(layers)
                                                            redoStack.clear()
                                                            onLayersChanged(layers.map { l ->
                                                                if (l.id == selectedLayerId) {
                                                                    l.copy(
                                                                        fontPath = outFile.absolutePath,
                                                                        fontFamilyName = displayName
                                                                    )
                                                                } else l
                                                            })
                                                        }
                                                    }
                                                    onDismiss()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Failed to fetch custom font URL.", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    } else {
                                        // PDF link conversion
                                        coroutineScope.launch(Dispatchers.IO) {
                                            try {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Downloading PDF document from URL...", Toast.LENGTH_SHORT).show()
                                                }
                                                val connection = URL(liveUrl).openConnection()
                                                val tempFile = File(context.cacheDir, "imported_${System.currentTimeMillis()}.pdf")
                                                connection.getInputStream().use { input ->
                                                    tempFile.outputStream().use { output ->
                                                        input.copyTo(output)
                                                    }
                                                }
                                                
                                                val pfd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                                                val pdfRenderer = android.graphics.pdf.PdfRenderer(pfd)
                                                val pageCount = pdfRenderer.pageCount
                                                val importedDir = File(context.filesDir, "pdf_imports")
                                                if (!importedDir.exists()) importedDir.mkdirs()
                                                
                                                val result = mutableListOf<ArtboardData>()
                                                for (i in 0 until pageCount) {
                                                    val page = pdfRenderer.openPage(i)
                                                    val savedWidth = page.width.toFloat()
                                                    val savedHeight = page.height.toFloat()
                                                    val bitmapW = (page.width * 2).coerceAtMost(2048)
                                                    val bitmapH = (page.height * 2).coerceAtMost(2048)
                                                    val bitmap = android.graphics.Bitmap.createBitmap(bitmapW, bitmapH, android.graphics.Bitmap.Config.ARGB_8888)
                                                    page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                                    
                                                    val pageFile = File(importedDir, "page_${System.currentTimeMillis()}_$i.png")
                                                    pageFile.outputStream().use { out ->
                                                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
                                                    }
                                                    page.close()
                                                    
                                                    val layerId = java.util.UUID.randomUUID().toString()
                                                    val pdfImageLayer = StudioLayer(
                                                        id = layerId,
                                                        name = "PDF Page ${i + 1}",
                                                        type = LayerType.IMAGE_CARD,
                                                        positionX = 0f,
                                                        positionY = 0f,
                                                        width = savedWidth,
                                                        height = savedHeight,
                                                        imageUri = pageFile.absolutePath
                                                    )
                                                    val artboardId = java.util.UUID.randomUUID().toString()
                                                    result.add(
                                                        ArtboardData(
                                                            id = artboardId,
                                                            name = "Web PDF Page ${i + 1}",
                                                            width = savedWidth,
                                                            height = savedHeight,
                                                            layers = listOf(pdfImageLayer),
                                                            renderedPdfBitmap = bitmap
                                                        )
                                                    )
                                                }
                                                pdfRenderer.close()
                                                
                                                withContext(Dispatchers.Main) {
                                                    if (result.isNotEmpty()) {
                                                        onImportPdfAsArtboards(result)
                                                        onDismiss()
                                                        Toast.makeText(context, "Fetched and converted ${result.size} PDF pages!", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "Failed to render PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EnergeticYellow),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("FETCH AND IMPORT WEB ASSET", color = DarkOnyx, fontWeight = FontWeight.Bold, style = Typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
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

private fun downloadAndSpawnImage(
    context: Context,
    img: Triple<String, String, String>,
    coroutineScope: CoroutineScope,
    layers: List<StudioLayer>,
    onLayersChanged: (List<StudioLayer>) -> Unit,
    onSelectedLayerIdChanged: (String) -> Unit,
    undoStack: CappedHistoryStack,
    redoStack: CappedHistoryStack,
    onDismissRequest: () -> Unit
) {
    coroutineScope.launch(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Streaming full-resolution web stock asset...", Toast.LENGTH_SHORT).show()
            }
            val userAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36"

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

            val webAssetsDir = File(context.filesDir, "web_assets")
            if (!webAssetsDir.exists()) webAssetsDir.mkdirs()

            val localFile = File(webAssetsDir, "stock_img_${System.currentTimeMillis()}.jpg")
            connection.inputStream.use { input ->
                localFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val (initW, initH) = getImageAspectRatioDimensions(localFile.absolutePath, 500f)

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
                    imageUri = localFile.absolutePath,
                    isAspectLocked = true
                )
                onLayersChanged(listOf(newL) + layers)
                onSelectedLayerIdChanged(newL.id)
                onDismissRequest()
                Toast.makeText(context, "Successfully spawned stock layer!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Streaming download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}

private val CURATED_PHOTOS = listOf(
    // Abstract
    Triple("Flowing Color Gradients", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1000", "Simeon Muller"),
    Triple("Soft Fluid Glass Visuals", "https://images.unsplash.com/photo-1618005198143-e528346d9a59?w=1000", "Simeon Muller"),
    Triple("Abstract Dreamy Fluid Wave", "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=1000", "Ilya Pavlov"),
    Triple("Warm Metallic Flow Liquid", "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=1000", "Paweł Czerwiński"),
    Triple("Digital Pink Silk Gradient", "https://images.unsplash.com/photo-1618005200387-a226b3e4f1a4?w=1000", "Simeon Muller"),
    Triple("Aqueous Pastel Swirls", "https://images.unsplash.com/photo-1541701494587-cb58502866ab?w=1000", "Joel Filipe"),

    // Neon
    Triple("Neon Cyber Grid Tokyo", "https://images.unsplash.com/photo-1540959733332-eab4deceeaf7?w=1000", "Jezael Melgoza"),
    Triple("Cyberpunk Alleyway Retro", "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?w=1000", "Sven Brandsma"),
    Triple("Neon Laser Wave Waveform", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1000", "Kamil S"),
    Triple("Synthwave Cyber Sunset Drive", "https://images.unsplash.com/photo-1515621061946-eff1c2a352bd?w=1000", "Goh Rhy Yan"),
    Triple("Retro Hologram Light Frame", "https://images.unsplash.com/photo-1563089145-599997674d42?w=1000", "Alex Perez"),
    Triple("Vibrant Purple Laser Beam", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=1000", "Maximilian Weisbecker"),

    // Nature
    Triple("Misty Emerald Pines", "https://images.unsplash.com/photo-1441974231531-c6227db76b6e?w=1000", "Jay Mantri"),
    Triple("Golden Sunset Ocean Horizon", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=1000", "Sean Oulashin"),
    Triple("Volcanic Black Sand Beach", "https://images.unsplash.com/photo-1505118380757-91f5f5632de0?w=1000", "Ishak Kacel"),
    Triple("Ethereal Autumn Forest Path", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=1000", "John Fowler"),
    Triple("Foggy Alpine Mountain Ridge", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=1000", "Kal Vis"),
    Triple("Iceland Majestic Glacier River", "https://images.unsplash.com/photo-1470071459604-3b5ec3a7fe05?w=1000", "Lukasz Szmigiel"),

    // Wallpaper
    Triple("Minimalist Pastel Dunes", "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=1000", "Wolfgang Hasselmann"),
    Triple("Geometric Architectural Line", "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=1000", "Jannis Lucas"),
    Triple("Clean Nordic Work Desk", "https://images.unsplash.com/photo-1499951360447-b19be8fe80f5?w=1000", "Domenico Loia"),
    Triple("Serene Soft Ocean Waves", "https://images.unsplash.com/photo-1519046904884-53103b34b206?w=1000", "Sean Oulashin"),
    Triple("Warm Terracotta Arches", "https://images.unsplash.com/photo-1615529182904-14819c35db37?w=1000", "Inge Maria"),
    Triple("Soft Warm Pastel Sunrise", "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?w=1000", "Ales Krivec"),

    // Texture
    Triple("Brushed Gold Stone Plate", "https://images.unsplash.com/photo-1533090161767-e6ffed986c88?w=1000", "Fannie de Villiers"),
    Triple("Cracked Desert Clay Ground", "https://images.unsplash.com/photo-1509316975850-ff9c5deb0cd9?w=1000", "Wolfgang Hasselmann"),
    Triple("Dark Crumpled Carbon Paper", "https://images.unsplash.com/photo-1517842645767-c639042777db?w=1000", "Melanie Werner"),
    Triple("Raw Industrial Concrete Wall", "https://images.unsplash.com/photo-1531685250784-7569952593d2?w=1000", "Katarzyna Pe"),
    Triple("Iridescent Holographic Foil", "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=1000", "Paweł Czerwiński"),
    Triple("Organic Woven Linen Fiber", "https://images.unsplash.com/photo-1544816155-12df9643f363?w=1000", "Kari Shea"),

    // Space
    Triple("Glow Milky Way Night Sky", "https://images.unsplash.com/photo-1506318137071-a8e063b4bec0?w=1000", "Manuel Cosentino"),
    Triple("Abstract Cosmic Blue Nebula", "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=1000", "Joel Filipe"),
    Triple("Deep Red Space Cosmos Star", "https://images.unsplash.com/photo-1462331940025-496dfbfc7564?w=1000", "NASA"),
    Triple("Solar Eclipse Ring Light", "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?w=1000", "Ales Krivec"),
    Triple("Luminescent Star Constellation", "https://images.unsplash.com/photo-1444703686981-a3abbc4d4fe3?w=1000", "NASA"),
    Triple("Mars Red Soil Surface", "https://images.unsplash.com/photo-1612892483236-42d68a57623d?w=1000", "Planet Volumes")
)

private fun searchCuratedPhotos(query: String): List<Triple<String, String, String>> {
    val lowerQuery = query.lowercase().trim()
    if (lowerQuery.isEmpty() || lowerQuery == "aesthetic minimal gradient") {
        // Return a premium handpicked selection
        return CURATED_PHOTOS.take(12)
    }
    
    val matches = CURATED_PHOTOS.filter { photo ->
        val title = photo.first.lowercase()
        val author = photo.third.lowercase()
        title.contains(lowerQuery) || author.contains(lowerQuery)
    }
    
    if (matches.isNotEmpty()) return matches
    
    // Check specific categories
    return when {
        lowerQuery.contains("abstract") -> CURATED_PHOTOS.filterIndexed { index, _ -> index in 0..5 }
        lowerQuery.contains("neon") -> CURATED_PHOTOS.filterIndexed { index, _ -> index in 6..11 }
        lowerQuery.contains("nature") -> CURATED_PHOTOS.filterIndexed { index, _ -> index in 12..17 }
        lowerQuery.contains("wallpaper") -> CURATED_PHOTOS.filterIndexed { index, _ -> index in 18..23 }
        lowerQuery.contains("texture") -> CURATED_PHOTOS.filterIndexed { index, _ -> index in 24..29 }
        lowerQuery.contains("space") -> CURATED_PHOTOS.filterIndexed { index, _ -> index in 30..35 }
        else -> emptyList()
    }
}

private fun searchWikimediaCommons(query: String): List<Triple<String, String, String>> {
    val photos = mutableListOf<Triple<String, String, String>>()
    try {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val urlString = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch=filetype:bitmap%20$encodedQuery&gsrlimit=18&prop=imageinfo&iiprop=url&format=json&gsrnamespace=6"
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) ZenithStudio/1.0")
        connection.connectTimeout = 8000
        connection.readTimeout = 8000
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
                    val filename = pageItem.optString("title", "Commons Stock").replace("File:", "")
                    val cleanTitle = filename.substringBeforeLast(".")
                        .replace("_", " ")
                        .replace("-", " ")
                        .trim()
                    val imgInfoArr = pageItem.optJSONArray("imageinfo")
                    if (imgInfoArr != null && imgInfoArr.length() > 0) {
                        val info = imgInfoArr.getJSONObject(0)
                        val imgUrl = info.optString("url", "")
                        if (imgUrl.isNotBlank()) {
                            photos.add(Triple(cleanTitle, imgUrl, "Wikimedia Commons"))
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return photos
}

private suspend fun handleWebDownload(
    context: Context,
    url: String,
    userAgent: String,
    contentDisposition: String,
    mimetype: String,
    contentLength: Long,
    workspaceViewModel: WorkspaceViewModel,
    layers: List<StudioLayer>,
    onLayersChanged: (List<StudioLayer>) -> Unit,
    selectedLayerId: String,
    undoStack: CappedHistoryStack,
    redoStack: CappedHistoryStack,
    onDismiss: () -> Unit
) {
    try {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Downloading custom asset from web...", Toast.LENGTH_SHORT).show()
        }

        // Fetch session cookies to preserve authentication state if needed
        val cookie = android.webkit.CookieManager.getInstance().getCookie(url)

        var connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("User-Agent", userAgent)
        if (cookie != null) {
            connection.setRequestProperty("Cookie", cookie)
        }
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.instanceFollowRedirects = true

        var status = connection.responseCode
        var redirectCount = 0
        var finalUrl = url
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
            if (cookie != null) {
                nextConn.setRequestProperty("Cookie", cookie)
            }
            nextConn.connectTimeout = 15000
            nextConn.readTimeout = 15000
            nextConn.instanceFollowRedirects = true
            connection = nextConn
            status = connection.responseCode
            redirectCount++
        }

        var fileName = android.webkit.URLUtil.guessFileName(finalUrl, contentDisposition, mimetype)
        if (fileName.isNullOrBlank()) {
            fileName = "web_asset_${System.currentTimeMillis()}"
        }

        val tempDir = File(context.cacheDir, "web_downloads")
        if (!tempDir.exists()) tempDir.mkdirs()
        val tempFile = File(tempDir, fileName)

        connection.getInputStream().use { inp ->
            tempFile.outputStream().use { out ->
                inp.copyTo(out)
            }
        }

        val lowerName = fileName.lowercase()
        if (lowerName.endsWith(".zip")) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Extracting downloaded zip font package...", Toast.LENGTH_SHORT).show()
            }
            val fontFamilyList = unzipAndRegisterFonts(context, tempFile, workspaceViewModel)
            withContext(Dispatchers.Main) {
                if (fontFamilyList.isNotEmpty()) {
                    Toast.makeText(context, "Successfully extracted and loaded: ${fontFamilyList.joinToString()}", Toast.LENGTH_LONG).show()
                    
                    // Auto-apply first extracted font in zip to the active layer if it is selected and is text
                    if (selectedLayerId.isNotEmpty()) {
                        val activeL = layers.find { it.id == selectedLayerId }
                        if (activeL?.type == LayerType.TEXT) {
                            val firstFont = fontFamilyList.first()
                            val ttfName = File(context.filesDir, "fonts").listFiles { file ->
                                file.isFile && file.nameWithoutExtension.replace("_", " ").replace("-", " ").equals(firstFont, ignoreCase = true)
                            }?.firstOrNull()?.absolutePath ?: File(File(context.filesDir, "fonts"), firstFont).absolutePath
                            
                            onLayersChanged(layers.map { l ->
                                if (l.id == selectedLayerId) {
                                    l.copy(
                                        fontPath = ttfName,
                                        fontFamilyName = firstFont
                                    )
                                } else l
                            })
                        }
                    }
                } else {
                    Toast.makeText(context, "No custom TrueType (.ttf) or OpenType (.otf) files found inside the zip.", Toast.LENGTH_LONG).show()
                }
            }
        } else if (lowerName.endsWith(".ttf") || lowerName.endsWith(".otf")) {
            val fontsFolder = File(context.filesDir, "fonts")
            if (!fontsFolder.exists()) fontsFolder.mkdirs()
            val destFile = File(fontsFolder, fileName)
            tempFile.copyTo(destFile, overwrite = true)

            val displayName = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
            workspaceViewModel.addCustomFont(displayName, destFile.absolutePath, "Display")

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Registered typography font: $displayName", Toast.LENGTH_LONG).show()
                if (selectedLayerId.isNotEmpty()) {
                    val activeL = layers.find { it.id == selectedLayerId }
                    if (activeL?.type == LayerType.TEXT) {
                        onLayersChanged(layers.map { l ->
                            if (l.id == selectedLayerId) {
                                l.copy(
                                    fontPath = destFile.absolutePath,
                                    fontFamilyName = displayName
                                )
                            } else l
                        })
                    }
                }
            }
        } else if (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp") || mimetype.startsWith("image/")) {
            val webAssetsDir = File(context.filesDir, "web_assets")
            if (!webAssetsDir.exists()) webAssetsDir.mkdirs()
            val destFile = File(webAssetsDir, fileName)
            tempFile.copyTo(destFile, overwrite = true)

            val (initW, initH) = getImageAspectRatioDimensions(destFile.absolutePath, 500f)

            withContext(Dispatchers.Main) {
                undoStack.add(layers)
                redoStack.clear()
                val newL = StudioLayer(
                    name = fileName.substringBeforeLast("."),
                    type = LayerType.IMAGE_CARD,
                    positionX = 150f,
                    positionY = 200f,
                    width = initW,
                    height = initH,
                    baseColor = Color.Transparent,
                    imageUri = destFile.absolutePath,
                    isAspectLocked = true
                )
                onLayersChanged(listOf(newL) + layers)
                onDismiss()
                Toast.makeText(context, "Spawned custom loaded image layer!", Toast.LENGTH_SHORT).show()
            }
        } else {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Downloaded unsupported resource: $fileName (${contentLength / 1024} KB)", Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed downloading web item: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

private fun unzipAndRegisterFonts(
    context: Context,
    zipFile: File,
    workspaceViewModel: WorkspaceViewModel
): List<String> {
    val fontsDir = File(context.filesDir, "fonts")
    if (!fontsDir.exists()) fontsDir.mkdirs()

    val registeredFonts = mutableListOf<String>()

    try {
        java.io.FileInputStream(zipFile).use { fis ->
            java.util.zip.ZipInputStream(fis).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val simpleName = File(entry.name).name
                        val lowerName = simpleName.lowercase()
                        if (lowerName.endsWith(".ttf") || lowerName.endsWith(".otf")) {
                            val targetFile = File(fontsDir, simpleName)
                            targetFile.outputStream().use { fos ->
                                zis.copyTo(fos)
                            }
                            val displayName = simpleName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
                            workspaceViewModel.addCustomFont(displayName, targetFile.absolutePath, "Display")
                            registeredFonts.add(displayName)
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return registeredFonts
}

