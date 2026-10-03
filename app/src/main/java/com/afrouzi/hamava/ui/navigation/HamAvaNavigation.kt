/*
 * Copyright (c) 2026 Mostafa Afrouzi
 * Licensed under the Apache License 2.0
 * https://github.com/mostafaafrouzi/HamAva
 */

package com.afrouzi.hamava.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.afrouzi.hamava.ui.screens.about.AboutScreen
import com.afrouzi.hamava.ui.screens.home.HomeScreen
import com.afrouzi.hamava.ui.screens.home.HomeViewModel
import com.afrouzi.hamava.ui.screens.settings.SettingsScreen
import com.afrouzi.hamava.ui.screens.settings.SettingsViewModel

object HamAvaDestinations {
    const val HOME_ROUTE = "home"
    const val SETTINGS_ROUTE = "settings"
    const val ABOUT_ROUTE = "about"
}

@Composable
fun HamAvaNavHost(
    onRequestMediaProjection: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = HamAvaDestinations.HOME_ROUTE
    ) {
        composable(HamAvaDestinations.HOME_ROUTE) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToSettings = { navController.navigate(HamAvaDestinations.SETTINGS_ROUTE) },
                onNavigateToAbout = { navController.navigate(HamAvaDestinations.ABOUT_ROUTE) },
                onRequestMediaProjection = onRequestMediaProjection
            )
        }

        composable(HamAvaDestinations.SETTINGS_ROUTE) {
            val settingsViewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(HamAvaDestinations.ABOUT_ROUTE) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
