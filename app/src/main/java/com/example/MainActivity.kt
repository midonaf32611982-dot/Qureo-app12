package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.QureoAutoSolverScreen
import com.example.ui.screens.RedeemCodeDialog
import com.example.ui.screens.SolveBothScreen
import com.example.ui.screens.SprixSolverScreen
import com.example.ui.screens.WisoGeneratorScreen
import com.example.ui.theme.CyberDarkBg
import com.example.ui.theme.MyApplicationTheme

import com.example.ui.screens.SplashScreen

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()

        // Start internal local HTTP server on 127.0.0.1:8770
        com.example.server.LocalSolverServer.start()

        setContent {
            val isArabic by viewModel.isArabic.collectAsState()
            val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme(darkTheme = true) {
                    SprixSolverApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.server.LocalSolverServer.stop()
    }
}

@Composable
fun SprixSolverApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val snackbarMsg by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.snackbarMessage.value = null
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = CyberDarkBg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberDarkBg)
        ) {
            Crossfade(
                targetState = currentScreen,
                label = "screen_crossfade"
            ) { screen ->
                when (screen) {
                    AppScreen.SPLASH -> SplashScreen(onSplashFinished = { viewModel.navigateTo(AppScreen.LOGIN) })
                    AppScreen.LOGIN -> LoginScreen(viewModel = viewModel)
                    AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                    AppScreen.SPRIX_SOLVER -> SprixSolverScreen(viewModel = viewModel)
                    AppScreen.QUREO_SOLVER -> QureoAutoSolverScreen(viewModel = viewModel)
                    AppScreen.WISO_GENERATOR -> WisoGeneratorScreen(viewModel = viewModel)
                    AppScreen.SOLVE_BOTH -> SolveBothScreen(viewModel = viewModel)
                    AppScreen.ADMIN_PANEL -> AdminDashboardScreen(viewModel = viewModel)
                }
            }

            // Global Dialog for redeeming activation codes
            RedeemCodeDialog(viewModel = viewModel)
        }
    }
}
