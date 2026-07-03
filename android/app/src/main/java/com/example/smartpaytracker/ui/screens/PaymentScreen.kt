package com.example.smartpaytracker.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartpaytracker.data.api.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    budgetId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val token = ApiClient.getSavedToken(context) ?: ""
    val coroutineScope = rememberCoroutineScope()

    // Inputs
    var amountInput by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    var upiIdInput by remember { mutableStateOf("veera@ybl") }

    // Date
    val currentDateStr = remember {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Calendar.getInstance().time)
    }

    // Categories
    var categories by remember { mutableStateOf<List<CategoryResponse>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<CategoryResponse?>(null) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Flow states
    var isLoading by remember { mutableStateOf(false) }
    var apiErrorMsg by remember { mutableStateOf<String?>(null) }
    
    // Payment Allowed state
    var showPaymentSuccessDialog by remember { mutableStateOf(false) }
    var remainingAfterPayment by remember { mutableStateOf(0.0) }

    // AI Classification tags
    var aiClassifiedTag by remember { mutableStateOf<String?>(null) }

    // Fetch categories on start
    LaunchedEffect(key1 = true) {
        coroutineScope.launch {
            try {
                val api = ApiClient.getApi(context)
                val catRes = withContext(Dispatchers.IO) {
                    api.getCategories(token, budgetId)
                }
                categories = catRes
                if (catRes.isNotEmpty()) {
                    selectedCategory = catRes[0]
                }
            } catch (e: Exception) {
                apiErrorMsg = "Failed to load categories: ${e.message}"
            }
        }
    }

    // AI auto categorization when merchant name changes (local regex + debounced api call)
    LaunchedEffect(key1 = merchant) {
        if (merchant.isBlank()) {
            aiClassifiedTag = null
            return@LaunchedEffect
        }
        
        // 1. Run local matching first for instant feedback
        val localMatch = localClassify(merchant)
        if (localMatch != null) {
            aiClassifiedTag = localMatch
            val matchedCat = categories.firstOrNull { it.category_name.equals(localMatch, ignoreCase = true) }
            if (matchedCat != null) {
                selectedCategory = matchedCat
            }
        }

        // 2. Query backend AI endpoint
        coroutineScope.launch {
            try {
                val api = ApiClient.getApi(context)
                val aiRes = withContext(Dispatchers.IO) {
                    api.getAICategorization(token, AICategorizationRequest(merchant))
                }
                if (aiRes.confidence >= 0.8) {
                    aiClassifiedTag = aiRes.category_name
                    val matchedCat = categories.firstOrNull { it.category_name.equals(aiRes.category_name, ignoreCase = true) }
                    if (matchedCat != null) {
                        selectedCategory = matchedCat
                    }
                }
            } catch (e: Exception) {
                // Ignore API categorization errors silently
            }
        }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1E1E2E), Color(0xFF0F0F16))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment & Expense", color = Color.White, fontWeight = FontWeight.Bold) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amount Field
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Payment Amount (₹)", color = Color.LightGray) },
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

                // Merchant Field
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant / Pay To (e.g. Swiggy)", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF00B4D8)) },
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

                // UPI VPA Address Field
                OutlinedTextField(
                    value = upiIdInput,
                    onValueChange = { upiIdInput = it },
                    label = { Text("Destination UPI ID (VPA)", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, tint = Color(0xFF00B4D8)) },
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

                // AI Suggested Badge
                if (aiClassifiedTag != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = Color(0xFF6BCB77),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Suggested Category: $aiClassifiedTag",
                            color = Color(0xFF6BCB77),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedDropdown,
                    onExpandedChange = { expandedDropdown = !expandedDropdown }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedCategory?.category_name ?: "Select Category",
                        onValueChange = {},
                        label = { Text("Budget Category", color = Color.LightGray) },
                        leadingIcon = { Icon(Icons.Default.List, contentDescription = null, tint = Color(0xFF00B4D8)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00B4D8),
                            unfocusedBorderColor = Color.DarkGray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.category_name) },
                                onClick = {
                                    selectedCategory = category
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }

                // Remarks Field
                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)", color = Color.LightGray) },
                    leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFF00B4D8)) },
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

                // Date Label (Show current date)
                Text(
                    text = "Transaction Date: $currentDateStr",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (apiErrorMsg != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x33F25C54)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFF25C54))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Budget Exceeded", color = Color(0xFFF25C54), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = apiErrorMsg!!,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val amount = amountInput.toDoubleOrNull()
                        if (amount == null || amount <= 0 || merchant.isBlank() || selectedCategory == null) {
                            apiErrorMsg = "Please enter valid payment details."
                            return@Button
                        }
                        
                        isLoading = true
                        apiErrorMsg = null

                        coroutineScope.launch {
                            try {
                                val api = ApiClient.getApi(context)
                                
                                val response = withContext(Dispatchers.IO) {
                                    api.createExpense(
                                        token,
                                        ExpenseCreateRequest(
                                            category_id = selectedCategory!!.id,
                                            amount = amount,
                                            merchant = merchant.trim(),
                                            date = currentDateStr,
                                            remarks = remarks.trim().ifEmpty { null }
                                        )
                                    )
                                }
                                
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    // Fetch remaining balance dynamically
                                    remainingAfterPayment = selectedCategory!!.remaining_amount - amount
                                    showPaymentSuccessDialog = true
                                }
                            } catch (e: retrofit2.HttpException) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    // Extract error message from server
                                    val errorBody = e.response()?.errorBody()?.string()
                                    apiErrorMsg = if (errorBody != null && errorBody.contains("detail")) {
                                        val regex = "\"detail\":\"([^\"]+)\"".toRegex()
                                        regex.find(errorBody)?.groupValues?.get(1) ?: "Budget limit exceeded."
                                    } else {
                                        "Budget Exceeded. You do not have enough funds."
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    apiErrorMsg = e.message ?: "Network error."
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(55.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Proceed to Pay", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Payment Allowed / Redirect to PhonePe Dialog
    if (showPaymentSuccessDialog) {
        val amount = amountInput.toDoubleOrNull() ?: 0.0
        AlertDialog(
            onDismissRequest = {
                showPaymentSuccessDialog = false
                onBack()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF6BCB77), modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Payment Allowed", color = Color.White)
                }
            },
            text = {
                Column {
                    Text("The transaction of ₹${amount.toInt()} to '$merchant' matches your budget.", color = Color.LightGray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Remaining in category after payment: ₹${remainingAfterPayment.toInt()}",
                        color = Color(0xFF6BCB77),
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaymentSuccessDialog = false
                        // Launch PhonePe UPI Intent
                        try {
                            val upiUri = "upi://pay?pa=$upiIdInput&pn=SmartPayTracker&am=$amount&cu=INR"
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                data = Uri.parse(upiUri)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No UPI App (PhonePe, GPay) found on device.", Toast.LENGTH_LONG).show()
                        }
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00B4D8))
                ) {
                    Text("Proceed to PhonePe", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPaymentSuccessDialog = false
                    onBack()
                }) {
                    Text("Skip Payment")
                }
            }
        )
    }
}

