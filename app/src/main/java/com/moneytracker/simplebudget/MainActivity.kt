package com.moneytracker.simplebudget

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.isSystemInDarkTheme
import com.moneytracker.simplebudget.BuildConfig
import com.moneytracker.simplebudget.data.preferences.PreferencesManager
import com.moneytracker.simplebudget.data.preferences.ThemeMode
import com.moneytracker.simplebudget.navigation.NavGraph
import com.moneytracker.simplebudget.navigation.Screen
import com.moneytracker.simplebudget.ui.onboarding.OnboardingScreen
import com.moneytracker.simplebudget.ui.theme.MoneyTrackerTheme
import kotlinx.coroutines.launch
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

sealed class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String,
    val hasSubmenu: Boolean = false
) {
    data object Records : BottomNavItem(
        "Transactions",
        Icons.Filled.Receipt,
        Icons.Outlined.Receipt,
        Screen.Dashboard.route
    )
    data object Stats : BottomNavItem(
        "Stats",
        Icons.Filled.Assessment,
        Icons.Outlined.Assessment,
        "stats",
        hasSubmenu = true
    )
    data object Accounts : BottomNavItem(
        "Accounts",
        Icons.Filled.AccountBalance,
        Icons.Outlined.AccountBalance,
        Screen.Accounts.route
    )
    data object Categories : BottomNavItem(
        "Categories",
        Icons.Filled.Category,
        Icons.Outlined.Category,
        Screen.Categories.route
    )
    data object Settings : BottomNavItem(
        "Settings",
        Icons.Filled.Settings,
        Icons.Outlined.Settings,
        Screen.Settings.route
    )
}

