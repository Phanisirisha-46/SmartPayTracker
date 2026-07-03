package com.example.smartpaytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpaytracker.data.api.ApiClient
import com.example.smartpaytracker.data.api.ExpenseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailsScreen(
    categoryId: Int,
    categoryName: String,
    allocated: Double,
    remaining: Double,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val token = ApiClient.getSavedToken(context) ?: ""
    val coroutineScope = rememberCoroutineScope()

    var expensesList by remember { mutableStateOf<List<ExpenseResponse>>(emptyList()) }
    var currentRemaining by remember { mutableStateOf(remaining) }
    
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun loadCategoryExpenses() {
        if (token.isBlank()) return
        isLoading = true
        coroutineScope.launch {
            try {
                val api = ApiClient.getApi(context)
                
                // Fetch categories to update remaining balance dynamically
                val calendar = java.util.Calendar.getInstance()
                val month = calendar.get(java.util.Calendar.MONTH) + 1
                val year = calendar.get(java.util.Calendar.YEAR)
                
                val budgetRes = withContext(Dispatchers.IO) {
                    api.getBudget(token, month, year)
                }
                
                val catsRes = withContext(Dispatchers.IO) {
                    api.getCategories(token, budgetRes.id)
                }
                
                val matchingCat = catsRes.firstOrNull { it.id == categoryId }
                if (matchingCat != null) {
                    currentRemaining = matchingCat.remaining_amount
                }

                // Fetch expenses and filter
                val expRes = withContext(Dispatchers.IO) {
                    api.getExpenses(token, budgetRes.id)
                }
                expensesList = expRes.filter { it.category_id == categoryId }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMsg = e.message ?: "Failed to fetch transactions."
            }
        }
    }

    LaunchedEffect(key1 = true) {
        loadCategoryExpenses()
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E2E), Color(0xFF0F0F16))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(categoryName, color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Category", tint = Color(0xFFF25C54))
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
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Category Budget Summary Card
                val spent = allocated - currentRemaining
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Category Budget Info", color = Color.LightGray, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Allocated", color = Color.Gray, fontSize = 12.sp)
                                Text("₹${allocated.toInt()}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Spent", color = Color.Gray, fontSize = 12.sp)
                                Text("₹${spent.toInt()}", color = Color(0xFFF25C54), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text("Remaining", color = Color.Gray, fontSize = 12.sp)
                                Text(
                                    text = "₹${currentRemaining.toInt()}",
                                    color = if (currentRemaining <= 0) Color(0xFFF25C54) else Color(0xFF6BCB77),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Transactions Header
                Text(
                    text = "Transactions",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF00B4D8))
                    }
                } else if (expensesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x1AFFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No transactions in this category yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(expensesList) { expense ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = expense.merchant,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        if (!expense.remarks.isNullOrBlank()) {
                                            Text(
                                                text = expense.remarks,
                                                color = Color.LightGray,
                                                fontSize = 12.sp
                                            )
                                        }
                                        Text(
                                            text = expense.date,
                                            color = Color.Gray,
                                            fontSize = 11.sp
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₹${expense.amount.toInt()}",
                                            color = Color(0xFFF25C54),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        IconButton(onClick = {
                                            isLoading = true
                                            coroutineScope.launch {
                                                try {
                                                    val api = ApiClient.getApi(context)
                                                    withContext(Dispatchers.IO) {
                                                        api.deleteExpense(token, expense.id)
                                                    }
                                                    loadCategoryExpenses()
                                                } catch (e: Exception) {
                                                    errorMsg = e.message ?: "Failed to delete expense."
                                                    isLoading = false
                                                }
                                            }
                                        }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Delete transaction", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Category Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Category?") },
            text = { Text("Are you sure you want to delete '$categoryName'? All associated expenses will be deleted permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val api = ApiClient.getApi(context)
                                withContext(Dispatchers.IO) {
                                    api.deleteCategory(token, categoryId)
                                }
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    onBack()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    errorMsg = e.message ?: "Failed to delete category."
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF25C54))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
