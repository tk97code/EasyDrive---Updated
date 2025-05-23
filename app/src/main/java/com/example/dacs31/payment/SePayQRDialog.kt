package com.example.dacs31.payment

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun SePayQRDialog(
    qrUrl: String,
    totalPrice: Int,
    paymentStatus: String?,
    isCheckingTransaction: Boolean,
    onDismiss: () -> Unit,
    onTryAgain: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    Log.d("SePayQRDialog", "Showing dialog: qrUrl=$qrUrl, totalPrice=$totalPrice, paymentStatus=$paymentStatus, isCheckingTransaction=$isCheckingTransaction")

    AlertDialog(
        onDismissRequest = {
            Log.d("SePayQRDialog", "Dialog dismissed via onDismissRequest")
            onDismiss()
        },
        title = { Text("Scan QR to Pay") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Please scan the QR code to pay ₫$totalPrice",
                    style = TextStyle(fontSize = 14.sp),
                    modifier = Modifier.padding(8.dp)
                )
                AsyncImage(
                    model = qrUrl,
                    contentDescription = "SePay QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .clip(MaterialTheme.shapes.medium),
                    onSuccess = { Log.d("SePayQRDialog", "QR image loaded successfully") },
                    onError = { Log.e("SePayQRDialog", "Failed to load QR image: ${it.result.throwable.message}") }
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (isCheckingTransaction) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                    Text(
                        text = "Checking payment status...",
                        style = TextStyle(fontSize = 14.sp)
                    )
                    Log.d("SePayQRDialog", "Showing checking payment status indicator")
                }
                paymentStatus?.let {
                    Text(
                        text = it,
                        style = TextStyle(
                            fontSize = 14.sp,
                            color = if (it.contains("successful")) Color.Green else Color.Red
                        ),
                        modifier = Modifier.padding(8.dp)
                    )
                    Log.d("SePayQRDialog", "Showing payment status: $it")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    Log.d("SePayQRDialog", "Close button clicked")
                    onDismiss()
                }
            ) {
                Text("Close")
            }
        },
        dismissButton = {
            if (paymentStatus != null && !paymentStatus.contains("successful")) {
                Button(
                    onClick = {
                        Log.d("SePayQRDialog", "Try Again button clicked")
                        onTryAgain()
                        coroutineScope.launch {
                            Log.d("SePayQRDialog", "Coroutine launched for Try Again")
                        }
                    }
                ) {
                    Text("Try Again")
                }
            } else {
                Log.d("SePayQRDialog", "Try Again button not shown: paymentStatus=$paymentStatus")
            }
        }
    )
}