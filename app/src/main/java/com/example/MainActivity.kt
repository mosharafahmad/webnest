package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.preferences.ThemeMode
import com.example.ui.screens.WebNestHomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BookmarkViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val bookmarkViewModel: BookmarkViewModel = viewModel(
                factory = BookmarkViewModel.provideFactory(applicationContext)
            )
            val uiState by bookmarkViewModel.uiState.collectAsStateWithLifecycle()

            val useDarkTheme = when (uiState.settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                WebNestHomeScreen(viewModel = bookmarkViewModel)
            }
        }
    }
}
