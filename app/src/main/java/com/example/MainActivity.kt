package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.studio.ui.WorkspaceScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    // Intercept uncaught exceptions to show a beautiful Recovery Screen
    val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
      try {
        val intent = android.content.Intent(applicationContext, CrashActivity::class.java).apply {
          putExtra("error_message", throwable.localizedMessage ?: throwable.toString())
          putExtra("stack_trace", android.util.Log.getStackTraceString(throwable))
          addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        applicationContext.startActivity(intent)
      } catch (e: Exception) {
        defaultHandler?.uncaughtException(thread, throwable)
      }
      android.os.Process.killProcess(android.os.Process.myPid())
      java.lang.System.exit(10)
    }

    // Pre-create WebView Code Cache directories to prevent benign opendir E/chromium warnings/errors on some platforms
    try {
        val webViewDir = java.io.File(cacheDir, "WebView")
        webViewDir.mkdirs()
        webViewDir.setReadable(true, false)
        webViewDir.setWritable(true, false)
        webViewDir.setExecutable(true, false)

        val defaultDir = java.io.File(webViewDir, "Default")
        defaultDir.mkdirs()
        defaultDir.setReadable(true, false)
        defaultDir.setWritable(true, false)
        defaultDir.setExecutable(true, false)

        val httpCacheDir = java.io.File(defaultDir, "HTTP Cache")
        httpCacheDir.mkdirs()
        httpCacheDir.setReadable(true, false)
        httpCacheDir.setWritable(true, false)
        httpCacheDir.setExecutable(true, false)

        val codeCacheDir = java.io.File(httpCacheDir, "Code Cache")
        codeCacheDir.mkdirs()
        codeCacheDir.setReadable(true, false)
        codeCacheDir.setWritable(true, false)
        codeCacheDir.setExecutable(true, false)

        val jsDir = java.io.File(codeCacheDir, "js")
        jsDir.mkdirs()
        jsDir.setReadable(true, false)
        jsDir.setWritable(true, false)
        jsDir.setExecutable(true, false)

        val wasmDir = java.io.File(codeCacheDir, "wasm")
        wasmDir.mkdirs()
        wasmDir.setReadable(true, false)
        wasmDir.setWritable(true, false)
        wasmDir.setExecutable(true, false)
    } catch (e: Exception) {
        // Ignore any file creation issues
    }

    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    // Initialize Device Performance Manager for low RAM / low storage optimization
    try {
        com.example.studio.model.DevicePerformanceManager.initialize(applicationContext)
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Restore saved default theme from SharedPreferences
    try {
        val pref = getSharedPreferences("studio_prefs", android.content.Context.MODE_PRIVATE)
        val savedThemeId = pref.getString("default_theme_id", com.example.ui.theme.StudioTheme.MONOCHROME_ACTIVE.id)
        val savedTheme = com.example.ui.theme.StudioTheme.values().find { it.id == savedThemeId }
        if (savedTheme != null) {
            com.example.ui.theme.currentThemeStateBySelection.value = savedTheme
        }
    } catch (e: Exception) {
        // Fallback gracefully
    }

    // Enable Edge-to-Edge layout: forcing the layout hierarchy to draw beneath status and navigation bars
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

    // Extract the WindowInsetsControllerCompat instance to configure system UI parameters
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

    // Set layout behavior to system bars showing briefly on gesture/swipe and hiding again
    windowInsetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

    // Hide the system status bar and the screen navigation bar under-pill
    windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())

    setContent {
      MyApplicationTheme {
        WorkspaceScreen(modifier = Modifier.fillMaxSize())
      }
    }
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    if (level >= TRIM_MEMORY_RUNNING_LOW || level >= TRIM_MEMORY_MODERATE) {
      com.example.studio.model.DevicePerformanceManager.trimAllMemoryCaches()
    }
  }

  override fun onLowMemory() {
    super.onLowMemory()
    com.example.studio.model.DevicePerformanceManager.trimAllMemoryCaches()
  }
}

