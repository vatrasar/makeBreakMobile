package com.example.makebreak.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Global bottom navigation bar providing primary tabs between Timer and Settings.
 *
 * Usage:
 * navController: manages top-level tab navigation and current destination observation.
 * modifier: optional layout modifier.
 *
 * Key UI elements:
 * NavigationBar container, Timer tab item, Settings tab item.
 *
 * Used In:
 * MainActivity
 */
@Composable
fun AppBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val items = listOf(Screen.Timer, Screen.Settings)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar(
        modifier = modifier.testTag("appBottomBar")
    ) {
        items.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any {
                it.route?.contains(screen.route::class.simpleName ?: "") == true
            } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = stringResource(id = screen.titleRes)
                    )
                },
                label = { Text(stringResource(id = screen.titleRes)) },
                modifier = Modifier.testTag(screen.testTag)
            )
        }
    }
}