// Local Categorizer helper
fun localClassify(merchant: String): String? {
    val m = merchant.lowercase()
    if (m.contains("swiggy") || m.contains("zomato") || m.contains("eat") || m.contains("restaurant") || m.contains("food") || m.contains("cafe") || m.contains("starbucks") || m.contains("pizza") || m.contains("burger") || m.contains("kfc") || m.contains("mcdonald") || m.contains("instamart") || m.contains("blinkit") || m.contains("zepto") || m.contains("grocery")) return "Food"
    if (m.contains("apollo") || m.contains("clinic") || m.contains("medplus") || m.contains("hospital") || m.contains("pharmacy") || m.contains("pharma") || m.contains("doctor") || m.contains("medicine") || m.contains("health") || m.contains("diagnostic") || m.contains("lab")) return "Hospital"
    if (m.contains("rent") || m.contains("landlord") || m.contains("pg") || m.contains("flat") || m.contains("lease") || m.contains("house")) return "Rent"
    if (m.contains("amazon") || m.contains("flipkart") || m.contains("shopping") || m.contains("myntra") || m.contains("ajio") || m.contains("zara") || m.contains("h&m") || m.contains("clothing")) return "Shopping"
    if (m.contains("indian oil") || m.contains("hp petrol") || m.contains("shell") || m.contains("fuel") || m.contains("cng") || m.contains("gas") || m.contains("petrol") || m.contains("station") || m.contains("bharat petroleum")) return "Fuel"
    if (m.contains("school") || m.contains("tuition") || m.contains("milk") || m.contains("grocery") || m.contains("supermarket") || m.contains("kids") || m.contains("parent") || m.contains("gift") || m.contains("transfer") || m.contains("cash")) return "Family"
    return null
}
