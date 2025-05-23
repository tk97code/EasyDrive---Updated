package com.example.dacs31.ui.screen.wallet

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.data.Transaction
import com.example.dacs31.data.Wallet
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class WalletModelView(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val scope = CoroutineScope(Dispatchers.Main)
    private val TAG = "WalletModelView"

    private val _wallet = MutableStateFlow(Wallet())
    val wallet = _wallet.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    init {
        loadWallet()
    }

    fun loadWallet() {
        scope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                val user = authRepository.getCurrentUser()
                val userId = user?.uid ?: run {
                    Log.e(TAG, "User not logged in")
                    _isLoading.value = false
                    _errorMessage.value = "User not logged in"
                    return@launch
                }
                Log.d(TAG, "Loading wallet for userId: $userId")
                val snapshot = db.collection("wallets")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                if (!snapshot.isEmpty) {
                    val document = snapshot.documents[0]
                    val walletData = document.toObject<Wallet>() ?: Wallet()
                    // Xử lý trường transactions nếu nó là Map
                    val transactions = if (walletData.transactions.isEmpty() && document.data?.containsKey("transactions") == true) {
                        val transactionMap = document.get("transactions") as? Map<String, Any> ?: emptyMap()
                        transactionMap.mapValues { convertMapToTransaction(it.value as Map<String, Any>) }
                    } else {
                        walletData.transactions
                    }
                    val updatedWallet = walletData.copy(transactions = transactions)
                    Log.d(TAG, "Wallet found: $updatedWallet")
                    _wallet.value = updatedWallet
                } else {
                    Log.w(TAG, "No wallet found for userId: $userId, creating new wallet")
                    val newWallet = Wallet(userId = userId, balance = 500.0)
                    val newDocRef = db.collection("wallets").add(newWallet).await()
                    val createdWallet = newWallet.copy(id = newDocRef.id)
                    Log.d(TAG, "New wallet created with ID: ${newDocRef.id}")
                    _wallet.value = createdWallet
                }
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e(TAG, "Error loading wallet: ${e.message}", e)
                _isLoading.value = false
                _errorMessage.value = "Error loading wallet: ${e.message}"
            }
        }
    }

    fun addTransaction(transaction: Transaction) {
        scope.launch {
            try {
                val user = authRepository.getCurrentUser()
                val userId = user?.uid ?: run {
                    Log.e(TAG, "User not logged in")
                    _errorMessage.value = "User not logged in"
                    return@launch
                }
                val walletRef = db.collection("wallets")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                if (!walletRef.isEmpty) {
                    val walletDoc = walletRef.documents[0]
                    val currentWallet = walletDoc.toObject<Wallet>() ?: Wallet()
                    val updatedTransactions = currentWallet.transactions.toMutableMap().apply {
                        put(transaction.id, transaction)
                    }
                    val newBalance = if (transaction.type == "credit") {
                        currentWallet.balance + transaction.amount
                    } else {
                        currentWallet.balance - transaction.amount
                    }
                    Log.d(TAG, "Updating wallet with new transaction: $transaction")
                    walletDoc.reference.update(
                        mapOf(
                            "balance" to newBalance,
                            "transactions" to updatedTransactions
                        )
                    ).await()
                    _wallet.value = currentWallet.copy(
                        balance = newBalance,
                        transactions = updatedTransactions
                    )
                    Log.d(TAG, "Wallet updated successfully: Balance = $newBalance")
                } else {
                    Log.w(TAG, "No wallet found to add transaction")
                    _errorMessage.value = "No wallet found to add transaction"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding transaction: ${e.message}", e)
                _errorMessage.value = "Error adding transaction: ${e.message}"
            }
        }
    }

    private fun convertMapToTransaction(map: Map<String, Any>): Transaction {
        return Transaction(
            id = map["id"] as? String ?: "",
            name = map["name"] as? String ?: "",
            amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
            timestamp = (map["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            type = map["type"] as? String ?: "debit"
        )
    }
}