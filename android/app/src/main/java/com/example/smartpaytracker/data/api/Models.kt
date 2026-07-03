package com.example.smartpaytracker.data.api

import kotlinx.serialization.Serializable

@Serializable
data class UserCreateRequest(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class UserLoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class UserResponse(
    val id: Int,
    val name: String,
    val email: String,
    val created_at: String
)

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String
)

@Serializable
data class BudgetCreateRequest(
    val total_amount: Double,
    val month: Int,
    val year: Int
)

@Serializable
data class BudgetResponse(
    val id: Int,
    val user_id: Int,
    val total_amount: Double,
    val month: Int,
    val year: Int,
    val created_at: String
)

@Serializable
data class CategoryCreateRequest(
    val budget_id: Int,
    val category_name: String,
    val allocated_amount: Double,
    val color: String,
    val icon: String
)

@Serializable
data class CategoryUpdateRequest(
    val category_name: String? = null,
    val allocated_amount: Double? = null,
    val color: String? = null,
    val icon: String? = null
)

@Serializable
data class CategoryResponse(
    val id: Int,
    val budget_id: Int,
    val category_name: String,
    val allocated_amount: Double,
    val remaining_amount: Double,
    val color: String,
    val icon: String
)

@Serializable
data class ExpenseCreateRequest(
    val category_id: Int,
    val amount: Double,
    val merchant: String,
    val date: String, // YYYY-MM-DD
    val remarks: String? = null
)

@Serializable
data class ExpenseResponse(
    val id: Int,
    val category_id: Int,
    val amount: Double,
    val merchant: String,
    val date: String,
    val remarks: String?
)

@Serializable
data class AICategorizationRequest(
    val merchant: String
)

@Serializable
data class AICategorizationResponse(
    val category_name: String,
    val confidence: Double
)

@Serializable
data class AIInsightsResponse(
    val insights: List<String>
)

@Serializable
data class CategoryReport(
    val category_id: Int,
    val category_name: String,
    val allocated_amount: Double,
    val spent_amount: Double,
    val remaining_amount: Double,
    val color: String,
    val percentage: Double
)

@Serializable
data class MonthlyReportResponse(
    val total_budget: Double,
    val total_spent: Double,
    val total_remaining: Double,
    val categories: List<CategoryReport>
)
