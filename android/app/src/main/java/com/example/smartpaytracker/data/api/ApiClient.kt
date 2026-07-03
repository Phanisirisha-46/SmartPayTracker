package com.example.smartpaytracker.data.api

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json

object ApiClient {
    private const val PREFS_NAME = "smart_pay_tracker_prefs"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_SERVER_IP = "server_ip"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"

    private var cachedIp: String? = null
    private var apiInstance: SmartPayApi? = null

    // Preferences operations
    fun getSavedServerIp(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ip = prefs.getString(KEY_SERVER_IP, null)
        if (ip == null || ip.isBlank() || ip == "10.0.2.2" || ip == "https://" || ip == "http://" || !ip.contains(".")) {
            return "smartpaytracker.onrender.com"
        }
        return ip
    }

    fun saveServerIp(context: Context, ip: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_IP, ip.trim()).apply()
    }

    fun getSavedToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, null)
    }

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun saveUser(context: Context, name: String, email: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun getUserName(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_NAME, "User") ?: "User"
    }

    fun getUserEmail(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_EMAIL, "") ?: ""
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER_NAME).remove(KEY_USER_EMAIL).apply()
    }

    // Retrofit Creator
    fun getApi(context: Context): SmartPayApi {
        val ip = getSavedServerIp(context)
        if (apiInstance == null || cachedIp != ip) {
            cachedIp = ip
            val baseUrl = if (ip.contains("://")) {
                if (ip.endsWith("/")) ip else "$ip/"
            } else if (ip.contains(".") && !ip.replace(".", "").all { it.isDigit() }) {
                "https://$ip/"
            } else {
                "http://$ip:8000/"
            }

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            val jsonConfig = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                encodeDefaults = true
            }

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(jsonConfig.asConverterFactory("application/json".toMediaType()))
                .build()

            apiInstance = retrofit.create(SmartPayApi::class.java)
        }
        return apiInstance!!
    }
}
