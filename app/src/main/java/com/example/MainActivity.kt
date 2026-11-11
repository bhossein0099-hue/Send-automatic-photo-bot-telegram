package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Queue
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.LockScreen
import com.example.ui.screens.MainScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.M3Background
import com.example.ui.theme.M3OnSecondaryContainer
import com.example.ui.theme.M3OnSurfaceVariant
import com.example.ui.theme.M3Outline
import com.example.ui.theme.M3SecondaryContainer
import com.example.ui.theme.M3SurfaceContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.util.PermissionHelper

object NavRoutes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val QUEUE = "queue"
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    AppNavigation(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshBatteryOptimizationStatus()
    }

    override fun onStop() {
        super.onStop()
        viewModel.lockApp()
    }
}

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavRoutes.MAIN

    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

    if (isAppLockEnabled && !isAuthenticated) {
        LockScreen(
            onUnlock = { pin -> viewModel.unlockWithPin(pin) }
        )
        return
    }

    // Request necessary runtime permissions on start
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions evaluated
    }

    LaunchedEffect(Unit) {
        if (!PermissionHelper.hasAllRequiredPermissions(context)) {
            val required = PermissionHelper.getRequiredPermissions()
            permissionLauncher.launch(required.toTypedArray())
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = M3Background,
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = M3Outline, thickness = 1.dp)
                NavigationBar(
                    containerColor = M3SurfaceContainer,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(72.dp)
                ) {
                    // Item 1: Main
                    NavigationBarItem(
                        selected = currentRoute == NavRoutes.MAIN,
                        onClick = {
                            if (currentRoute != NavRoutes.MAIN) {
                                navController.navigate(NavRoutes.MAIN) {
                                    popUpTo(NavRoutes.MAIN) { inclusive = true }
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "اصلی") },
                        label = {
                            Text(
                                text = "اصلی",
                                fontSize = 12.sp,
                                fontWeight = if (currentRoute == NavRoutes.MAIN) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = M3SecondaryContainer,
                            selectedIconColor = M3OnSecondaryContainer,
                            selectedTextColor = M3OnSecondaryContainer,
                            unselectedIconColor = M3OnSurfaceVariant,
                            unselectedTextColor = M3OnSurfaceVariant
                        )
                    )

                    // Item 2: Queue
                    NavigationBarItem(
                        selected = currentRoute == NavRoutes.QUEUE,
                        onClick = {
                            if (currentRoute != NavRoutes.QUEUE) {
                                navController.navigate(NavRoutes.QUEUE) {
                                    popUpTo(NavRoutes.MAIN)
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Queue, contentDescription = "صف ارسال") },
                        label = {
                            Text(
                                text = "صف ارسال",
                                fontSize = 12.sp,
                                fontWeight = if (currentRoute == NavRoutes.QUEUE) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = M3SecondaryContainer,
                            selectedIconColor = M3OnSecondaryContainer,
                            selectedTextColor = M3OnSecondaryContainer,
                            unselectedIconColor = M3OnSurfaceVariant,
                            unselectedTextColor = M3OnSurfaceVariant
                        )
                    )

                    // Item 3: Settings
                    NavigationBarItem(
                        selected = currentRoute == NavRoutes.SETTINGS,
                        onClick = {
                            if (currentRoute != NavRoutes.SETTINGS) {
                                navController.navigate(NavRoutes.SETTINGS) {
                                    popUpTo(NavRoutes.MAIN)
                                }
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "تنظیمات") },
                        label = {
                            Text(
                                text = "تنظیمات",
                                fontSize = 12.sp,
                                fontWeight = if (currentRoute == NavRoutes.SETTINGS) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = M3SecondaryContainer,
                            selectedIconColor = M3OnSecondaryContainer,
                            selectedTextColor = M3OnSecondaryContainer,
                            unselectedIconColor = M3OnSurfaceVariant,
                            unselectedTextColor = M3OnSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = NavRoutes.MAIN
            ) {
                composable(NavRoutes.MAIN) {
                    MainScreen(
                        viewModel = viewModel,
                        onNavigateToSettings = { navController.navigate(NavRoutes.SETTINGS) },
                        onNavigateToQueue = { navController.navigate(NavRoutes.QUEUE) }
                    )
                }

                composable(NavRoutes.SETTINGS) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(NavRoutes.QUEUE) {
                    QueueScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