// Navigation bar accent color (golden/yellow like in the reference image)
private val NavBarAccent = Color(0xFFD4C896)
private val NavBarAccentDark = Color(0xFFB8A970)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onResume() {
        super.onResume()
        androidx.core.app.NotificationManagerCompat.from(this)
            .cancel(com.moneytracker.simplebudget.notifications.ReminderNotificationHelper.NOTIFICATION_ID)
    }

    /** Handles re-delivery when the activity is already running (singleTop). */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Modern exit animation: icon zooms up and fades while background slides away.
        // When launched from a notification, accessing splashScreenView.iconView can throw
        // an NPE inside the AndroidX SplashScreen library — wrap in try-catch and fall back
        // to an instant dismiss in that case.
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val iconView = try { splashScreenView.iconView } catch (_: Exception) { null }

            if (iconView == null) {
                splashScreenView.remove()
                return@setOnExitAnimationListener
            }

            // Icon zoom up animation (creates "diving into app" effect)
            val iconScaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 1.5f)
            val iconScaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 1.5f)
            val iconFade = ObjectAnimator.ofFloat(iconView, View.ALPHA, 1f, 0f)

            // Background slide up and fade
            val backgroundSlide = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_Y,
                0f,
                -splashScreenView.view.height.toFloat() * 0.3f
            )
            val backgroundFade = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f)

            val iconAnimator = AnimatorSet().apply {
                playTogether(iconScaleX, iconScaleY, iconFade)
                duration = 400L
                interpolator = DecelerateInterpolator(1.5f)
            }

            val backgroundAnimator = AnimatorSet().apply {
                playTogether(backgroundSlide, backgroundFade)
                duration = 350L
                startDelay = 100L
                interpolator = AnticipateOvershootInterpolator(0.5f)
            }

            AnimatorSet().apply {
                playTogether(iconAnimator, backgroundAnimator)
                doOnEnd { splashScreenView.remove() }
                start()
            }
        }

        enableEdgeToEdge()

        // Initialize Google Mobile Ads SDK
        MobileAds.initialize(this) {}

        setContent {
            val themeMode by preferencesManager.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val isDarkMode = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            val isPremium by preferencesManager.isPremium.collectAsState(initial = false)
            val onboardingCompleted by preferencesManager.onboardingCompleted.collectAsState(initial = true)
            var showOnboarding by remember { mutableStateOf(false) }
            var onboardingFinished by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            // Only show onboarding once the preference is loaded and it hasn't been completed.
            // onboardingFinished guards against re-triggering while the async DataStore write settles.
            if (!onboardingCompleted && !showOnboarding && !onboardingFinished) {
                showOnboarding = true
            }

            MoneyTrackerTheme(darkTheme = isDarkMode) {
                if (showOnboarding) {
                    OnboardingScreen(
                        onFinish = {
                            onboardingFinished = true
                            showOnboarding = false
                            coroutineScope.launch {
                                preferencesManager.setOnboardingCompleted(true)
                            }
                        }
                    )
                } else {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val bottomNavItems = listOf(
                        BottomNavItem.Records,
                        BottomNavItem.Stats,
                        BottomNavItem.Accounts,
                        BottomNavItem.Categories,
                        BottomNavItem.Settings
                    )

                    // Main nav destinations where bottom nav should be visible
                    val mainNavRoutes = listOf(
                        Screen.Dashboard.route,
                        Screen.MonthlyReports.route,
                        Screen.YearlyReports.route,
                        Screen.Accounts.route,
                        Screen.Categories.route,
                        Screen.Settings.route
                    )

                    val showBottomNav = currentDestination?.route in mainNavRoutes

                    // Track stats submenu visibility
                    var showStatsSubmenu by remember { mutableStateOf(false) }

                    // Determine which nav item is selected
                    val currentRoute = currentDestination?.route
                    val isStatsSelected = currentRoute in listOf(
                        Screen.MonthlyReports.route,
                        Screen.YearlyReports.route
                    )

                    // Routes where the ad banner should be visible
                    val adRoutes = listOf(
                        Screen.Accounts.route,
                        Screen.Categories.route,
                        Screen.Settings.route
                    )
                    // TODO: Restore for release
                    // val showAd = !isPremium && currentRoute in adRoutes
                    val showAd = false

                    // Create a single AdView instance that lives for the composable's lifetime
                    val configuration = LocalConfiguration.current
                    val screenWidthDp = configuration.screenWidthDp
                    val adView = remember {
                        AdView(this@MainActivity).apply {
                            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this@MainActivity, screenWidthDp))
                            adUnitId = BuildConfig.ADMOB_BANNER_ID
                            loadAd(AdRequest.Builder().build())
                        }
                    }

                    // Manage AdView lifecycle
                    val lifecycleOwner = LocalLifecycleOwner.current
                    DisposableEffect(lifecycleOwner) {
                        val observer = LifecycleEventObserver { _, event ->
                            when (event) {
                                Lifecycle.Event.ON_PAUSE -> adView.pause()
                                Lifecycle.Event.ON_RESUME -> adView.resume()
                                else -> {}
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                            adView.destroy()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
                                if (showBottomNav) {
                                    Column(modifier = Modifier.navigationBarsPadding()) {
                                        CustomNavigationBar(
                                            items = bottomNavItems,
                                            currentRoute = currentRoute,
                                            isStatsSelected = isStatsSelected,
                                            isDarkMode = isDarkMode,
                                            onItemClick = { item ->
                                                if (item.hasSubmenu) {
                                                    showStatsSubmenu = !showStatsSubmenu
                                                } else {
                                                    showStatsSubmenu = false
                                                    if (currentRoute != item.route) {
                                                        navController.navigate(item.route) {
                                                            popUpTo(navController.graph.findStartDestination().id) {
                                                                saveState = false
                                                            }
                                                            launchSingleTop = true
                                                            restoreState = false
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        ) { _ ->
                            NavGraph(
                                navController = navController,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Shared ad banner overlay — one AdView instance, visibility toggled per route
                        AndroidView(
                            factory = { adView },
                            update = { view ->
                                view.visibility = if (showAd) View.VISIBLE else View.GONE
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(bottom = 56.dp)
                                .fillMaxWidth()
                        )

                        // Dismiss overlay when stats submenu is open (captures taps outside submenu)
                        // Excludes the nav bar area so nav bar remains clickable
                        if (showStatsSubmenu) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .navigationBarsPadding()
                                    .padding(bottom = 56.dp) // Nav bar height
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showStatsSubmenu = false }
                            )
                        }

                        // Stats submenu popup - positioned above nav bar (on top of overlay)
                        if (showBottomNav) {
                            AnimatedVisibility(
                                visible = showStatsSubmenu,
                                enter = fadeIn() + slideInVertically { it },
                                exit = fadeOut() + slideOutVertically { it },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .offset(x = (-60).dp, y = (-70).dp)
                            ) {
                                StatsSubmenu(
                                    isDarkMode = isDarkMode,
                                    currentRoute = currentRoute,
                                    onMonthlyClick = {
                                        showStatsSubmenu = false
                                        if (currentRoute != Screen.MonthlyReports.route) {
                                            navController.navigate(Screen.MonthlyReports.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = false
                                                }
                                                launchSingleTop = true
                                                restoreState = false
                                            }
                                        }
                                    },
                                    onYearlyClick = {
                                        showStatsSubmenu = false
                                        if (currentRoute != Screen.YearlyReports.route) {
                                            navController.navigate(Screen.YearlyReports.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = false
                                                }
                                                launchSingleTop = true
                                                restoreState = false
                                            }
                                        }
                                    },
                                    onDismiss = { showStatsSubmenu = false }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    isStatsSelected: Boolean,
    isDarkMode: Boolean,
    onItemClick: (BottomNavItem) -> Unit
) {
    val backgroundColor = if (isDarkMode) {
        Color(0xFF1E1E1E)
    } else {
        Color(0xFFF5F5F0)
    }

    val selectedColor = if (isDarkMode) NavBarAccent else NavBarAccentDark
    val unselectedColor = if (isDarkMode) Color(0xFF8A8A7A) else Color(0xFF8A8A7A)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = backgroundColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = when {
                    item.hasSubmenu -> isStatsSelected
                    else -> currentRoute == item.route
                }

                val itemColor = if (isSelected) selectedColor else unselectedColor

                NavBarItem(
                    item = item,
                    isSelected = isSelected,
                    color = itemColor,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun NavBarItem(
    item: BottomNavItem,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "nav_press_scale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true, color = color),
                onClick = onClick
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = item.label,
            color = color,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun StatsSubmenu(
    isDarkMode: Boolean,
    currentRoute: String?,
    onMonthlyClick: () -> Unit,
    onYearlyClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val backgroundColor = if (isDarkMode) Color(0xFF3D3D3D) else Color(0xFFEEEEE8)
    val selectedColor = if (isDarkMode) NavBarAccent else NavBarAccentDark
    val unselectedColor = if (isDarkMode) Color(0xFFAAAAAA) else Color(0xFF8A8A7A)

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = backgroundColor,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Monthly option
            val isMonthlySelected = currentRoute == Screen.MonthlyReports.route
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onMonthlyClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .width(84.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Assessment,
                    contentDescription = null,
                    tint = if (isMonthlySelected) selectedColor else unselectedColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Monthly",
                    color = if (isMonthlySelected) selectedColor else unselectedColor,
                    fontSize = 12.sp,
                    fontWeight = if (isMonthlySelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            // Yearly option
            val isYearlySelected = currentRoute == Screen.YearlyReports.route
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onYearlyClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .width(84.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = if (isYearlySelected) selectedColor else unselectedColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Yearly",
                    color = if (isYearlySelected) selectedColor else unselectedColor,
                    fontSize = 12.sp,
                    fontWeight = if (isYearlySelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}
