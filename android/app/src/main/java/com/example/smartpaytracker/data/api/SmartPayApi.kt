package com.example.smartpaytracker.data.api

import retrofit2.http.*

interface SmartPayApi {

    @POST("signup")
    suspend fun signup(
        @Body request: UserCreateRequest
    ): UserResponse

    @POST("login")
    suspend fun login(
        @Body request: UserLoginRequest
    ): TokenResponse

    @GET("user/me")
    suspend fun getUserMe(
        @Header("Authorization") token: String
    ): UserResponse

    @POST("create-budget")
    suspend fun createBudget(
        @Header("Authorization") token: String,
        @Body request: BudgetCreateRequest
    ): BudgetResponse

    @GET("budget")
    suspend fun getBudget(
        @Header("Authorization") token: String,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): BudgetResponse

    @PUT("budget")
    suspend fun updateBudget(
        @Header("Authorization") token: String,
        @Query("budget_id") budgetId: Int,
        @Query("total_amount") totalAmount: Double
    ): BudgetResponse

    @POST("category")
    suspend fun createCategory(
        @Header("Authorization") token: String,
        @Body request: CategoryCreateRequest
    ): CategoryResponse

    @GET("categories")
    suspend fun getCategories(
        @Header("Authorization") token: String,
        @Query("budget_id") budgetId: Int
    ): List<CategoryResponse>

    @PUT("category/{id}")
    suspend fun updateCategory(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: CategoryUpdateRequest
    ): CategoryResponse

    @DELETE("category/{id}")
    suspend fun deleteCategory(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Map<String, String>

    @POST("expense")
    suspend fun createExpense(
        @Header("Authorization") token: String,
        @Body request: ExpenseCreateRequest
    ): ExpenseResponse

    @GET("expenses")
    suspend fun getExpenses(
        @Header("Authorization") token: String,
        @Query("budget_id") budgetId: Int
    ): List<ExpenseResponse>

    @DELETE("expense/{id}")
    suspend fun deleteExpense(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Map<String, String>

    @GET("reports/monthly")
    suspend fun getMonthlyReport(
        @Header("Authorization") token: String,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null
    ): MonthlyReportResponse

    @POST("ai/classify")
    suspend fun getAICategorization(
        @Header("Authorization") token: String,
        @Body request: AICategorizationRequest
    ): AICategorizationResponse

    @GET("ai/insights")
    suspend fun getAIInsights(
        @Header("Authorization") token: String,
        @Query("budget_id") budgetId: Int
    ): AIInsightsResponse
}
