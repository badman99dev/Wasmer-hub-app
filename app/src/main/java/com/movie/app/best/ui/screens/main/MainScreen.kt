package com.movie.app.best.ui.screens.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.movie.app.best.ui.navigation.AppNavigation
import com.movie.app.best.ui.navigation.BottomNavigationBar
import com.movie.app.best.ui.navigation.Screen
import com.movie.app.best.ui.screens.auth.AuthViewModel
import com.movie.app.best.ui.screens.splash.SplashScreen

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    var splashScreenFinished by remember { mutableStateOf(false) }
    var storagePermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        storagePermissionGranted = granted
    }

    if (!splashScreenFinished) {
        SplashScreen(onSplashScreenFinish = { splashScreenFinished = true })
    } else {
        if (!storagePermissionGranted && Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            LaunchedEffect(Unit) {
                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            storagePermissionGranted = true
        }

        MainContent()
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainContent() {
    val context = LocalContext.current
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var prevLoggedIn by remember { mutableStateOf(authState.isLoggedIn) }

    LaunchedEffect(authState.isLoggedIn) {
        if (prevLoggedIn != authState.isLoggedIn) {
            prevLoggedIn = authState.isLoggedIn
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!isOnline(context)) {
            navController.navigate(Screen.Library.route)
        }
    }

    val shouldShowBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Movies.route,
        Screen.TVShows.route,
        Screen.Categories.route,
        Screen.Library.route,
        Screen.Trending.route,
        Screen.Downloads.route,
        Screen.Profile.route
    )

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                BottomNavigationBar(
                    navController = navController
                )
            }
        }
    ) { innerPadding ->
        AppNavigation(
            navController = navController,
        )
    }
}

private fun isOnline(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}