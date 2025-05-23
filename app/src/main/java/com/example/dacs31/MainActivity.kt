package com.example.dacs31

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.ui.screen.ProfileScreen
import com.example.dacs31.ui.screen.SignInScreen
import com.example.dacs31.ui.screen.SignUpScreen
import com.example.dacs31.ui.screen.customer.CustomerHomeScreen
import com.example.dacs31.ui.screen.driver.DriverHomeScreen
import com.example.dacs31.ui.screen.history.HistoryScreen
import com.example.dacs31.ui.screen.ProfileScreen
import com.example.dacs31.ui.screen.wallet.WalletScreen
import com.example.dacs31.ui.theme.DACS31Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint // Đảm bảo activity được quản lý bởi Hilt
class MainActivity : ComponentActivity() {

    @Inject // Inject AuthRepository
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DACS31Theme {
                AppNavigation(authRepository = authRepository) // Truyền authRepository đã inject
            }
        }
    }
}

@Composable
fun AppNavigation(authRepository: AuthRepository) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()
    var startDestination by remember { mutableStateOf("signin") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val currentUser = authRepository.getCurrentUser()
            startDestination = if (currentUser != null) {
                try {
                    val role = authRepository.getUserRole() ?: "unknown"
                    when (role) {
                        "Driver" -> "driver_home"
                        "Customer" -> "customer_home"
                        else -> "signin"
                    }
                } catch (e: Exception) {
                    "signin"
                }
            } else {
                "signin"
            }

            Log.d("Role", startDestination)

            navController.navigate(startDestination) {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("signin") {
            SignInScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("signup") {
            SignUpScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("customer_home") {
            CustomerHomeScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("driver_home") {
            DriverHomeScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("profile") {
            ProfileScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
        composable("history") {
            HistoryScreen(navController = navController, authRepository = authRepository)
        }
        composable("wallet") {
            WalletScreen(navController = navController)
        }
        composable("home") {
            CustomerHomeScreen(
                navController = navController,
                authRepository = authRepository
            )
        }
    }
}