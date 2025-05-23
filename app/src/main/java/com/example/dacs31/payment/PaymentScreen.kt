package com.example.dacs31.payment

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dacs31.R

@Composable
fun PaymentScreen(
    selectedTransport: String,
    routeDistance: Double,
    onDismiss: () -> Unit,
    onConfirmRide: (paymentMethod: String, fee: Int, requestId: String) -> Unit,
    requestId: String? = null,
    fee: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedPaymentMethod by remember { mutableStateOf("Visa") }
    var showQRDialog by remember { mutableStateOf(false) }
    var isCheckingTransaction by remember { mutableStateOf(false) }
    var paymentStatus by remember { mutableStateOf<String?>(null) }

    // Log trạng thái ban đầu
    Log.d("PaymentScreen", "PaymentScreen started: selectedTransport=$selectedTransport, routeDistance=$routeDistance, requestId=$requestId, fee=$fee")

    // Tính giá (dùng fee từ ViewModel nếu có, nếu không thì tính lại)
    val totalPrice = if (fee > 0) fee else {
        val distanceInKm = routeDistance / 1000
        val basePrice = 2000 //test 2000 vì k đủ tiền test
//        val basePrice = 17000
        val additionalRate = 3000
        when {
            distanceInKm <= 2 -> basePrice
            distanceInKm > 2 -> {
                val additionalKm = distanceInKm - 2
                basePrice + (additionalKm * additionalRate).toInt()
            }
            else -> 0
        }.let { if (selectedTransport == "Car") it * 2 else it }
    }
    Log.d("PaymentScreen", "Calculated totalPrice: $totalPrice, distanceInKm: ${routeDistance / 1000}")

    // Thông tin SePay
    val sepayAccountNumber = "1011200567890"
    val sepayBank = "MBBank"
    val qrDescription = requestId ?: "Ride${System.currentTimeMillis()}"
    val apiKey = "Enter API key"
    Log.d("PaymentScreen", "SePay info: accountNumber=$sepayAccountNumber, bank=$sepayBank, qrDescription=$qrDescription")

    val qrUrl = generateSePayQRUrl(
        accountNumber = sepayAccountNumber,
        bank = sepayBank,
        amount = totalPrice,
        description = qrDescription
    )
    Log.d("PaymentScreen", "Generated QR URL: $qrUrl")

    // Kiểm tra giao dịch định kỳ
    if (showQRDialog && selectedPaymentMethod == "SePay") {
        Log.d("PaymentScreen", "Starting CheckSePayTransactionPeriodically with isCheckingTransaction=$isCheckingTransaction")
        CheckSePayTransactionPeriodically(
            accountNumber = sepayAccountNumber,
            qrDescription = qrDescription,
            totalPrice = totalPrice,
            apiKey = apiKey,
            onSuccess = {
                Log.d("PaymentScreen", "Payment successful for qrDescription=$qrDescription")
                paymentStatus = "Payment successful!"
                onConfirmRide("SePay", totalPrice, qrDescription)
                showQRDialog = false
            },
            onTimeout = {
                Log.w("PaymentScreen", "Payment timed out for qrDescription=$qrDescription")
                paymentStatus = "Payment timed out. Please try again."
                showQRDialog = false
            },
            onError = { error ->
                Log.e("PaymentScreen", "Payment error: $error")
                paymentStatus = error
            },
            isChecking = isCheckingTransaction,
            setChecking = { isCheckingTransaction = it }
        )
    }

    Dialog(
        onDismissRequest = {
            Log.d("PaymentScreen", "Dialog dismissed")
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnClickOutside = true,
            dismissOnBackPress = true
        )
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            Log.d("PaymentScreen", "Back icon clicked")
                            onDismiss()
                        },
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Payment",
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // Thông tin xe
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFBE7), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Mustang Shelby GT",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = "Star",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "4.9 (531 reviews)",
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        )
                    }
                }
                Spacer(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.Gray, RoundedCornerShape(8.dp))
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chi tiết giá
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Distance",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )
                    Text(
                        text = String.format("%.2f km", routeDistance / 1000),
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Price",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )
                    Text(
                        text = "₫$totalPrice",
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phương thức thanh toán
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select payment method",
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                )
                Text(
                    text = "View All",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFFEDAE10),
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.clickable { Log.d("PaymentScreen", "View All clicked") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Danh sách phương thức thanh toán
            PaymentMethodOption(
                iconResId = R.drawable.wallet_icon_default,
                label = "Wallet",
                isSelected = selectedPaymentMethod == "Wallet",
                onClick = {
                    Log.d("PaymentScreen", "Selected payment method: Wallet")
                    selectedPaymentMethod = "Wallet"
                }
            )
            PaymentMethodOption(
                iconResId = R.drawable.sepay_icon_default,
                label = "SePay",
                isSelected = selectedPaymentMethod == "SePay",
                onClick = {
                    Log.d("PaymentScreen", "Selected payment method: SePay")
                    selectedPaymentMethod = "SePay"
                    showQRDialog = true
                    paymentStatus = null
                }
            )
            PaymentMethodOption(
                iconResId = R.drawable.ic_cash,
                label = "Cash",
                isSelected = selectedPaymentMethod == "Cash",
                onClick = {
                    Log.d("PaymentScreen", "Selected payment method: Cash")
                    selectedPaymentMethod = "Cash"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Nút Confirm Ride
            Button(
                onClick = {
                    Log.d("PaymentScreen", "Confirm Ride clicked with selectedPaymentMethod=$selectedPaymentMethod")
                    if (selectedPaymentMethod == "SePay") {
                        showQRDialog = true
                    } else {
                        onConfirmRide(selectedPaymentMethod, totalPrice, requestId ?: "")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEDAE10)
                )
            ) {
                Text(
                    text = "Confirm Ride",
                    color = Color.Black,
                    style = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Dialog hiển thị mã QR
            if (showQRDialog && selectedPaymentMethod == "SePay") {
                Log.d("PaymentScreen", "Showing SePayQRDialog with qrUrl=$qrUrl")
                SePayQRDialog(
                    qrUrl = qrUrl,
                    totalPrice = totalPrice,
                    paymentStatus = paymentStatus,
                    isCheckingTransaction = isCheckingTransaction,
                    onDismiss = {
                        Log.d("PaymentScreen", "SePayQRDialog dismissed")
                        showQRDialog = false
                    },
                    onTryAgain = {
                        Log.d("PaymentScreen", "SePayQRDialog Try Again clicked")
                        paymentStatus = null
                        showQRDialog = true
                        isCheckingTransaction = true
                    }
                )
            }
        }
    }

    // Log khi trạng thái thay đổi
    LaunchedEffect(showQRDialog, selectedPaymentMethod) {
        if (showQRDialog && selectedPaymentMethod == "SePay") {
            Log.d("PaymentScreen", "Setting isCheckingTransaction to true because showQRDialog=$showQRDialog")
            isCheckingTransaction = true
        }
    }
}

@Composable
fun PaymentMethodOption(
    iconResId: Int,
    label: String,
    cardNumber: String? = null,
    email: String? = null,
    expiry: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isSelected) Color(0xFFFFFBE7) else Color.White, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = label,
            tint = if (label == "Cash") Color.Gray else Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            when {
                cardNumber != null -> Text(
                    text = "$label $cardNumber",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
                email != null -> Text(
                    text = "$label $email",
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                )
                else -> Text(
                    text = label,
                    style = TextStyle(
                        fontSize = 16.sp,
                        color = if (label == "Cash") Color.Gray else Color.Black
                    )
                )
            }
            if (expiry != null) {
                Text(
                    text = "Expires: $expiry",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}