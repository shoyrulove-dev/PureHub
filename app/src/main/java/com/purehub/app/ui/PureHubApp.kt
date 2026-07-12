package com.purehub.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.purehub.app.feature.catalog.MiniAppId
import com.purehub.app.navigation.PureHubDestination.FinanceFun
import com.purehub.app.navigation.PureHubDestination.Help
import com.purehub.app.navigation.PureHubDestination.MeasureTools
import com.purehub.app.navigation.PureHubDestination.Settings
import com.purehub.app.navigation.PureHubDestination.SystemSecurity
import com.purehub.app.navigation.PureHubDestination.Vision
import com.purehub.app.navigation.PureHubDestination.ZenTime
import com.purehub.app.navigation.bottomNavDestinations
import com.purehub.app.ui.screens.BillSplitterCard
import com.purehub.app.ui.screens.BoostScreen
import com.purehub.app.ui.screens.BubbleLevelCard
import com.purehub.app.ui.screens.CleanerScreen
import com.purehub.app.ui.screens.ColorGrabberCard
import com.purehub.app.ui.screens.CommunityScreen
import com.purehub.app.ui.screens.CompassScreen
import com.purehub.app.ui.screens.DecibelMeterCard
import com.purehub.app.ui.screens.DecisionWheelCard
import com.purehub.app.ui.screens.DocToPdfCard
import com.purehub.app.ui.screens.ExpenseTrackerCard
import com.purehub.app.ui.screens.FinanceFunScreen
import com.purehub.app.ui.screens.HelpScreen
import com.purehub.app.ui.screens.LunarCalendarScreen
import com.purehub.app.ui.screens.OcrTextExtractorCard
import com.purehub.app.ui.screens.PasswordVaultCard
import com.purehub.app.ui.screens.PomodoroCard
import com.purehub.app.ui.screens.QrStudioScreen
import com.purehub.app.ui.screens.ScanScreen
import com.purehub.app.ui.screens.SettingsScreen
import com.purehub.app.ui.screens.SmartFlashlightCard
import com.purehub.app.ui.screens.SpeakerCleanerCard
import com.purehub.app.ui.screens.ToolsScreen
import com.purehub.app.ui.screens.UnitConverterCard
import com.purehub.app.ui.screens.WallpaperChangerCard
import com.purehub.app.ui.screens.WifiAnalyzerCard
import com.purehub.app.ui.screens.ZenBreathCard
import com.purehub.app.ui.screens.ZenHabitCard
import com.purehub.app.ui.screens.ZenTimeScreen

private const val MINI_APP_ROUTE_PREFIX = "mini_app"

@Composable
fun PureHubApp() {
    val navController = rememberNavController()
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry.value?.destination
    val snackbarHostState = remember { SnackbarHostState() }
    val currentMiniAppRoute = currentDestination?.route?.startsWith("$MINI_APP_ROUTE_PREFIX/") == true

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (currentMiniAppRoute) {
                    MiniAppTopBar(
                        title = currentMiniAppTitle(currentDestination?.route),
                        onBack = { navController.popBackStack() },
                    )
                }
            },
            bottomBar = {
                if (!currentMiniAppRoute) {
                    NavigationBar {
                        bottomNavDestinations.forEach { destination ->
                            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.route,
                                    )
                                },
                                alwaysShowLabel = false,
                                label = null,
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            PureHubNavHost(
                innerPadding = innerPadding,
                navController = navController,
                onOpenMiniApp = { navController.navigate(miniAppRoute(it)) },
                onOpenHelp = { navController.navigate(Help.route) },
            )
        }
    }
}

