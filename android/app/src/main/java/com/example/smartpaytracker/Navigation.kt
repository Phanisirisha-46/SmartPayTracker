package com.example.smartpaytracker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.smartpaytracker.ui.screens.*

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Splash)

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider {
            entry<Splash> {
                SplashScreen(
                    onNavigate = { navKey ->
                        backStack.add(navKey)
                        backStack.remove(Splash)
                    }
                )
            }
            entry<Login> {
                LoginScreen(
                    onNavigateToSignup = { backStack.add(Signup) },
                    onLoginSuccess = {
                        backStack.add(Main)
                        backStack.remove(Login)
                    }
                )
            }
            entry<Signup> {
                SignupScreen(
                    onNavigateToLogin = {
                        backStack.removeLastOrNull()
                    },
                    onSignupSuccess = {
                        backStack.add(Main)
                        backStack.remove(Signup)
                    }
                )
            }
            entry<Main> {
                DashboardScreen(
                    onNavigate = { navKey -> backStack.add(navKey) },
                    onLogout = {
                        backStack.add(Login)
                        backStack.remove(Main)
                    }
                )
            }
            entry<AddCategory> { key ->
                AddCategoryScreen(
                    budgetId = key.budgetId,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<CategoryDetails> { key ->
                CategoryDetailsScreen(
                    categoryId = key.categoryId,
                    categoryName = key.categoryName,
                    allocated = key.allocated,
                    remaining = key.remaining,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<PaymentScreen> { key ->
                PaymentScreen(
                    budgetId = key.budgetId,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
            entry<Reports> { key ->
                ReportsScreen(
                    budgetId = key.budgetId,
                    onBack = { backStack.removeLastOrNull() }
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
