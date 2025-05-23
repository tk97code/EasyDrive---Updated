package com.example.dacs31.payment

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

suspend fun checkSePayTransaction(
    accountNumber: String,
    limit: Int = 20,
    apiKey: String
): SepayTransactionResponse? {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true // Cho phép xử lý JSON không nghiêm ngặt
                encodeDefaults = false
            })
            engine {
                requestTimeout = 30000 // Đặt timeout 30 giây
            }
        }
    }
    return try {
        Log.d("SePayApiClient", "Sending request: accountNumber=$accountNumber, limit=$limit")
        val response: HttpResponse = client.get("https://my.sepay.vn/userapi/transactions/list") {
            headers {
                append("Authorization", "Bearer $apiKey")
                append("Content-Type", "application/json")
            }
            parameter("account_number", accountNumber)
            parameter("limit", limit)
        }
        val statusCode = response.status.value
        Log.d("SePayApiClient", "Received response with status: $statusCode")
        val responseBody: SepayTransactionResponse = response.body()
        Log.d("SePayApiClient", "API Response: Status=$statusCode, Body=$responseBody")
        if (statusCode == 200) responseBody else {
            Log.w("SePayApiClient", "API returned non-200 status: $statusCode")
            null
        }
    } catch (e: CancellationException) {
        Log.w("SePayApiClient", "API call cancelled: ${e.message}")
        null
    } catch (e: kotlinx.serialization.SerializationException) {
        Log.e("SePayApiClient", "Serialization error: ${e.message}", e)
        null
    } catch (e: Exception) {
        Log.e("SePayApiClient", "Error calling SePay API: ${e.message}", e)
        null
    } finally {
        Log.d("SePayApiClient", "Closing HTTP client")
        client.close()
    }
}