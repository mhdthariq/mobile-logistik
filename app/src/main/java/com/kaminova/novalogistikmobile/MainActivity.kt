package com.kaminova.novalogistikmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kaminova.novalogistikmobile.data.api.NetworkModule
import com.kaminova.novalogistikmobile.data.repository.AuthRepository
import com.kaminova.novalogistikmobile.data.repository.DriverRepository
import com.kaminova.novalogistikmobile.data.repository.ExpenseRepository
import com.kaminova.novalogistikmobile.data.session.SessionManager
import com.kaminova.novalogistikmobile.ui.navigation.NavRoute
import com.kaminova.novalogistikmobile.ui.screens.capture.CaptureScreen
import com.kaminova.novalogistikmobile.ui.screens.capture.CaptureViewModel
import com.kaminova.novalogistikmobile.ui.screens.detail.TransportDetailScreen
import com.kaminova.novalogistikmobile.ui.screens.detail.TransportDetailViewModel
import com.kaminova.novalogistikmobile.ui.screens.drops.DropsScreen
import com.kaminova.novalogistikmobile.ui.screens.drops.DropsViewModel
import com.kaminova.novalogistikmobile.ui.screens.expenses.ExpensesScreen
import com.kaminova.novalogistikmobile.ui.screens.expenses.ExpensesViewModel
import com.kaminova.novalogistikmobile.ui.screens.history.HistoryViewModel
import com.kaminova.novalogistikmobile.ui.screens.home.HomeViewModel
import com.kaminova.novalogistikmobile.ui.screens.login.LoginScreen
import com.kaminova.novalogistikmobile.ui.screens.login.LoginViewModel
import com.kaminova.novalogistikmobile.ui.screens.main.MainScreen
import com.kaminova.novalogistikmobile.ui.screens.profile.ProfileViewModel
import com.kaminova.novalogistikmobile.ui.theme.NovaLogistikMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sessionManager = SessionManager(applicationContext)
        val initialRoute = if (sessionManager.currentToken == null) NavRoute.Login.route else NavRoute.Main.route

        setContent {
            val usePhoneColors by sessionManager.usePhoneColors.collectAsStateWithLifecycle()
            NovaLogistikMobileTheme(usePhoneColors = usePhoneColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(sessionManager = sessionManager, initialRoute = initialRoute)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppNavigation(sessionManager: SessionManager, initialRoute: String) {
    val navController = rememberNavController()

    // Create ApiService with 401 callback that returns to login
    val apiService = remember {
        NetworkModule.createApiService(
            sessionManager = sessionManager,
            onUnauthorized = {
                navController.navigate(NavRoute.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        )
    }

    val authRepository = remember { AuthRepository(apiService, sessionManager) }
    val driverRepository = remember { DriverRepository(apiService) }
    val expenseRepository = remember { ExpenseRepository(apiService) }

    val motion = MaterialTheme.motionScheme
    NavHost(
        navController = navController,
        startDestination = initialRoute,
        enterTransition = { fadeIn(motion.defaultEffectsSpec()) },
        exitTransition = { fadeOut(motion.fastEffectsSpec()) }
    ) {
        // Login Screen
        composable(NavRoute.Login.route) {
            val loginViewModel = remember { LoginViewModel(authRepository) }
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(NavRoute.Main.route) {
                        popUpTo(NavRoute.Login.route) { inclusive = true }
                    }
                }
            )
        }

        // Main Dashboard Screen with tabs (Home, History, Profile)
        composable(NavRoute.Main.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val homeViewModel = remember { HomeViewModel(driverRepository, sessionManager, context) }
            val historyViewModel = remember { HistoryViewModel(driverRepository) }
            val profileViewModel = remember { ProfileViewModel(authRepository, driverRepository, sessionManager) }

            MainScreen(
                homeViewModel = homeViewModel,
                historyViewModel = historyViewModel,
                profileViewModel = profileViewModel,
                onNavigateToDetail = { id ->
                    navController.navigate(NavRoute.TransportDetail.createRoute(id))
                },
                onLoggedOut = {
                    navController.navigate(NavRoute.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // Transport Detail Screen
        composable(
            route = NavRoute.TransportDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            val detailViewModel = viewModel(viewModelStoreOwner = backStackEntry) {
                TransportDetailViewModel(id, driverRepository, sessionManager)
            }

            TransportDetailScreen(
                viewModel = detailViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToExpenses = { transportId ->
                    navController.navigate(NavRoute.Expenses.createRoute(transportId))
                },
                onNavigateToDrops = { transportId ->
                    navController.navigate(NavRoute.Drops.createRoute(transportId))
                },
                onNavigateToCapture = {
                    navController.navigate(NavRoute.Capture.route)
                }
            )
        }

        // Operational Expenses Screen
        composable(
            route = NavRoute.Expenses.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            val expensesViewModel = viewModel(viewModelStoreOwner = backStackEntry) {
                ExpensesViewModel(id, expenseRepository, driverRepository, sessionManager)
            }

            ExpensesScreen(
                viewModel = expensesViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCapture = {
                    navController.navigate(NavRoute.Capture.route)
                }
            )
        }

        // Multi-Drop Board Screen
        composable(
            route = NavRoute.Drops.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("id") ?: return@composable
            val dropsViewModel = viewModel(viewModelStoreOwner = backStackEntry) {
                DropsViewModel(id, expenseRepository, driverRepository, sessionManager)
            }

            DropsScreen(
                viewModel = dropsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Full Screen Camera Evidence Capture
        composable(NavRoute.Capture.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val captureViewModel = remember {
                CaptureViewModel(sessionManager, context)
            }

            CaptureScreen(
                viewModel = captureViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
