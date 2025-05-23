package com.example.dacs31.data

import java.util.UUID

data class Wallet(
    val id: String = "",
    val userId: String = "",
    val balance: Double = 0.0,
    val transactions: Map<String, Transaction> = emptyMap()
)

data class Transaction(
    val id: String = UUID.randomUUID().toString(), // Sử dụng UUID làm id mặc định
    val name: String = "",
    val amount: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "debit"
)