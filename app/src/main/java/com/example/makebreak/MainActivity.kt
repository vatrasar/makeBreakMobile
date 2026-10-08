package com.example.makebreak

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.example.makebreak.common.theme.MakeBreakTheme
import com.example.makebreak.navigation.AppBottomBar
import com.example.makebreak.navigation.NavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        setContent {
            MakeBreakTheme {
                MainAppScaffold()
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }
}

/**
 * Root application scaffold containing the NavGraph host and persistent AppBottomBar.
 *
 * Usage:
 * Serves as the top-level container for all primary application navigation routes.
 *
 * Key UI elements:
 * Scaffold, NavGraph, AppBottomBar.
 *
 * Used In:
 * MainActivity
 */
@Composable
fun MainAppScaffold() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("mainAppScaffold"),
        bottomBar = {
            AppBottomBar(navController = navController)
        }
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
