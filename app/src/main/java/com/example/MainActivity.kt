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

    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

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
}

