package com.example.dacs31.ui.screen.wallet

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dacs31.data.Transaction
import com.example.dacs31.payment.CheckSePayTransactionPeriodically
import com.example.dacs31.payment.SepayTransactionResponse
import com.example.dacs31.payment.generateSePayQRUrl
import com.example.dacs31.payment.SePayQRDialog

@Composable
fun AddMoneyDialog(
    onDismiss: () -> Unit,
    viewModel: WalletModelView = viewModel()
) {
    var amount by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showQRDialog by remember { mutableStateOf(false) }
    var isCheckingTransaction by remember { mutableStateOf(false) }
    var paymentStatus by remember { mutableStateOf<String?>(null) }

    // Thông tin SePay
    val sepayAccountNumber = "1011200567890"
    val sepayBank = "MBBank"
    val qrDescription = "AddMoney${System.currentTimeMillis()}"
    val apiKey = "VBDZBRFJNSURATPSJY20BKGA3AKPMUWJSYA7RY59MCOQNERHSZ9KMOFIX87QFLFX"

    // Tạo QR URL khi cần
    val qrUrl = if (showQRDialog) {
        val amountValue = amount.toDoubleOrNull()?.toInt() ?: 0
        generateSePayQRUrl(
            accountNumber = sepayAccountNumber,
            bank = sepayBank,
            amount = amountValue,
            description = qrDescription
        )
    } else {
        ""
    }

    // Kiểm tra giao dịch định kỳ
    if (showQRDialog) {
        CheckSePayTransactionPeriodically(
            accountNumber = sepayAccountNumber,
            qrDescription = qrDescription,
            totalPrice = amount.toDoubleOrNull()?.toInt() ?: 0,
            apiKey = apiKey,
            onSuccess = {
                paymentStatus = "Payment successful!"
                val amountValue = amount.toDoubleOrNull() ?: 0.0
                val transaction = Transaction(
                    name = "Add Money via SePay",
                    amount = amountValue,
                    type = "credit"
                )
                viewModel.addTransaction(transaction)
                showQRDialog = false
                onDismiss()
            },
            onTimeout = {
                paymentStatus = "Payment timed out. Please try again."
                showQRDialog = false
            },
            onError = { error ->
                paymentStatus = error
            },
            isChecking = isCheckingTransaction,
            setChecking = { isCheckingTransaction = it }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.White, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Add Money to Wallet",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val amountValue = amount.toDoubleOrNull()
                            if (amountValue == null || amountValue <= 0) {
                                errorMessage = "Please enter a valid amount greater than 0."
                            } else {
                                errorMessage = null
                                showQRDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDAE10))
                    ) {
                        Text("Add via SePay", color = Color.Black)
                    }
                }

                if (showQRDialog) {
                    SePayQRDialog(
                        qrUrl = qrUrl,
                        totalPrice = amount.toDoubleOrNull()?.toInt() ?: 0,
                        paymentStatus = paymentStatus,
                        isCheckingTransaction = isCheckingTransaction,
                        onDismiss = {
                            showQRDialog = false
                        },
                        onTryAgain = {
                            paymentStatus = null
                            showQRDialog = true
                            isCheckingTransaction = true
                        }
                    )
                }
            }
        }
    }
}