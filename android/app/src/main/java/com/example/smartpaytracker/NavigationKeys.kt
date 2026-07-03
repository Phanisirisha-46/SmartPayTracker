package com.example.smartpaytracker

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Splash : NavKey
@Serializable data object Login : NavKey
@Serializable data object Signup : NavKey
@Serializable data object Main : NavKey
@Serializable data class AddCategory(val budgetId: Int) : NavKey
@Serializable data class CategoryDetails(
    val categoryId: Int,
    val categoryName: String,
    val allocated: Double,
    val remaining: Double
) : NavKey
@Serializable data class PaymentScreen(val budgetId: Int) : NavKey
@Serializable data class Reports(val budgetId: Int) : NavKey
