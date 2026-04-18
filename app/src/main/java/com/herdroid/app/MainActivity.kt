package com.herdroid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.herdroid.app.ui.main.MainScreen
import com.herdroid.app.ui.main.MainViewModel
import com.herdroid.app.ui.main.MainViewModelFactory
import com.herdroid.app.ui.settings.SettingsScreen
import com.herdroid.app.ui.settings.SettingsViewModel
import com.herdroid.app.ui.theme.HerdroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as HerdroidApplication
        setContent {
            HerdroidTheme {
                val navController = rememberNavController()
                val mainVm: MainViewModel = viewModel(factory = MainViewModelFactory(app))
                val settingsVm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_MAIN,
                ) {
                    composable(ROUTE_MAIN) {
                        MainScreen(
                            viewModel = mainVm,
                            onOpenSettings = { navController.navigate(ROUTE_SETTINGS) },
                        )
                    }
                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            repository = app.settingsRepository,
                            viewModel = settingsVm,
                            onBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }

    companion object {
        private const val ROUTE_MAIN = "main"
        private const val ROUTE_SETTINGS = "settings"
    }
}
