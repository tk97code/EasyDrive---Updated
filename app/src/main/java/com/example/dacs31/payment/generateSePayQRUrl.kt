package com.example.dacs31.payment

import android.util.Log

fun generateSePayQRUrl(
    accountNumber: String,
    bank: String,
    amount: Int,
    description: String
): String {
    Log.d("generateSePayQRUrl", "Generating QR URL with: accountNumber=$accountNumber, bank=$bank, amount=$amount, description=$description")
    val url = "https://qr.sepay.vn/img?acc=$accountNumber&bank=$bank&amount=$amount&des=$description"
    Log.d("generateSePayQRUrl", "Generated QR URL: $url")
    return url
}