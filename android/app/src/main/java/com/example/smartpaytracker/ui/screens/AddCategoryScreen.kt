package com.example.smartpaytracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpaytracker.data.api.ApiClient
import com.example.smartpaytracker.data.api.CategoryCreateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryScreen(
    budgetId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val token = ApiClient.getSavedToken(context) ?: ""
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var allocatedInput by remember { mutableStateOf("") }
    
    // Premium color options
    val colors = listOf(
        "#00B4D8", // Cyan
        "#FF6B6B", // Red
        "#4D96FF", // Blue
        "#6BCB77", // Green
        "#FFD93D", // Yellow
        "#FF8E3C"  // Orange
    )
    var selectedColor by remember { mutableStateOf(colors[0]) }

    // Icon options
    val icons = listOf(
        Pair("food", Icons.Default.Restaurant),
        Pair("home", Icons.Default.Home),
        Pair("hospital", Icons.Default.LocalHospital),
        Pair("family", Icons.Default.People),
        Pair("shopping", Icons.Default.ShoppingBag),
        Pair("fuel", Icons.Default.LocalGasStation)
    )
    var selectedIcon by remember { mutableStateOf(icons[0].first) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E2E), Color(0xFF0F0F16))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Category", color = Color.White, fontWeight = FontWeight.Bold) },
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
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFF00B4D8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00B4D8),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Allocated Amount Field
                OutlinedTextField(
                    value = allocatedInput,
                    onValueChange = { allocatedInput = it },
                    label = { Text("Allocated Amount (₹)", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, contentDescription = null, tint = Color(0xFF00B4D8)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00B4D8),
                        unfocusedBorderColor = Color.DarkGray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Color Picker Label
                Text("Select Theme Color", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                // Colors grid row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex }
                        )
                    }
                }

                // Icon Picker Label
                Text("Select Category Icon", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                // Icons selection row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    icons.forEach { (name, imageVector) ->
                        val isSelected = selectedIcon == name
                        Box(
                            modifier = Modifier
                                .size(45.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) Color(0xFF00B4D8).copy(alpha = 0.2f)
                                    else Color(0xFF1E1E2E)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color(0xFF00B4D8) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedIcon = name },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = imageVector,
                                contentDescription = name,
                                tint = if (isSelected) Color(0xFF00B4D8) else Color.LightGray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Button(
                    onClick = {
                        val allocated = allocatedInput.toDoubleOrNull()
                        if (name.isBlank() || allocated == null || allocated <= 0) {
                            errorMsg = "Please enter a valid name and amount."
                            return@Button
                        }
                        
                        isLoading = true
                        errorMsg = null

                        coroutineScope.launch {
                            try {
                                val api = ApiClient.getApi(context)
                                withContext(Dispatchers.IO) {
                                    api.createCategory(
                                        token,
                                        CategoryCreateRequest(
                                            budget_id = budgetId,
                                            category_name = name.trim(),
                                            allocated_amount = allocated,
                                            color = selectedColor,
                                            icon = selectedIcon
                                        )
                                    )
                                }
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    onBack()
                                }
                            } catch (e: retrofit2.HttpException) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    errorMsg = try {
                                        // Retrieve error detail from FastAPI
                                        val errorBody = e.response()?.errorBody()?.string()
                                        if (errorBody != null && errorBody.contains("detail")) {
                                            // Quick regex parse of json detail message
                                            val regex = "\"detail\":\"([^\"]+)\"".toRegex()
                                            regex.find(errorBody)?.groupValues?.get(1) ?: "Failed to add category."
                                        } else {
                                            "Total budget limit exceeded."
                                        }
                                    } catch (ex: Exception) {
                                        "Budget limit exceeded."
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    errorMsg = e.message ?: "An unexpected error occurred."
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Add Category", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
