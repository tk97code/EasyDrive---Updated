package com.example.dacs31.ui.screen.wallet

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.data.Transaction
import com.example.dacs31.data.Wallet
import com.example.dacs31.ui.screen.componentsUI.BottomControlBar
import com.example.dacs31.ui.screen.componentsUI.TopControlBar

@SuppressLint("UnrememberedMutableState")
@Composable
fun WalletScreen(navController: NavController) {
    val authRepository = AuthRepository()
    val viewModel: WalletModelView = viewModel(factory = WalletModelViewFactory(authRepository))
    val wallet by viewModel.wallet.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var showAddMoneyDialog by remember { mutableStateOf(false) }

    // Kiểm tra trạng thái đăng nhập
    LaunchedEffect(Unit) {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            navController.navigate("login")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (errorMessage != null) {
            Text(
                text = errorMessage ?: "Unknown error",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Red,
                textAlign = TextAlign.Center
            )
        } else if (wallet.userId.isEmpty()) {
            Text(
                text = "No wallet found. Creating a new one...",
                modifier = Modifier.align(Alignment.Center),
                color = Color.Gray
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TopControlBar(
                    modifier = Modifier,
                    onMenuClick = { /* Xử lý sự kiện menu */ }
                )
                WalletContent(wallet, viewModel, showAddMoneyDialog, { showAddMoneyDialog = it })
            }
        }

        BottomControlBar(
            navController = navController,
            showConnectButton = false,
            isConnected = false,
            onConnectClick = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )

        if (showAddMoneyDialog) {
            AddMoneyDialog(
                onDismiss = { showAddMoneyDialog = false }
            )
        }
    }
}

@Composable
fun WalletContent(wallet: Wallet, viewModel: WalletModelView, showAddMoneyDialog: Boolean, setShowAddMoneyDialog: (Boolean) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 90.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
                .background(
                    color = Color(0xFFFFD700).copy(alpha = 0.3f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$${String.format("%.2f", wallet.balance)}",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.Black
                )
                Text(
                    text = "Available Balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        Button(
            onClick = { setShowAddMoneyDialog(true) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFB800)
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(text = "Add Money", color = Color.White)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            item {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )
            }
            items(wallet.transactions.values.toList().sortedByDescending { it.timestamp }) { transaction ->
                TransactionItem(
                    name = transaction.name,
                    amount = if (transaction.type == "debit") "-$${String.format("%.2f", transaction.amount)}" else "$${String.format("%.2f", transaction.amount)}",
                    color = if (transaction.type == "debit") Color(0xFFFF9999) else Color(0xFF99FF99),
                    timestamp = transaction.timestamp
                )
            }
        }
    }
}

@Composable
fun TransactionItem(name: String, amount: String, color: Color, timestamp: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, shape = RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(text = name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "Today at ${android.text.format.DateFormat.format("hh:mm a", timestamp)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        Text(text = amount, style = MaterialTheme.typography.bodyMedium)
    }
}