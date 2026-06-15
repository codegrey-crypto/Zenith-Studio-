package com.example.studio.ui

import android.content.Context
import android.widget.Toast
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry

@Composable
fun OnlineFontEngineDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    coroutineScope: CoroutineScope,
    workspaceViewModel: WorkspaceViewModel,
    layers: List<StudioLayer>,
    onLayersChanged: (List<StudioLayer>) -> Unit,
    selectedLayerId: String,
    
    // Scanned device state hooks so scanning remains robustly functional
    discoveredFonts: List<com.example.studio.ui.FontScanner.DiscoveredFont>,
    isFontScanning: Boolean,
    fontScanStatusMessage: String,
    isBatchImportingFonts: Boolean,
    selectedFontIndexes: MutableMap<Int, Boolean>,
    selectedFoldersToScan: MutableMap<String, Boolean>,
    requestOrPromptStoragePermission: () -> Unit,
    onFontPickerLaunch: () -> Unit,
    onStartBatchImport: (List<com.example.studio.ui.FontScanner.DiscoveredFont>) -> Unit
) {
    if (!showDialog) return

    val context = LocalContext.current
    var fontEngineTab by remember { mutableStateOf(0) } // 0: Online Google Fonts Catalog, 1: Device storage scanner

    val webFontsCatalogue = remember {
        listOf(
            Triple("Montserrat", "https://github.com/google/fonts/raw/main/ofl/montserrat/static/Montserrat-Medium.ttf", "Sans-Serif"),
            Triple("Playfair Display", "https://github.com/google/fonts/raw/main/ofl/playfairdisplay/static/PlayfairDisplay-Medium.ttf", "Serif"),
            Triple("Pacifico", "https://github.com/google/fonts/raw/main/ofl/pacifico/Pacifico-Regular.ttf", "Script"),
            Triple("Oswald", "https://github.com/google/fonts/raw/main/ofl/oswald/static/Oswald-Medium.ttf", "Sans-Serif Condensed"),
            Triple("Cinzel", "https://github.com/google/fonts/raw/main/ofl/cinzel/static/Cinzel-Medium.ttf", "Serif Classical"),
            Triple("Inconsolata", "https://github.com/google/fonts/raw/main/ofl/inconsolata/static/Inconsolata-Regular.ttf", "Monospace"),
            Triple("Bebas Neue", "https://github.com/google/fonts/raw/main/ofl/bebasneue/BebasNeue-Regular.ttf", "Display Bold"),
            Triple("Caveat", "https://github.com/google/fonts/raw/main/ofl/caveat/static/Caveat-Medium.ttf", "Handwriting"),
            Triple("Lobster", "https://github.com/google/fonts/raw/main/ofl/lobster/Lobster-Regular.ttf", "Script Bold"),
            Triple("Roboto", "https://github.com/google/fonts/raw/main/ofl/roboto/static/Roboto-Medium.ttf", "Sans-Serif"),
            Triple("Lato", "https://github.com/google/fonts/raw/main/ofl/lato/Lato-Regular.ttf", "Sans-Serif"),
            Triple("Inter", "https://github.com/google/fonts/raw/main/ofl/inter/static/Inter-Medium.ttf", "Sans-Serif UI"),
            Triple("Poppins", "https://github.com/google/fonts/raw/main/ofl/poppins/Poppins-Medium.ttf", "Sans-Serif Geometric"),
            Triple("Nunito", "https://github.com/google/fonts/raw/main/ofl/nunito/static/Nunito-Medium.ttf", "Sans-Serif Rounded"),
            Triple("Lora", "https://github.com/google/fonts/raw/main/ofl/lora/static/Lora-Medium.ttf", "Serif Literary"),
            Triple("Rubik", "https://github.com/google/fonts/raw/main/ofl/rubik/static/Rubik-Medium.ttf", "Sans-Serif Friendly"),
            Triple("Josefin Sans", "https://github.com/google/fonts/raw/main/ofl/josefinsans/static/JosefinSans-Medium.ttf", "Sans-Serif Elegant"),
            Triple("Quicksand", "https://github.com/google/fonts/raw/main/ofl/quicksand/static/Quicksand-Medium.ttf", "Sans-Serif Soft"),
            Triple("Dancing Script", "https://github.com/google/fonts/raw/main/ofl/dancingscript/static/DancingScript-Medium.ttf", "Handwriting Cursive"),
            Triple("Great Vibes", "https://github.com/google/fonts/raw/main/ofl/greatvibes/GreatVibes-Regular.ttf", "Script Calligraphy"),
            Triple("Amatic SC", "https://github.com/google/fonts/raw/main/ofl/amaticsc/AmaticSC-Regular.ttf", "Handwriting Tall"),
            Triple("Shadows Into Light", "https://github.com/google/fonts/raw/main/ofl/shadowsintolight/ShadowsIntoLight.ttf", "Handwriting Minimal"),
            Triple("Sacramento", "https://github.com/google/fonts/raw/main/ofl/sacramento/Sacramento-Regular.ttf", "Script Flowing"),
            Triple("Orbitron", "https://github.com/google/fonts/raw/main/ofl/orbitron/static/Orbitron-Medium.ttf", "Display Techno"),
            Triple("Comfortaa", "https://github.com/google/fonts/raw/main/ofl/comfortaa/static/Comfortaa-Medium.ttf", "Display Rounded"),
            Triple("Teko", "https://github.com/google/fonts/raw/main/ofl/teko/static/Teko-Medium.ttf", "Display Heavy"),
            Triple("Righteous", "https://github.com/google/fonts/raw/main/ofl/righteous/Righteous-Regular.ttf", "Display ArtDeco"),
            Triple("Special Elite", "https://github.com/google/fonts/raw/main/ofl/specialelite/SpecialElite-Regular.ttf", "Display Typewriter"),
            Triple("Audiowide", "https://github.com/google/fonts/raw/main/ofl/audiowide/Audiowide-Regular.ttf", "Display Futurist"),
            Triple("Press Start 2P", "https://github.com/google/fonts/raw/main/ofl/pressstart2p/PressStart2P-Regular.ttf", "Display Retro 8Bit"),
            Triple("Merriweather", "https://github.com/google/fonts/raw/main/ofl/merriweather/static/Merriweather-Medium.ttf", "Serif Journal"),
            Triple("PT Serif", "https://github.com/google/fonts/raw/main/ofl/ptserif/PTSerif-Regular.ttf", "Serif Content"),
            Triple("Raleway", "https://github.com/google/fonts/raw/main/ofl/raleway/static/Raleway-Medium.ttf", "Sans-Serif Sophisticated"),
            Triple("Alfa Slab One", "https://github.com/google/fonts/raw/main/ofl/alfaslabone/AlfaSlabOne-Regular.ttf", "Display Slab Heavy"),
            Triple("Creepster", "https://github.com/google/fonts/raw/main/ofl/creepster/Creepster-Regular.ttf", "Display Gothic spooky"),
            Triple("Permanent Marker", "https://github.com/google/fonts/raw/main/ofl/permanentmarker/PermanentMarker-Regular.ttf", "Handwriting Brush")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = null,
                        tint = IndustrialAmber,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Typography Asset System",
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp),
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF131317))
                        .padding(2.dp)
                ) {
                    listOf("Catalog Fonts", "Web Font Browser", "Local Storage Scan").forEachIndexed { index, label ->
                        val isSel = fontEngineTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSel) Color(0xFF2E2E38) else Color.Transparent)
                                .clickable { fontEngineTab = index }
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = Typography.labelSmall.copy(fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSel) IndustrialAmber else TextSecondary
                            )
                        }
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 410.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (fontEngineTab == 0) {
                    // Google Fonts catalog tab
                    var fontSearchVal by remember { mutableStateOf("") }
                    val filteredCatalogue = remember(fontSearchVal) {
                        if (fontSearchVal.isBlank()) {
                            webFontsCatalogue
                        } else {
                            webFontsCatalogue.filter { 
                                it.first.contains(fontSearchVal, ignoreCase = true) ||
                                it.third.contains(fontSearchVal, ignoreCase = true)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = fontSearchVal,
                        onValueChange = { fontSearchVal = it },
                        placeholder = { Text("Search 35+ premium Google Fonts...", style = Typography.bodyMedium, color = TextSecondary) },
                        textStyle = Typography.bodyMedium.copy(color = TextPrimary),
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null, tint = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = IndustrialAmber,
                            unfocusedBorderColor = HighslateOutline
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )

                    Text(
                        text = "OFL Open Catalogue (Tap download to dynamically register & style active layers):",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondary
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(0.5.dp, HighslateOutline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .background(SlatePanel.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(4.dp)
                    ) {
                        items(filteredCatalogue) { fnt ->
                            val cleanName = fnt.first.replace(" ", "")
                            val fontsFolder = File(context.filesDir, "fonts")
                            if (!fontsFolder.exists()) fontsFolder.mkdirs()
                            val localFontFile = File(fontsFolder, "$cleanName.ttf")
                            val alreadyExists = localFontFile.exists()

                            // Dynamically build native font family from ttf binary
                            val previewFontFamily = if (alreadyExists) {
                                try {
                                    androidx.compose.ui.text.font.FontFamily(android.graphics.Typeface.createFromFile(localFontFile))
                                } catch (e: Exception) {
                                    androidx.compose.ui.text.font.FontFamily.Default
                                }
                            } else {
                                androidx.compose.ui.text.font.FontFamily.Default
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
                                border = BorderStroke(1.dp, HighslateOutline.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(fnt.first, style = Typography.bodyMedium.copy(fontSize = 14.sp), color = TextPrimary, fontWeight = FontWeight.Bold)
                                            Text("Category: Google Fonts (${fnt.third})", style = Typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary)
                                        }

                                        Button(
                                            onClick = {
                                                coroutineScope.launch(Dispatchers.IO) {
                                                    try {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Streaming '${fnt.first}' down from OFL catalog...", Toast.LENGTH_SHORT).show()
                                                        }
                                                        val connection = URL(fnt.second).openConnection()
                                                        connection.connectTimeout = 12000
                                                        connection.readTimeout = 12000
                                                        val stream = connection.getInputStream()
                                                        
                                                        val destTtf = File(fontsFolder, "$cleanName.ttf")
                                                        destTtf.outputStream().use { out -> stream.copyTo(out) }

                                                        val friendlyName = fnt.first
                                                        workspaceViewModel.addCustomFont(friendlyName, destTtf.absolutePath, fnt.third)

                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Render font registered: ${fnt.first}!", Toast.LENGTH_LONG).show()
                                                            
                                                            // Auto-apply to selected TEXT layer
                                                            if (selectedLayerId.isNotEmpty()) {
                                                                val activeL = layers.find { it.id == selectedLayerId }
                                                                if (activeL?.type == LayerType.TEXT) {
                                                                    onLayersChanged(layers.map { l ->
                                                                        if (l.id == selectedLayerId) {
                                                                            l.copy(
                                                                                fontPath = destTtf.absolutePath,
                                                                                fontFamilyName = friendlyName
                                                                            )
                                                                        } else l
                                                                    })
                                                                }
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        e.printStackTrace()
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(context, "Download catalog mismatch: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                        }
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (alreadyExists) Color(0xFF4CAF50) else Color(0xFF00FF66)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(if (alreadyExists) Icons.Default.CheckCircle else Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(10.dp), tint = DarkOnyx)
                                                Text(if (alreadyExists) "INSTALLED" else "DOWNLOAD", style = Typography.labelSmall.copy(fontSize = 9.sp), color = DarkOnyx, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    // Display Custom typography preview line natively styled with custom font if installed!
                                    Text(
                                        text = "Elegant typography rendering: Aa Bb Cc 123",
                                        style = Typography.bodyMedium.copy(color = IndustrialAmber, fontSize = 11.sp),
                                        fontFamily = previewFontFamily,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFF131317))
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                } else if (fontEngineTab == 1) {
                    // Web Font Browser tab!
                    // Let's implement an elegant in-app font browser with downloads listening & auto-extraction!
                    val fontBrowserContext = LocalContext.current
                    var webUrlInput by remember { mutableStateOf("https://www.dafont.com") }
                    var webUrlTargetToLoad by remember { mutableStateOf("https://www.dafont.com") }
                    var isWebLoading by remember { mutableStateOf(false) }
                    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

                    LaunchedEffect(webUrlTargetToLoad) {
                        webViewInstance?.let { view ->
                            if (view.url != webUrlTargetToLoad) {
                                view.loadUrl(webUrlTargetToLoad)
                            }
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth().height(410.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = { webViewInstance?.goBack() },
                                modifier = Modifier.size(32.dp).background(MidSlate, RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.ArrowBack, "Back", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { webViewInstance?.goForward() },
                                modifier = Modifier.size(32.dp).background(MidSlate, RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.ArrowForward, "Forward", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { webUrlTargetToLoad = "https://www.dafont.com"; webUrlInput = "https://www.dafont.com" },
                                modifier = Modifier.size(32.dp).background(MidSlate, RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.Home, "Home", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { webViewInstance?.reload() },
                                modifier = Modifier.size(32.dp).background(MidSlate, RoundedCornerShape(4.dp))
                            ) {
                                Icon(Icons.Default.Refresh, "Reload", tint = TextPrimary, modifier = Modifier.size(16.dp))
                            }

                            OutlinedTextField(
                                value = webUrlInput,
                                onValueChange = { webUrlInput = it },
                                modifier = Modifier.weight(1f).height(36.dp),
                                textStyle = Typography.labelSmall.copy(color = TextPrimary, fontSize = 11.sp),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Go
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onGo = { webUrlTargetToLoad = if (webUrlInput.startsWith("http://") || webUrlInput.startsWith("https://")) webUrlInput else "https://$webUrlInput" }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = IndustrialAmber,
                                    unfocusedBorderColor = HighslateOutline
                                )
                            )
                        }

                        // Bookmark Row
                        val fontBookmarks = listOf(
                            Pair("DaFont 🔠", "https://www.dafont.com"),
                            Pair("FontSpace 🔤", "https://www.fontspace.com"),
                            Pair("1001 Fonts 🔡", "https://www.1001freefonts.com"),
                            Pair("Google Fonts 🌐", "https://fonts.google.com")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            fontBookmarks.forEach { (label, targetUrl) ->
                                Text(
                                    text = label,
                                    style = Typography.labelSmall.copy(fontSize = 10.sp),
                                    color = IndustrialAmber,
                                    modifier = Modifier
                                        .background(MidSlate.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .clickable {
                                            webUrlTargetToLoad = targetUrl
                                            webUrlInput = targetUrl
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (isWebLoading) {
                            LinearProgressIndicator(color = IndustrialAmber, modifier = Modifier.fillMaxWidth().height(2.dp))
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(0.5.dp, HighslateOutline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        settings.apply {
                                            javaScriptEnabled = true
                                            domStorageEnabled = true
                                            userAgentString = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Mobile Safari/537.36"
                                        }
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                                super.onPageStarted(view, url, favicon)
                                                isWebLoading = true
                                                url?.let { webUrlInput = it }
                                            }
                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                super.onPageFinished(view, url)
                                                isWebLoading = false
                                                url?.let { webUrlInput = it }
                                            }
                                        }
                                        webChromeClient = object : WebChromeClient() {
                                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                                isWebLoading = (newProgress < 100)
                                            }
                                        }
                                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                            coroutineScope.launch(Dispatchers.IO) {
                                                try {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(fontBrowserContext, "Downloading custom font from web...", Toast.LENGTH_SHORT).show()
                                                    }
                                                    val cookie = CookieManager.getInstance().getCookie(url)
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

                                                    var fileName = URLUtil.guessFileName(finalUrl, contentDisposition, mimetype)
                                                    if (fileName.isNullOrBlank()) {
                                                        fileName = "font_${System.currentTimeMillis()}.zip"
                                                    }

                                                    val tempDir = File(fontBrowserContext.cacheDir, "font_downloads")
                                                    if (!tempDir.exists()) tempDir.mkdirs()
                                                    val tempFile = File(tempDir, fileName)

                                                    connection.getInputStream().use { inp ->
                                                        FileOutputStream(tempFile).use { out ->
                                                            inp.copyTo(out)
                                                        }
                                                    }

                                                    val lowerName = fileName.lowercase()
                                                    val fontsFolder = File(fontBrowserContext.filesDir, "fonts")
                                                    if (!fontsFolder.exists()) fontsFolder.mkdirs()

                                                    if (lowerName.endsWith(".zip")) {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(fontBrowserContext, "Extracting downloaded zip font package...", Toast.LENGTH_SHORT).show()
                                                        }
                                                        val registered = mutableListOf<String>()
                                                        FileInputStream(tempFile).use { fis ->
                                                            ZipInputStream(fis).use { zis ->
                                                                var entry: ZipEntry? = zis.getNextEntry()
                                                                while (entry != null) {
                                                                    if (!entry.isDirectory()) {
                                                                        val simpleName = File(entry.getName()).name
                                                                        val lowerEntry = simpleName.lowercase()
                                                                        if (lowerEntry.endsWith(".ttf") || lowerEntry.endsWith(".otf")) {
                                                                            val destFile = File(fontsFolder, simpleName)
                                                                            FileOutputStream(destFile).use { fos ->
                                                                                zis.copyTo(fos)
                                                                            }
                                                                            val displayName = simpleName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
                                                                            workspaceViewModel.addCustomFont(displayName, destFile.absolutePath, "Display")
                                                                            registered.add(displayName)
                                                                        }
                                                                    }
                                                                    zis.closeEntry()
                                                                    entry = zis.getNextEntry()
                                                                }
                                                            }
                                                        }

                                                        withContext(Dispatchers.Main) {
                                                            if (registered.isNotEmpty()) {
                                                                Toast.makeText(fontBrowserContext, "Loaded and compiled fonts: ${registered.joinToString()}", Toast.LENGTH_LONG).show()
                                                                
                                                                // Apply first extracted font to text layer if active
                                                                if (selectedLayerId.isNotEmpty()) {
                                                                    val activeL = layers.find { it.id == selectedLayerId }
                                                                    if (activeL?.type == LayerType.TEXT) {
                                                                        val firstF = registered.first()
                                                                        val matchingTtf = fontsFolder.listFiles { f ->
                                                                            f.isFile && f.nameWithoutExtension.replace("_", " ").replace("-", " ").equals(firstF, ignoreCase = true)
                                                                        }?.firstOrNull() ?: File(fontsFolder, firstF)
                                                                        onLayersChanged(layers.map { l ->
                                                                            if (l.id == selectedLayerId) {
                                                                                l.copy(
                                                                                    fontPath = matchingTtf.absolutePath,
                                                                                    fontFamilyName = firstF
                                                                                )
                                                                            } else l
                                                                        })
                                                                    }
                                                                }
                                                            } else {
                                                                Toast.makeText(fontBrowserContext, "No TrueType (.ttf) or OpenType (.otf) files found in the zip.", Toast.LENGTH_LONG).show()
                                                            }
                                                        }
                                                    } else if (lowerName.endsWith(".ttf") || lowerName.endsWith(".otf")) {
                                                        val destFile = File(fontsFolder, fileName)
                                                        tempFile.copyTo(destFile, overwrite = true)
                                                        val displayName = fileName.substringBeforeLast(".").replace("_", " ").replace("-", " ")
                                                        workspaceViewModel.addCustomFont(displayName, destFile.absolutePath, "Display")

                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(fontBrowserContext, "Registered typography font: $displayName", Toast.LENGTH_SHORT).show()
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
                                                    } else {
                                                        withContext(Dispatchers.Main) {
                                                            Toast.makeText(fontBrowserContext, "Unsupported web asset format: $fileName", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(fontBrowserContext, "Failed downloading font: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                    }
                                                }
                                            }
                                        }
                                    }
                                },
                                update = { webViewInstance = it },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    // Local scanned device storage tab
                    LaunchedEffect(Unit) {
                        requestOrPromptStoragePermission()
                    }

                    Text(
                        text = "Zenith Studio will dynamically scan device storage volumes (Download, Documents) for custom TTF and OTF design files.",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )

                    // Folder selection
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Specific Folders to scan:",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = IndustrialAmber
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Download", "Fonts", "Documents").forEach { folder ->
                                val isSelected = selectedFoldersToScan[folder] ?: false
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MidSlate.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                        .clickable { selectedFoldersToScan[folder] = !isSelected }
                                        .padding(end = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { selectedFoldersToScan[folder] = it },
                                        colors = CheckboxDefaults.colors(checkedColor = IndustrialAmber),
                                        modifier = Modifier.scale(0.75f)
                                    )
                                    Text(
                                        text = folder,
                                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Progress
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MidSlate.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .border(1.dp, HighslateOutline.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFontScanning) "Scanning Storage..." else if (isBatchImportingFonts) "Batch Importing..." else "Scan Engine",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = IndustrialAmber
                                )
                                if (isFontScanning || isBatchImportingFonts) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = IndustrialAmber, strokeWidth = 1.6.dp)
                                }
                            }
                            Text(fontScanStatusMessage, style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextPrimary)
                        }
                    }

                    // Scan Tools row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = requestOrPromptStoragePermission,
                            colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp),
                            enabled = !isFontScanning && !isBatchImportingFonts
                        ) {
                            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(12.dp), tint = TextPrimary)
                            Spacer(Modifier.width(4.dp))
                            Text("Rescan", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextPrimary)
                        }

                        Button(
                            onClick = onFontPickerLaunch,
                            colors = ButtonDefaults.buttonColors(containerColor = MidSlate),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp),
                            enabled = !isFontScanning && !isBatchImportingFonts
                        ) {
                            Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(12.dp), tint = TextPrimary)
                            Spacer(Modifier.width(4.dp))
                            Text("Manual Pick", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextPrimary)
                        }

                        if (discoveredFonts.isNotEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                            val currentSelectedCount = selectedFontIndexes.filter { it.value }.size
                            val allSelected = currentSelectedCount == discoveredFonts.size
                            TextButton(
                                onClick = {
                                    val nextSel = !allSelected
                                    if (nextSel) {
                                        discoveredFonts.indices.forEach { selectedFontIndexes[it] = true }
                                    } else {
                                        selectedFontIndexes.clear()
                                    }
                                },
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text(if (allSelected) "Deselect All" else "Select All", style = Typography.labelSmall, color = IndustrialAmber, fontSize = 10.sp)
                            }
                        }
                    }

                    // Scanned files list
                    if (discoveredFonts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(0.5.dp, HighslateOutline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isFontScanning) "Searching Directories..." else "No ttf/otf files found inside selected directories. Move .ttf/.otf files to external Download folder and click Rescan!",
                                style = Typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(0.5.dp, HighslateOutline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .background(SlatePanel.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            items(discoveredFonts.size) { index ->
                                val fontItem = discoveredFonts[index]
                                val isChecked = selectedFontIndexes[index] ?: false
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedFontIndexes[index] = !isChecked
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { selectedFontIndexes[index] = it },
                                        colors = CheckboxDefaults.colors(checkedColor = IndustrialAmber)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(fontItem.name, style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = TextPrimary)
                                        Text(fontItem.file.parent ?: "/storage", style = Typography.labelSmall.copy(fontSize = 9.sp), color = TextSecondary, maxLines = 1)
                                    }
                                    Text("." + fontItem.file.extension.uppercase(), style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = IndustrialAmber)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (fontEngineTab == 2 && discoveredFonts.isNotEmpty()) {
                    val count = discoveredFonts.filterIndexed { idx, _ -> selectedFontIndexes[idx] == true }.size
                    Button(
                        onClick = {
                            val selectedToImport = discoveredFonts.filterIndexed { idx, _ -> selectedFontIndexes[idx] == true }
                            onStartBatchImport(selectedToImport)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IndustrialAmber),
                        shape = RoundedCornerShape(8.dp),
                        enabled = count > 0 && !isFontScanning && !isBatchImportingFonts
                    ) {
                        Text("Batch Import Scan ($count)", color = DarkOnyx, style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.width(8.dp))
                }

                TextButton(onClick = onDismiss, enabled = !isBatchImportingFonts) {
                    Text("Close", color = TextSecondary, style = Typography.labelMedium)
                }
            }
        },
        containerColor = SlatePanel
    )
}
