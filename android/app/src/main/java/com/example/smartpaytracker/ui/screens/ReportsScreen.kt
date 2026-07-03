package com.example.smartpaytracker.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpaytracker.data.api.ApiClient
import com.example.smartpaytracker.data.api.CategoryReport
import com.example.smartpaytracker.data.api.MonthlyReportResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    budgetId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val token = ApiClient.getSavedToken(context) ?: ""
    val coroutineScope = rememberCoroutineScope()

    var reportData by remember { mutableStateOf<MonthlyReportResponse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
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

    LaunchedEffect(key1 = true) {
        isLoading = true
        coroutineScope.launch {
            try {
                val api = ApiClient.getApi(context)
                
                val calendar = Calendar.getInstance()
                val month = calendar.get(Calendar.MONTH) + 1
                val year = calendar.get(Calendar.YEAR)

                val res = withContext(Dispatchers.IO) {
                    api.getMonthlyReport(token, month, year)
                }
                reportData = res
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMsg = e.message ?: "Failed to load report."
            }
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E2E), Color(0xFF0F0F16))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$currentMonthName $currentYear Report", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color(0xFF00B4D8),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (reportData != null) {
                val data = reportData!!
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Donut Chart Header
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Spending Distribution", color = Color.LightGray, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Box(
                                    modifier = Modifier.size(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DonutChart(
                                        categories = data.categories,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    // Total Spent text in center
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Spent", color = Color.Gray, fontSize = 12.sp)
                                        Text(
                                            text = "₹${data.total_spent.toInt()}",
                                            color = Color.White,
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "of ₹${data.total_budget.toInt()}",
                                            color = Color.LightGray,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Remaining: ₹${data.total_remaining.toInt()}", color = Color(0xFF6BCB77), fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                    Text("Budget Used: ${(if (data.total_budget > 0) (data.total_spent / data.total_budget * 100).toInt() else 0)}%", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    // Category List Headers
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Category Breakdown", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Share (%)", color = Color.LightGray, fontSize = 14.sp)
                        }
                    }

                    // Categories detailed rows
                    if (data.categories.isEmpty() || data.total_spent == 0.0) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0x1AFFFFFF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No expenses recorded yet.", color = Color.Gray)
                            }
                        }
                    } else {
                        items(data.categories.filter { it.spent_amount > 0 }) { category ->
                            CategoryReportRow(category = category)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    categories: List<CategoryReport>,
    modifier: Modifier = Modifier
) {
    val totalSpent = categories.sumOf { it.spent_amount }
    
    if (totalSpent <= 0) {
        Canvas(modifier = modifier) {
            drawCircle(
                color = Color.DarkGray,
                style = Stroke(width = 30.dp.toPx())
            )
        }
        return
    }

    Canvas(modifier = modifier) {
        var startAngle = 270f
        val strokeWidth = 24.dp.toPx()
        
        // Filter out empty spent categories
        val activeCategories = categories.filter { it.spent_amount > 0 }
        
        activeCategories.forEach { report ->
            val sweepAngle = (report.spent_amount / totalSpent * 360).toFloat()
            val color = parseColor(report.color)
            
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = androidx.compose.ui.geometry.Size(
                    size.width - strokeWidth,
                    size.height - strokeWidth
                ),
                topLeft = androidx.compose.ui.geometry.Offset(
                    strokeWidth / 2,
                    strokeWidth / 2
                )
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryReportRow(category: CategoryReport) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Color Bullet indicator
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(parseColor(category.color))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = category.category_name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Spent ₹${category.spent_amount.toInt()} of ₹${category.allocated_amount.toInt()}",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            
            Text(
                text = "${category.percentage.toInt()}%",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
