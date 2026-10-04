package com.purehub.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.purehub.app.navigation.PureHubDestination.Home
import com.purehub.app.navigation.PureHubDestination.AllTools
import com.purehub.app.navigation.PureHubDestination.Community
import com.purehub.app.navigation.PureHubDestination.MeasureTools
import com.purehub.app.navigation.PureHubDestination.Settings
import com.purehub.app.navigation.PureHubDestination.SystemSecurity
import com.purehub.app.navigation.PureHubDestination.Vision
import com.purehub.app.navigation.PureHubDestination.ZenTime
import com.purehub.app.navigation.bottomNavDestinations
import com.purehub.app.navigation.labelFor
import com.purehub.app.ui.screens.BillSplitterCard
import com.purehub.app.ui.screens.BoostScreen
import com.purehub.app.ui.screens.BubbleLevelCard
import com.purehub.app.ui.screens.CleanerScreen
import com.purehub.app.ui.screens.ColorGrabberCard
import com.purehub.app.ui.screens.ColorGrabberMenuAction
import com.purehub.app.ui.screens.CommunityScreen
import com.purehub.app.ui.screens.CompassScreen
import com.purehub.app.ui.screens.DecibelMeterCard
import com.purehub.app.ui.screens.DecisionWheelCard
import com.purehub.app.ui.screens.DocToPdfCard
import com.purehub.app.ui.screens.DocToPdfMenuAction
import com.purehub.app.ui.screens.ExpenseTrackerCard
import com.purehub.app.ui.screens.FinanceFunScreen
import com.purehub.app.ui.screens.HelpScreen
import com.purehub.app.ui.screens.HomeScreen
import com.purehub.app.ui.screens.LunarCalendarScreen
import com.purehub.app.ui.screens.OcrTextExtractorCard
import com.purehub.app.ui.screens.PasswordVaultCard
import com.purehub.app.ui.screens.PhotoPrivacyScreen
import com.purehub.app.ui.screens.PhotoPrivacyMenuAction
import com.purehub.app.ui.screens.AuthenticatorVaultCard
import com.purehub.app.ui.screens.FileStudioCard
import com.purehub.app.ui.screens.PomodoroCard
import com.purehub.app.ui.screens.QrStudioScreen
import com.purehub.app.ui.screens.QrStudioTab
import com.purehub.app.ui.screens.QrMenuRequest
import com.purehub.app.ui.screens.ScanScreen
import com.purehub.app.ui.screens.ScreenRecorderCard
import com.purehub.app.ui.screens.ScreenRecorderMenuAction
import com.purehub.app.ui.screens.SettingsScreen
import com.purehub.app.ui.screens.PlayTesterInviteSheet
import com.purehub.app.ui.screens.dismissPlayTesterInvite
import com.purehub.app.ui.screens.openPlayTesterGroup
import com.purehub.app.ui.screens.shouldShowPlayTesterInvite
import com.purehub.app.ui.screens.SmartFlashlightCard
import com.purehub.app.ui.screens.SpeakerCleanerCard
import com.purehub.app.ui.screens.ToolsScreen
import com.purehub.app.ui.screens.UnitConverterCard
import com.purehub.app.ui.screens.WallpaperChangerCard
import com.purehub.app.ui.screens.WifiAnalyzerCard
import com.purehub.app.ui.screens.ZenBreathCard
import com.purehub.app.ui.screens.ZenHabitCard
import com.purehub.app.ui.screens.ZenTimeScreen
import com.purehub.app.ui.screens.VisionFullscreenShell

private const val MINI_APP_ROUTE_PREFIX = "mini_app"

/** Vision tools own their immersive navigation, so they must not also receive the
 * generic mini-app header from the application scaffold. */
private val immersiveVisionTools = setOf(
    MiniAppId.QR_STUDIO,
    MiniAppId.DOC_TO_PDF,
    MiniAppId.OCR_TEXT,
    MiniAppId.COLOR_GRABBER,
    MiniAppId.PHOTO_PRIVACY,
    MiniAppId.SCREEN_RECORDER,
)

@Composable
private fun LanguageWelcomeScreen(
    selected: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onContinue: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text("PureHub", style = MaterialTheme.typography.displaySmall)
            Text("Choose your language", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 12.dp))
            Text("Chọn ngôn ngữ · 选择语言 · Elige idioma", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            Spacer(Modifier.height(24.dp))
            AppLanguage.entries.forEach { language ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), onClick = { onSelect(language) }) {
                    Text("${if (selected == language) "✓ " else ""}${language.label}", modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.titleMedium)
                }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Text(appText(selected, "Continue", "Tiếp tục", "继续", "Continuar"))
            }
        }
    }
}

