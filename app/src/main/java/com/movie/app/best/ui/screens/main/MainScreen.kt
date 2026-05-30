package com.movie.app.best.ui.screens.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.movie.app.best.ui.components.CategoryDrawerContent
import com.movie.app.best.ui.navigation.AppNavigation
import com.movie.app.best.ui.navigation.BottomNavigationBar
import com.movie.app.best.ui.navigation.Screen
import com.movie.app.best.ui.screens.auth.AuthViewModel
import com.movie.app.best.ui.screens.splash.SplashScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }
    var isConnected by remember { mutableStateOf(isCurrentlyConnected(connectivityManager)) }
    var wasOffline by remember { mutableStateOf(!isConnected) }
    var showBackOnlineBanner by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { isConnected = true }
            override fun onLost(network: Network) { isConnected = false }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                isConnected = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        onDispose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    LaunchedEffect(isConnected) {
        if (isConnected && wasOffline) {
            showBackOnlineBanner = true
            wasOffline = false
            delay(1500)
            showBackOnlineBanner = false
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
        } else if (!isConnected) {
            wasOffline = true
        }
    }

    LaunchedEffect(authState.isLoggedIn) {
        if (prevLoggedIn != authState.isLoggedIn) {
            prevLoggedIn = authState.isLoggedIn
            navController.navigate(Screen.Home.route) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                launchSingleTop = true
            }
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

    val shouldShowNoInternet = !isConnected && currentRoute != Screen.Downloads.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute in listOf(
            Screen.Home.route,
            Screen.Trending.route,
            Screen.Library.route,
            Screen.Downloads.route,
            Screen.Profile.route,
            Screen.Categories.route
        ) && isConnected,
        drawerContent = {
            CategoryDrawerContent(
                onCategoryClick = { slug, name ->
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.CategoryPage.createRoute(slug, name))
                },
                onAllCategoriesClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate(Screen.Categories.route)
                }
            )
        }
    ) {
        Scaffold(
            bottomBar = {
                Column {
                    if (showBackOnlineBanner) {
                        BackOnlineBanner()
                    }
                    if (shouldShowBottomBar) {
                        BottomNavigationBar(
                            navController = navController
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AppNavigation(
                    navController = navController,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )

                if (shouldShowNoInternet) {
                    NoInternetOverlay()
                }
            }
        }
    }
}

private fun isCurrentlyConnected(cm: ConnectivityManager): Boolean {
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
private fun NoInternetOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "No Internet Connection",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Check your network settings",
                color = Color.White.copy(alpha = 0.45f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Downloads are still available",
                color = Color(0xFF4CAF50).copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BackOnlineBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF4CAF50))
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "✓ Back Online",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
