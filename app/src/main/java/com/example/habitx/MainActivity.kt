package com.example.habitx

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.habitx.ui.AuthScreen
import com.example.habitx.ui.DashboardScreen
import com.example.habitx.ui.HabitViewModel
import com.example.habitx.ui.HabitViewModelFactory
import com.example.habitx.ui.theme.HabitXTheme

class MainActivity : ComponentActivity() {
    private val viewModel: HabitViewModel by viewModels {
        HabitViewModelFactory(application, (application as HabitApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        handleIntent(intent)

        setContent {
            HabitXTheme {
                val navController = rememberNavController()
                val loggedInUser by viewModel.loggedInUser.collectAsState()

                NavHost(
                    navController = navController,
                    startDestination = if (loggedInUser != null) "dashboard" else "auth"
                ) {
                    composable("auth") {
                        AuthScreen(
                            viewModel = viewModel,
                            onAuthSuccess = {
                                navController.navigate("dashboard") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("dashboard") {
                        DashboardScreen(
                            viewModel = viewModel,
                            onSignOut = {
                                viewModel.signOut()
                                navController.navigate("auth") {
                                    popUpTo("dashboard") { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Requirement: refreshPermissionState onResume
        viewModel.refreshPermissionState()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.getStringExtra("habitId")?.let { habitId ->
            viewModel.selectHabit(habitId)
        }
    }
}