@Composable
private fun PureHubNavHost(
    innerPadding: PaddingValues,
    navController: NavHostController,
    onOpenMiniApp: (MiniAppId) -> Unit,
    onOpenHelp: () -> Unit,
) {
    NavHost(
        navController = navController,
        startDestination = ZenTime.route,
    ) {
        composable(ZenTime.route) {
            ZenTimeScreen(
                innerPadding = innerPadding,
                onOpenSettings = { navController.navigate(Settings.route) },
                onOpenMiniApp = onOpenMiniApp,
            )
        }
        composable(MeasureTools.route) {
            ToolsScreen(
                innerPadding = innerPadding,
                onOpenMiniApp = onOpenMiniApp,
            )
        }
        composable(Vision.route) {
            ScanScreen(
                innerPadding = innerPadding,
                onOpenMiniApp = onOpenMiniApp,
            )
        }
        composable(SystemSecurity.route) {
            BoostScreen(
                innerPadding = innerPadding,
                onOpenMiniApp = onOpenMiniApp,
            )
        }
        composable(FinanceFun.route) {
            FinanceFunScreen(
                innerPadding = innerPadding,
                onOpenMiniApp = onOpenMiniApp,
            )
        }
        composable(Settings.route) {
            SettingsScreen(
                innerPadding = innerPadding,
                onOpenHelp = onOpenHelp,
            )
        }
        composable(Help.route) {
            HelpScreen(innerPadding = innerPadding)
        }
        composable("$MINI_APP_ROUTE_PREFIX/{miniAppId}") { entry ->
            val miniAppId = entry.arguments?.getString("miniAppId")?.let(MiniAppId::valueOf) ?: return@composable
            MiniAppScreen(
                innerPadding = innerPadding,
                miniAppId = miniAppId,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiniAppTopBar(
    title: String,
    onBack: () -> Unit,
) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                )
            }
        },
    )
}

@Composable
private fun MiniAppScreen(
    innerPadding: PaddingValues,
    miniAppId: MiniAppId,
) {
    val context = LocalContext.current
    var hasCameraPermission by rememberSaveable {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
    }

    when (miniAppId) {
        MiniAppId.LUNAR_CALENDAR -> LunarCalendarScreen(innerPadding = innerPadding, embedded = false)
        MiniAppId.ZEN_HABIT -> ScrollHost(innerPadding) { ZenHabitCard() }
        MiniAppId.ZEN_POMODORO -> ScrollHost(innerPadding) { PomodoroCard() }
        MiniAppId.ZEN_BREATH -> ScrollHost(innerPadding) { ZenBreathCard() }
        MiniAppId.COMPASS -> CompassScreen(innerPadding = innerPadding, embedded = false)
        MiniAppId.BUBBLE_LEVEL -> ScrollHost(innerPadding) { BubbleLevelCard() }
        MiniAppId.DECIBEL_METER -> ScrollHost(innerPadding) { DecibelMeterCard() }
        MiniAppId.SMART_FLASHLIGHT -> ScrollHost(innerPadding) { SmartFlashlightCard() }
        MiniAppId.UNIT_CONVERTER -> ScrollHost(innerPadding) { UnitConverterCard() }
        MiniAppId.QR_STUDIO -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            QrStudioScreen(
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            )
        }
        MiniAppId.DOC_TO_PDF -> ScrollHost(innerPadding) {
            DocToPdfCard(
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            )
        }
        MiniAppId.OCR_TEXT -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OcrTextExtractorCard(
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            )
        }
        MiniAppId.COLOR_GRABBER -> ScrollHost(innerPadding) {
            ColorGrabberCard(
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            )
        }
        MiniAppId.DEEP_CLEANER -> CleanerScreen(innerPadding = innerPadding, embedded = false)
        MiniAppId.SPEAKER_CLEANER -> ScrollHost(innerPadding) { SpeakerCleanerCard() }
        MiniAppId.WIFI_ANALYZER -> ScrollHost(innerPadding) { WifiAnalyzerCard() }
        MiniAppId.PASSWORD_VAULT -> ScrollHost(innerPadding) { PasswordVaultCard() }
        MiniAppId.WALLPAPER_CHANGER -> ScrollHost(innerPadding) { WallpaperChangerCard() }
        MiniAppId.BILL_SPLITTER -> ScrollHost(innerPadding) { BillSplitterCard() }
        MiniAppId.EXPENSE_TRACKER -> ScrollHost(innerPadding) { ExpenseTrackerCard() }
        MiniAppId.DECISION_WHEEL -> ScrollHost(innerPadding) { DecisionWheelCard() }
        MiniAppId.COMMUNITY_UNLOCK -> CommunityScreen(innerPadding = innerPadding, embedded = false)
    }
}

@Composable
private fun ScrollHost(
    innerPadding: PaddingValues,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState()),
    ) {
        content()
    }
}

private fun miniAppRoute(miniAppId: MiniAppId): String = "$MINI_APP_ROUTE_PREFIX/${miniAppId.name}"

private fun currentMiniAppTitle(route: String?): String {
    val rawId = route?.substringAfter("$MINI_APP_ROUTE_PREFIX/", "")
    return MiniAppId.entries.firstOrNull { it.name == rawId }?.title.orEmpty()
}