@Composable
fun PureHubApp(initialMiniAppId: MiniAppId? = null) {
    val context = LocalContext.current
    var language by rememberSaveable { mutableStateOf(context.loadAppLanguage()) }
    var languageChosen by rememberSaveable { mutableStateOf(context.hasChosenAppLanguage()) }
    if (!languageChosen) {
        LanguageWelcomeScreen(
            selected = language,
            onSelect = { language = it },
            onContinue = { context.saveAppLanguage(language); languageChosen = true },
        )
        return
    }
    val navController = rememberNavController()
    val backStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry.value?.destination
    val currentMiniApp = backStackEntry.value?.arguments
        ?.getString("miniAppId")
        ?.let { value -> MiniAppId.entries.firstOrNull { it.name == value } }
    val snackbarHostState = remember { SnackbarHostState() }
    val currentMiniAppRoute = currentDestination?.route?.startsWith("$MINI_APP_ROUTE_PREFIX/") == true
    var showTesterInvite by rememberSaveable {
        mutableStateOf(initialMiniAppId == null && context.shouldShowPlayTesterInvite())
    }

    // An app shortcut can arrive while MainActivity is already visible. Navigate from
    // the retained NavController rather than relying only on NavHost's initial route.
    LaunchedEffect(initialMiniAppId) {
        initialMiniAppId?.let { miniAppId ->
            navController.navigate(miniAppRoute(miniAppId)) {
                launchSingleTop = true
            }
        }
    }

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState, LocalAppLanguage provides language) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar { LocalizedText(data.visuals.message) }
                }
            },
            topBar = {
                if (currentMiniAppRoute && currentMiniApp !in immersiveVisionTools) {
                    MiniAppTopBar(
                        title = currentMiniApp?.title.orEmpty(),
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
                                    if (destination == Home) {
                                        navController.returnHome()
                                    } else {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = destination.route,
                                    )
                                },
                                alwaysShowLabel = false,
                                label = { Text(destination.labelFor(language)) },
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
                language = language,
                onLanguageChange = { next -> context.saveAppLanguage(next); language = next },
                onJoinPlayTesters = { context.openPlayTesterGroup() },
                initialMiniAppId = initialMiniAppId,
            )
        }
        if (showTesterInvite && !currentMiniAppRoute) {
            PlayTesterInviteSheet(
                language = language,
                onJoin = {
                    context.dismissPlayTesterInvite()
                    showTesterInvite = false
                    context.openPlayTesterGroup()
                },
                onLater = {
                    context.dismissPlayTesterInvite()
                    showTesterInvite = false
                },
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
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onJoinPlayTesters: () -> Unit,
    initialMiniAppId: MiniAppId? = null,
) {
    NavHost(
        navController = navController,
        startDestination = initialMiniAppId?.let(::miniAppRoute) ?: Home.route,
    ) {
        composable(Home.route) {
            HomeScreen(
                innerPadding = innerPadding,
                onOpenSettings = { navController.navigate(Settings.route) },
                onOpenMiniApp = onOpenMiniApp,
            )
        }
        composable(AllTools.route) {
            ToolsScreen(
                innerPadding = innerPadding,
                onOpenMiniApp = onOpenMiniApp,
                showAllTools = true,
            )
        }
        composable(Community.route) {
            CommunityScreen(innerPadding = innerPadding, embedded = false)
        }
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
                language = language,
                onLanguageChange = onLanguageChange,
                onJoinPlayTesters = onJoinPlayTesters,
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
                onBack = { navController.popBackStack() },
                onHome = { navController.returnHome() },
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
        title = { LocalizedText(if (title == "WiFi Analyzer") localizedWifiAnalyzerTitle() else title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = translateUiText("Back", LocalAppLanguage.current),
                )
            }
        },
    )
}

@Composable
private fun localizedWifiAnalyzerTitle(): String = when (LocalAppLanguage.current) {
    AppLanguage.Vietnamese -> "Phân tích Wi-Fi"
    AppLanguage.Chinese -> "Wi-Fi 分析"
    AppLanguage.Spanish -> "Analizador Wi-Fi"
    AppLanguage.English -> "WiFi Analyzer"
}

