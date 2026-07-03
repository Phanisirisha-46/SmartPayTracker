package com.example.smartpaytracker.ui.screens

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.smartpaytracker.*
import com.example.smartpaytracker.data.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigate: (NavKey) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val token = ApiClient.getSavedToken(context) ?: ""
    val coroutineScope = rememberCoroutineScope()
    
    // States
    var budget by remember { mutableStateOf<BudgetResponse?>(null) }
    var categories by remember { mutableStateOf<List<CategoryResponse>>(emptyList()) }
    var insights by remember { mutableStateOf<List<String>>(emptyList()) }
    
    var showBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var isBudgetError by remember { mutableStateOf(false) } // 404 means no budget created yet
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val currentMonthName = remember {
        val calendar = Calendar.getInstance()
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        months[calendar.get(Calendar.MONTH)]
    }
    
    val currentYear = remember {
        Calendar.getInstance().get(Calendar.YEAR)
    }

    // Load dashboard data function
    fun loadDashboardData() {
        if (token.isBlank()) return
        isLoading = true
        errorMsg = null
        
        coroutineScope.launch {
            try {
                val api = ApiClient.getApi(context)
                
                // Fetch current monthly budget
                val calendar = Calendar.getInstance()
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)
                
                val budgetRes = withContext(Dispatchers.IO) {
                    try {
                        api.getBudget(token, month, year)
                    } catch (e: retrofit2.HttpException) {
                        if (e.code() == 404) {
                            isBudgetError = true
                            null
                        } else {
                            throw e
                        }
                    }
                }
                
                budget = budgetRes
                
                if (budgetRes != null) {
                    isBudgetError = false
                    // Fetch categories
                    val catRes = withContext(Dispatchers.IO) {
                        api.getCategories(token, budgetRes.id)
                    }
                    categories = catRes
                    
                    // Fetch AI insights
                    val insightRes = withContext(Dispatchers.IO) {
                        api.getAIInsights(token, budgetRes.id)
                    }
                    insights = insightRes.insights
                }
                
                isLoading = false
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMsg = e.message ?: "Failed to fetch dashboard data."
                }
            }
        }
    }

    // Load data on start
    LaunchedEffect(key1 = true) {
        loadDashboardData()
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E2E), Color(0xFF0F0F16))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SmartPay Tracker", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "Hello, ${ApiClient.getUserName(context)}",
                            fontSize = 13.sp,
                            color = Color.LightGray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadDashboardData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                    IconButton(onClick = {
                        ApiClient.clearSession(context)
                        onLogout()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E2E))
            )
        },
        containerColor = Color(0xFF0F0F16)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
        ) {
            if (budget == null && !isBudgetError) {
                CircularProgressIndicator(
                    color = Color(0xFF00B4D8),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (isBudgetError) {
                // Prompt to setup budget
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Budget Set",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To start tracking, please set your monthly income budget for $currentMonthName $currentYear.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text("Income Budget (₹)", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Color(0xFF00B4D8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B4D8),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            val amt = budgetInput.toDoubleOrNull()
                            if (amt == null || amt <= 0) {
                                errorMsg = "Please enter a valid amount"
                                return@Button
                            }
                            isLoading = true
                            coroutineScope.launch {
                                try {
                                    val calendar = Calendar.getInstance()
                                    val month = calendar.get(Calendar.MONTH) + 1
                                    val year = calendar.get(Calendar.YEAR)
                                    
                                    ApiClient.getApi(context).createBudget(
                                        token,
                                        BudgetCreateRequest(amt, month, year)
                                    )
                                    isBudgetError = false
                                    loadDashboardData()
                                } catch (e: Exception) {
                                    errorMsg = e.message ?: "Failed to set budget."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                        modifier = Modifier.fillMaxWidth(0.8f)
                    ) {
                        Text("Create Budget", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    if (errorMsg != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                // Budget is loaded, show actual Dashboard
                val activeBudget = budget!!
                val totalSpent = categories.sumOf { it.allocated_amount - it.remaining_amount }
                val totalRemaining = activeBudget.total_amount - totalSpent
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header spacing
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Notification alerts at 80% and 100% budget limit checks
                    val warningNotifications = getWarningNotifications(categories)
                    if (warningNotifications.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x33FF9F1C))
                                    .border(1.dp, Color(0xFFFF9F1C), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color(0xFFFF9F1C))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Budget Alerts", color = Color(0xFFFF9F1C), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                warningNotifications.forEach { alert ->
                                    Text("• $alert", color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }

                    // Main Budget Card
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                                Column(modifier = Modifier.align(Alignment.TopStart)) {
                                    Text(text = "Remaining Balance", color = Color.LightGray, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "₹${totalRemaining.toInt()}",
                                        color = Color.White,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Budget", color = Color.Gray, fontSize = 12.sp)
                                        Text("₹${activeBudget.total_amount.toInt()}", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Spent This Month", color = Color.Gray, fontSize = 12.sp)
                                        Text("₹${totalSpent.toInt()}", color = Color(0xFFF25C54), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                                
                                IconButton(
                                    onClick = {
                                        budgetInput = activeBudget.total_amount.toInt().toString()
                                        showBudgetDialog = true
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Budget", tint = Color.LightGray)
                                }
                            }
                        }
                    }

                    // Action Buttons Quick Menu
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Pay Button
                            Button(
                                onClick = { onNavigate(PaymentScreen(activeBudget.id)) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(55.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Pay (UPI)", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            
                            // Reports Button
                            Button(
                                onClick = { onNavigate(Reports(activeBudget.id)) },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A4E69)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(55.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.PieChart, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reports", fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }

                    // AI Spending Insights Section
                    if (insights.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1E1E2E))
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Psychology,
                                        contentDescription = null,
                                        tint = Color(0xFF00B4D8),
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "AI Spending Insights",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                insights.forEach { insight ->
                                    Text(
                                        text = insight,
                                        color = Color.LightGray,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Categories Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Categories", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { onNavigate(AddCategory(activeBudget.id)) }) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF00B4D8))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Category", color = Color(0xFF00B4D8))
                            }
                        }
                    }

                    // Empty category checker
                    if (categories.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x1AFFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No categories added yet.", color = Color.Gray)
                            }
                        }
                    }

                    // Categories Items list
                    items(categories) { category ->
                        CategoryListItem(category = category, onClick = {
                            onNavigate(
                                CategoryDetails(
                                    categoryId = category.id,
                                    categoryName = category.category_name,
                                    allocated = category.allocated_amount,
                                    remaining = category.remaining_amount
                                )
                            )
                        })
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Edit Budget Dialog
    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Update Income Budget") },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("Income Amount (₹)") },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00B4D8)
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = budgetInput.toDoubleOrNull()
                        if (amt == null || amt <= 0) return@Button
                        
                        showBudgetDialog = false
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val api = ApiClient.getApi(context)
                                api.updateBudget(token, budget!!.id, amt)
                                loadDashboardData()
                            } catch (e: Exception) {
                                errorMsg = e.message ?: "Failed to update budget."
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                ) {
                    Text("Update", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategoryListItem(category: CategoryResponse, onClick: () -> Unit) {
    val spent = category.allocated_amount - category.remaining_amount
    val progress = if (category.allocated_amount > 0) (spent / category.allocated_amount).toFloat() else 0f
    
    // Choose progress color depending on limits
    val progressColor = when {
        progress >= 1.0f -> Color(0xFFF25C54) // Critical Red
        progress >= 0.8f -> Color(0xFFF4A261) // Warning Orange
        else -> parseColor(category.color)   // User Defined Color
    }

    val icon = getCategoryIcon(category.icon)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon container
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(CircleShape)
                    .background(parseColor(category.color).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = parseColor(category.color),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Name and progress bar
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.category_name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "₹${category.remaining_amount.toInt()} left",
                        color = if (category.remaining_amount <= 0) Color(0xFFF25C54) else Color.LightGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress Bar
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = progressColor,
                    trackColor = Color(0x33FFFFFF)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Spent: ₹${spent.toInt()} of ₹${category.allocated_amount.toInt()}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

// Helpers
fun getCategoryIcon(iconName: String): ImageVector {
    return when (iconName.lowercase()) {
        "food" -> Icons.Default.Restaurant
        "home" -> Icons.Default.Home
        "hospital" -> Icons.Default.LocalHospital
        "family" -> Icons.Default.People
        "shopping" -> Icons.Default.ShoppingBag
        "fuel" -> Icons.Default.LocalGasStation
        else -> Icons.Default.Category
    }
}

fun parseColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Color.Gray
    }
}

fun getWarningNotifications(categories: List<CategoryResponse>): List<String> {
    val alerts = mutableListOf<String>()
    categories.forEach { cat ->
        val spent = cat.allocated_amount - cat.remaining_amount
        val spentPct = if (cat.allocated_amount > 0) (spent / cat.allocated_amount) * 100 else 0.0
        
        if (spentPct >= 100) {
            alerts.add("${cat.category_name} budget completed (100% spent).")
        } else if (spentPct >= 80) {
            alerts.add("${cat.category_name} budget is almost exhausted (${spentPct.toInt()}% spent).")
        }
    }
    return alerts
}
