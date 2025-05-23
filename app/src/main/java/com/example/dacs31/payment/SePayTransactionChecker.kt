package com.example.dacs31.payment

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CheckSePayTransactionPeriodically(
    accountNumber: String,
    qrDescription: String,
    totalPrice: Int,
    apiKey: String,
    onSuccess: () -> Unit,
    onTimeout: () -> Unit,
    onError: (String) -> Unit,
    isChecking: Boolean,
    setChecking: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var checkCount by remember { mutableStateOf(0) }
    var lastCheckTime by remember { mutableStateOf(0L) }

    // Hàm kiểm tra kết nối mạng
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        Log.d("SePayTransactionChecker", "Network available: $hasInternet")
        return hasInternet
    }

    LaunchedEffect(isChecking, qrDescription, totalPrice) {
        Log.d("SePayTransactionChecker", "LaunchedEffect triggered: isChecking=$isChecking, qrDescription=$qrDescription, totalPrice=$totalPrice")
        if (!isChecking) {
            Log.d("SePayTransactionChecker", "isChecking is false, setting to true to start checking")
            setChecking(true)
        }

        if (!isNetworkAvailable()) {
            Log.w("SePayTransactionChecker", "No internet connection")
            onError("No internet connection. Please check your network and try again.")
            setChecking(false)
            return@LaunchedEffect
        }

        val startTime = System.currentTimeMillis()
        val timeout = 5 * 60 * 1000 // 5 phút timeout
        Log.d("SePayTransactionChecker", "Starting transaction check loop: timeout=$timeout ms")

        while (isActive && System.currentTimeMillis() - startTime < timeout) {
            Log.d("SePayTransactionChecker", "Checking transaction: account=$accountNumber, desc=$qrDescription, amount=$totalPrice, checkCount=$checkCount")
            val response = checkSePayTransaction(accountNumber, apiKey = apiKey)
            if (isActive) {
                if (response != null) {
                    if (response.status == 200 && response.messages["success"] == true) {
                        Log.d("SePayTransactionChecker", "API response successful, checking transactions: ${response.transactions.size} transactions found")
                        response.transactions.forEach { transaction ->
                            Log.d("SePayTransactionChecker", "Transaction: content=${transaction.transactionContent}, amountIn=${transaction.amountIn}")
                            if (transaction.transactionContent == qrDescription &&
                                transaction.amountIn.toDoubleOrNull() == totalPrice.toDouble()
                            ) {
                                Log.d("SePayTransactionChecker", "Payment successful for $qrDescription")
                                onSuccess()
                                setChecking(false)
                                return@LaunchedEffect
                            }
                        }
                        Log.d("SePayTransactionChecker", "No matching transaction found in response")
                    } else {
                        val errorMessage = response.error ?: "Unknown error"
                        Log.w("SePayTransactionChecker", "Error checking payment status: $errorMessage")
                        onError("Error checking payment status: $errorMessage")
                    }
                } else {
                    Log.w("SePayTransactionChecker", "Error checking payment status: Response is null")
                    onError("Error checking payment status: Response is null. Please try again.")
                }
                lastCheckTime = System.currentTimeMillis()
                Log.d("SePayTransactionChecker", "Waiting 5 seconds before next check, lastCheckTime=$lastCheckTime")
                delay(5000)
                checkCount++
            }
        }
        if (isActive) {
            Log.w("SePayTransactionChecker", "Payment timed out for $qrDescription after $checkCount checks")
            onTimeout()
            setChecking(false)
        } else {
            Log.w("SePayTransactionChecker", "Coroutine cancelled during transaction check")
        }
    }
}