package com.zephyrus.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.zephyrus.app.ui.about.AboutScreen
import com.zephyrus.app.ui.current.CurrentScreen
import com.zephyrus.app.ui.forecast.ForecastScreen
import com.zephyrus.app.ui.maps.MapsScreen
import com.zephyrus.app.ui.search.SearchScreen
import com.zephyrus.app.ui.settings.SettingsScreen
import timber.log.Timber

private const val SEARCH_ROUTE = "search"
private const val SETTINGS_ROUTE = "settings"
private const val ABOUT_ROUTE = "about"

@Composable
fun ZephyrusNavHost(
    modifier: Modifier = Modifier,
    navigationViewModel: NavigationViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val screens = ZephyrusScreen.entries

    // Shared location state across tabs
    var activeLatitude by rememberSaveable { mutableDoubleStateOf(0.0) }
    var activeLongitude by rememberSaveable { mutableDoubleStateOf(0.0) }
    var activeLocationName by rememberSaveable { mutableStateOf("Zephyrus") }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val modalRoutes = setOf(SEARCH_ROUTE, SETTINGS_ROUTE, ABOUT_ROUTE)
    val showBottomBar = currentRoute !in modalRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    val currentDestination = navBackStackEntry?.destination

                    screens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                Timber.d("Navigating to %s", screen.route)
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
        modifier = modifier,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ZephyrusScreen.Current.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(ZephyrusScreen.Current.route) {
                CurrentScreen(
                    latitude = activeLatitude,
                    longitude = activeLongitude,
                    locationName = activeLocationName,
                    onSearchClick = {
                        navController.navigate(SEARCH_ROUTE)
                    },
                    onSettingsClick = {
                        navController.navigate(SETTINGS_ROUTE)
                    },
                    onAboutClick = {
                        navController.navigate(ABOUT_ROUTE)
                    },
                    onLocationResolved = { lat, lon, name ->
                        activeLatitude = lat
                        activeLongitude = lon
                        activeLocationName = name
                    },
                )
            }
            composable(ZephyrusScreen.Forecast.route) {
                ForecastScreen(
                    latitude = activeLatitude,
                    longitude = activeLongitude,
                    locationName = activeLocationName,
                    onSearchClick = { navController.navigate(SEARCH_ROUTE) },
                    onSettingsClick = { navController.navigate(SETTINGS_ROUTE) },
                    onAboutClick = { navController.navigate(ABOUT_ROUTE) },
                )
            }
            composable(ZephyrusScreen.Maps.route) {
                MapsScreen(
                    locationName = activeLocationName,
                    latitude = activeLatitude,
                    longitude = activeLongitude,
                    onSearchClick = { navController.navigate(SEARCH_ROUTE) },
                    onSettingsClick = { navController.navigate(SETTINGS_ROUTE) },
                    onAboutClick = { navController.navigate(ABOUT_ROUTE) },
                )
            }
            composable(SEARCH_ROUTE) {
                SearchScreen(
                    onLocationSelected = { location ->
                        activeLatitude = location.latitude
                        activeLongitude = location.longitude
                        activeLocationName = if (location.admin1.isNotEmpty()) {
                            "${location.name}, ${location.admin1}"
                        } else {
                            location.name
                        }
                        navController.popBackStack()
                    },
                    onCurrentLocationSelected = {
                        // Resolve GPS and update shared state — stays on current screen
                        navigationViewModel.resolveDeviceLocation(
                            onResolved = { location ->
                                activeLatitude = location.latitude
                                activeLongitude = location.longitude
                                activeLocationName = if (location.admin1.isNotEmpty()) {
                                    "${location.name}, ${location.admin1}"
                                } else {
                                    location.name
                                }
                            },
                        )
                        navController.popBackStack()
                    },
                    onDismiss = { navController.popBackStack() },
                )
            }
            composable(SETTINGS_ROUTE) {
                SettingsScreen(
                    onDismiss = { navController.popBackStack() },
                )
            }
            composable(ABOUT_ROUTE) {
                AboutScreen(
                    onDismiss = { navController.popBackStack() },
                )
            }
        }
    }
}
