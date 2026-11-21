package com.kaminova.novalogistikmobile.ui.screens.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.ShortNavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.kaminova.novalogistikmobile.ui.navigation.MainTab
import com.kaminova.novalogistikmobile.ui.screens.history.HistoryScreen
import com.kaminova.novalogistikmobile.ui.screens.history.HistoryViewModel
import com.kaminova.novalogistikmobile.ui.screens.home.HomeScreen
import com.kaminova.novalogistikmobile.ui.screens.home.HomeViewModel
import com.kaminova.novalogistikmobile.ui.screens.profile.ProfileScreen
import com.kaminova.novalogistikmobile.ui.screens.profile.ProfileViewModel

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    historyViewModel: HistoryViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onLoggedOut: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }

    Scaffold(
        bottomBar = {
            ShortNavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                val items = listOf(
                    Triple(MainTab.HOME, "Beranda", Icons.Default.LocalShipping),
                    Triple(MainTab.HISTORY, "Riwayat", Icons.Default.History),
                    Triple(MainTab.PROFILE, "Profil", Icons.Default.AccountCircle)
                )
                items.forEach { (tab, label, icon) ->
                    ShortNavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label) },
                        colors = ShortNavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedIndicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Crossfade(
            targetState = selectedTab,
            animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
            label = "Tab navigation",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToHistory = { selectedTab = MainTab.HISTORY },
                    onNavigateToProfile = { selectedTab = MainTab.PROFILE }
                )
                MainTab.HISTORY -> HistoryScreen(
                    viewModel = historyViewModel,
                    onNavigateToDetail = onNavigateToDetail
                )
                MainTab.PROFILE -> ProfileScreen(
                    viewModel = profileViewModel,
                    onLoggedOut = onLoggedOut
                )
            }
        }
    }
}
