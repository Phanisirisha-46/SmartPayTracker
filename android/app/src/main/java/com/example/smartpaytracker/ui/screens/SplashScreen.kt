package com.example.smartpaytracker.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.smartpaytracker.Login
import com.example.smartpaytracker.Main
import com.example.smartpaytracker.data.api.ApiClient
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigate: (NavKey) -> Unit) {
    val context = LocalContext.current
    val scale = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 1.2f,
            animationSpec = tween(durationMillis = 800)
        )
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 300)
        )
        delay(1000)

        // Session check
        val token = ApiClient.getSavedToken(context)
        if (token != null) {
            onNavigate(Main)
        } else {
            onNavigate(Login)
        }
    }

    // Deep premium gradient background
    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF1E1E2E), // Slate Dark
            Color(0xFF0F0F16)  // Pitch Black Accent
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.scale(scale.value)
        ) {
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = "Wallet Logo",
                tint = Color(0xFF00B4D8), // Vibrant cyan
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "SmartPay Tracker",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Secure Smart Budgeting & UPI Pay",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
