package com.example.dacs31.payment

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SepayTransactionResponse(
    @SerialName("status") val status: Int,
    @SerialName("error") val error: String? = null,
    @SerialName("messages") val messages: Map<String, Boolean>,
    @SerialName("transactions") val transactions: List<Transaction>
)

@Serializable
data class Transaction(
    @SerialName("id") val id: String,
    @SerialName("bank_brand_name") val bankBrandName: String,
    @SerialName("account_number") val accountNumber: String,
    @SerialName("transaction_date") val transactionDate: String,
    @SerialName("amount_out") val amountOut: String,
    @SerialName("amount_in") val amountIn: String,
    @SerialName("accumulated") val accumulated: String,
    @SerialName("transaction_content") val transactionContent: String,
    @SerialName("reference_number") val referenceNumber: String,
    @SerialName("code") val code: String? = null,
    @SerialName("sub_account") val subAccount: String? = null,
    @SerialName("bank_account_id") val bankAccountId: String
)

@Serializable
data class Messages(
    val success: Boolean
)