@Composable
private fun MiniAppScreen(
    innerPadding: PaddingValues,
    miniAppId: MiniAppId,
    onBack: () -> Unit,
    onHome: () -> Unit,
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
        MiniAppId.BUBBLE_LEVEL -> BubbleLevelCard(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        )
        MiniAppId.DECIBEL_METER -> ScrollHost(innerPadding) { DecibelMeterCard() }
        MiniAppId.SMART_FLASHLIGHT -> ScrollHost(innerPadding) { SmartFlashlightCard() }
        MiniAppId.UNIT_CONVERTER -> ScrollHost(innerPadding) { UnitConverterCard() }
        MiniAppId.QR_STUDIO -> {
            var selectedQrTab by rememberSaveable { mutableStateOf(QrStudioTab.Scan) }
            var qrMenuRequest by rememberSaveable { mutableStateOf<QrMenuRequest?>(null) }
            VisionFullscreenShell(
                title = "QR Studio",
                onHome = onHome,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                drawerActions = { closeDrawer ->
                    NavigationDrawerItem(
                        label = { LocalizedText("Scan") },
                        selected = selectedQrTab == QrStudioTab.Scan,
                        icon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
                        onClick = { selectedQrTab = QrStudioTab.Scan; closeDrawer() },
                        modifier = Modifier.height(48.dp),
                    )
                    NavigationDrawerItem(
                        label = { LocalizedText("Create QR") },
                        selected = selectedQrTab == QrStudioTab.Create,
                        icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                        onClick = { selectedQrTab = QrStudioTab.Create; closeDrawer() },
                        modifier = Modifier.height(48.dp),
                    )
                    NavigationDrawerItem(
                        label = { LocalizedText("History") },
                        selected = selectedQrTab == QrStudioTab.Library,
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        onClick = { selectedQrTab = QrStudioTab.Library; closeDrawer() },
                        modifier = Modifier.height(48.dp),
                    )
                    NavigationDrawerItem(
                        label = { LocalizedText("Scan image") },
                        selected = false,
                        icon = { Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null) },
                        onClick = {
                            selectedQrTab = QrStudioTab.Scan
                            qrMenuRequest = QrMenuRequest.Image
                            closeDrawer()
                        },
                        modifier = Modifier.height(48.dp),
                    )
                    NavigationDrawerItem(
                        label = { LocalizedText("Batch scan") },
                        selected = false,
                        icon = { Icon(Icons.Rounded.Collections, contentDescription = null) },
                        onClick = {
                            selectedQrTab = QrStudioTab.Scan
                            qrMenuRequest = QrMenuRequest.Batch
                            closeDrawer()
                        },
                        modifier = Modifier.height(48.dp),
                    )
                },
            ) { visionPadding ->
                QrStudioScreen(
                    hasCameraPermission = hasCameraPermission,
                    onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    selectedTab = selectedQrTab,
                    onTabSelected = { selectedQrTab = it },
                    menuRequest = qrMenuRequest,
                    onMenuRequestHandled = { qrMenuRequest = null },
                    innerPadding = visionPadding,
                )
            }
        }
        MiniAppId.DOC_TO_PDF -> {
            var menuAction by rememberSaveable { mutableStateOf<DocToPdfMenuAction?>(null) }
            VisionFullscreenShell(
                title = "Doc to PDF",
                onHome = onHome,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                drawerActions = { closeDrawer ->
                    NavigationDrawerItem(label = { LocalizedText("Capture Page", maxLines = 1, softWrap = false) }, selected = false, icon = { Icon(Icons.Rounded.QrCodeScanner, null) }, onClick = { menuAction = DocToPdfMenuAction.Camera; closeDrawer() }, modifier = Modifier.height(48.dp))
                    NavigationDrawerItem(label = { LocalizedText("Import images", maxLines = 1, softWrap = false) }, selected = false, icon = { Icon(Icons.Rounded.AddPhotoAlternate, null) }, onClick = { menuAction = DocToPdfMenuAction.Images; closeDrawer() }, modifier = Modifier.height(48.dp))
                    NavigationDrawerItem(label = { LocalizedText("PDF toolbox", maxLines = 1, softWrap = false) }, selected = false, icon = { Icon(Icons.Rounded.AutoAwesome, null) }, onClick = { menuAction = DocToPdfMenuAction.PdfToolbox; closeDrawer() }, modifier = Modifier.height(48.dp))
                },
            ) { visionPadding ->
            DocToPdfCard(
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                menuAction = menuAction,
                onMenuActionHandled = { menuAction = null },
                innerPadding = visionPadding,
            )
            }
        }
        MiniAppId.OCR_TEXT -> Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            OcrTextExtractorCard(
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onExit = onHome,
            )
        }
        MiniAppId.COLOR_GRABBER -> {
            var menuAction by rememberSaveable { mutableStateOf<ColorGrabberMenuAction?>(null) }
            VisionFullscreenShell(
                title = "Color Grabber",
                onHome = onHome,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                drawerActions = { closeDrawer ->
                    NavigationDrawerItem(label = { LocalizedText("Camera", maxLines = 1, softWrap = false) }, selected = false, icon = { Icon(Icons.Rounded.QrCodeScanner, null) }, onClick = { menuAction = ColorGrabberMenuAction.Camera; closeDrawer() }, modifier = Modifier.height(48.dp))
                    NavigationDrawerItem(label = { LocalizedText("Copy HEX", maxLines = 1, softWrap = false) }, selected = false, icon = { Icon(Icons.Rounded.Collections, null) }, onClick = { menuAction = ColorGrabberMenuAction.CopyHex; closeDrawer() }, modifier = Modifier.height(48.dp))
                },
            ) { visionPadding ->
            ColorGrabberCard(
                hasCameraPermission = hasCameraPermission,
                onRequestCameraPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                menuAction = menuAction,
                onMenuActionHandled = { menuAction = null },
                innerPadding = visionPadding,
            )
            }
        }
        MiniAppId.PHOTO_PRIVACY -> {
            var menuAction by rememberSaveable { mutableStateOf<PhotoPrivacyMenuAction?>(null) }
            VisionFullscreenShell(
                title = "Photo Privacy",
                onHome = onHome,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                drawerActions = { closeDrawer ->
                    NavigationDrawerItem(label = { LocalizedText("Choose photo", maxLines = 1, softWrap = false) }, selected = false, icon = { Icon(Icons.Rounded.AddPhotoAlternate, null) }, onClick = { menuAction = PhotoPrivacyMenuAction.ChoosePhoto; closeDrawer() }, modifier = Modifier.height(48.dp))
                },
            ) { visionPadding -> PhotoPrivacyScreen(innerPadding = visionPadding, menuAction = menuAction, onMenuActionHandled = { menuAction = null }) }
        }
        MiniAppId.DEEP_CLEANER -> CleanerScreen(innerPadding = innerPadding, embedded = false)
        MiniAppId.SPEAKER_CLEANER -> ScrollHost(innerPadding) { SpeakerCleanerCard() }
        MiniAppId.WIFI_ANALYZER -> WifiAnalyzerCard(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 8.dp, vertical = 6.dp))
        MiniAppId.PASSWORD_VAULT -> ScrollHost(innerPadding) { PasswordVaultCard() }
        MiniAppId.AUTHENTICATOR_VAULT -> ScrollHost(innerPadding) { AuthenticatorVaultCard() }
        MiniAppId.FILE_STUDIO -> ScrollHost(innerPadding) { FileStudioCard() }
        MiniAppId.WALLPAPER_CHANGER -> ScrollHost(innerPadding) { WallpaperChangerCard() }
        MiniAppId.BILL_SPLITTER -> ScrollHost(innerPadding) { BillSplitterCard() }
        MiniAppId.EXPENSE_TRACKER -> ScrollHost(innerPadding) { ExpenseTrackerCard() }
        MiniAppId.DECISION_WHEEL -> ScrollHost(innerPadding) { DecisionWheelCard() }
        MiniAppId.COMMUNITY_UNLOCK -> CommunityScreen(innerPadding = innerPadding, embedded = false)
        MiniAppId.SCREEN_RECORDER -> {
            var menuAction by rememberSaveable { mutableStateOf<ScreenRecorderMenuAction?>(null) }
            VisionFullscreenShell(
                title = "Screen Recorder",
                onHome = onHome,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                drawerActions = { closeDrawer ->
                    NavigationDrawerItem(label = { LocalizedText("Start recording", maxLines = 1, softWrap = false) }, selected = false, icon = { Icon(Icons.Rounded.QrCodeScanner, null) }, onClick = { menuAction = ScreenRecorderMenuAction.Start; closeDrawer() }, modifier = Modifier.height(48.dp))
                    NavigationDrawerItem(label = { LocalizedText("Quality preset", maxLines = 1, softWrap = false) }, selected = false, icon = { Icon(Icons.Rounded.AutoAwesome, null) }, onClick = { menuAction = ScreenRecorderMenuAction.Quality; closeDrawer() }, modifier = Modifier.height(48.dp))
                },
            ) { visionPadding -> ScreenRecorderCard(innerPadding = visionPadding, menuAction = menuAction, onMenuActionHandled = { menuAction = null }) }
        }
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
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

private fun miniAppRoute(miniAppId: MiniAppId): String = "$MINI_APP_ROUTE_PREFIX/${miniAppId.name}"

/**
 * Home is an exit destination, not another restorable tab state. Pop to the
 * existing root when possible so a saved Settings state can never be restored
 * over Home. Shortcut launches may not have Home on their back stack, so the
 * fallback clears the graph and creates a fresh Home destination.
 */
private fun NavHostController.returnHome() {
    if (currentDestination?.route == Home.route) return
    if (popBackStack(Home.route, inclusive = false)) return
    navigate(Home.route) {
        popUpTo(graph.id)
        launchSingleTop = true
        restoreState = false
    }
}
