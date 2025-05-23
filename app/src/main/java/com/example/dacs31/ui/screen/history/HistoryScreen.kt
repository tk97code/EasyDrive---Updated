package com.example.dacs31.ui.screen.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.dacs31.data.AuthRepository
import com.google.firebase.Timestamp
import android.util.Log
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.dacs31.data.Trip
import com.example.dacs31.data.TripRepository
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    var userId by remember { mutableStateOf<String?>(null) }
    var userRole by remember { mutableStateOf<String?>(null) }
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }
    var selectedTab by remember { mutableStateOf("Upcoming") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val tripRepository = remember { TripRepository() }

    // Lấy thông tin người dùng
    LaunchedEffect(Unit) {
        val user = authRepository.getCurrentUser()
        Log.d("HistoryScreen", "Current user: $user")
        if (user == null) {
            Log.d("HistoryScreen", "Người dùng chưa đăng nhập, điều hướng đến màn hình đăng nhập")
            navController.navigate("signin") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            return@LaunchedEffect
        }

        userId = user.uid
        userRole = user.role
        Log.d("HistoryScreen", "User ID: $userId, Role: $userRole")
    }

    // Lấy dữ liệu chuyến đi
    LaunchedEffect(userId, userRole, selectedTab) {
        userId?.let { id: String ->
            Log.d("HistoryScreen", "Fetching trips for userId: $id, role: $userRole")
            try {
                val allTrips = when (userRole) {
                    "Customer" -> tripRepository.getTripsByCustomer(id)
                    "Driver" -> tripRepository.getTripsByDriver(id)
                    else -> {
                        Log.w("HistoryScreen", "Unknown role: $userRole, returning empty list")
                        emptyList<Trip>()
                    }
                }
                Log.d("HistoryScreen", "All trips fetched: $allTrips")
                trips = when (selectedTab) {
                    "Upcoming" -> allTrips.filter { it.status in listOf("pending", "accepted") }
                    "Completed" -> allTrips.filter { it.status == "completed" }
                    "Canceled" -> allTrips.filter { it.status == "canceled" }
                    else -> emptyList()
                }
                Log.d("HistoryScreen", "Filtered trips for $selectedTab: $trips")
                errorMessage = null
            } catch (e: Exception) {
                Log.e("HistoryScreen", "Error fetching trips: ${e.message}")
                errorMessage = "Lỗi tải chuyến đi: ${e.message}"
                trips = emptyList()
            }
        } ?: run {
            Log.w("HistoryScreen", "userId is null, skipping trip fetch")
        }
    }

    // Đợi userId
    if (userId == null || userRole == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Hiển thị lỗi nếu có
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Lỗi") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        // Thanh điều hướng với nút Back
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quay lại",
                modifier = Modifier.clickable { navController.popBackStack() },
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Lịch sử chuyến đi",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        // Tab Row
        TabRow(
            selectedTabIndex = when (selectedTab) {
                "Upcoming" -> 0
                "Completed" -> 1
                "Canceled" -> 2
                else -> 0
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            containerColor = Color.White,
            contentColor = Color.Black,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[when (selectedTab) {
                        "Upcoming" -> 0
                        "Completed" -> 1
                        "Canceled" -> 2
                        else -> 0
                    }]),
                    color = Color(0xFFEDAE10)
                )
            }
        ) {
            Tab(
                selected = selectedTab == "Upcoming",
                onClick = { selectedTab = "Upcoming" },
                text = { Text("Sắp tới") }
            )
            Tab(
                selected = selectedTab == "Completed",
                onClick = { selectedTab = "Completed" },
                text = { Text("Hoàn thành") }
            )
            Tab(
                selected = selectedTab == "Canceled",
                onClick = { selectedTab = "Canceled" },
                text = { Text("Đã hủy") }
            )
        }

        // Danh sách chuyến đi
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (trips.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Không có chuyến đi ${selectedTab.lowercase(Locale.getDefault())}",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                        )
                    }
                }
            } else {
                items(trips) { trip ->
                    TripItem(trip = trip, userRole = userRole!!)
                }
            }
        }
    }
}

@Composable
fun TripItem(trip: Trip, userRole: String) {
    // Lấy thông tin tên khách hàng hoặc tài xế
    var name by remember { mutableStateOf("N/A") }
//    LaunchedEffect(trip) {
//        try {
//            val db = Firebase.firestore
//            name = when (userRole) {
//                "Customer" -> trip.driverId?.let { driverId ->
//                    db.collection("drivers").document(driverId)
//                        .get()
//                        .await()
//                        .getString("name") ?: "N/A"
//                } ?: "Chưa có tài xế"
//                "Driver" -> db.collection("users").document(trip.customerId)
//                    .get()
//                    .await()
//                    .getString("name") ?: "N/A"
//                else -> "N/A"
//            }
//        } catch (e: Exception) {
//            Log.e("TripItem", "Error fetching name: ${e.message}")
//            name = "N/A"
//        }
//    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFBE7)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = when (userRole) {
                    "Customer" -> "Chuyến đi: ${trip.id}"
                    "Driver" -> "Khách hàng: $name"
                    else -> "Chuyến đi: ${trip.id}"
                },
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Điểm đón: (${trip.pickupLocation?.latitude() ?: "N/A"}, ${trip.pickupLocation?.longitude() ?: "N/A"})",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Điểm đến: (${trip.destination?.latitude() ?: "N/A"}, ${trip.destination?.longitude() ?: "N/A"})",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Thời gian: ${formatTimestamp(trip.time)}",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (userRole == "Customer") {
                Text(
                    text = "Tài xế: $name",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Phương thức thanh toán: ${trip.paymentMethod.takeIf { it.isNotEmpty() } ?: "N/A"}",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chi phí: ₫${trip.fee}",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
            } else {
                Text(
                    text = "Chi phí: ₫${trip.fee}",
                    style = TextStyle(
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Trạng thái: ${when (trip.status) {
                    "pending" -> "Đang chờ"
                    "accepted" -> "Đã chấp nhận"
                    "completed" -> "Hoàn thành"
                    "canceled" -> "Đã hủy"
                    else -> "Không xác định"
                }}",
                style = TextStyle(
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            )
        }
    }
}

fun formatTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "Không xác định"
    val date = timestamp.toDate()
    val today = Calendar.getInstance().apply { time = Date() }
    val tripDate = Calendar.getInstance().apply { time = date }
    val isToday = today.get(Calendar.YEAR) == tripDate.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == tripDate.get(Calendar.DAY_OF_YEAR)

    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = formatter.format(date)
    return if (isToday) {
        "Hôm nay lúc $timeString"
    } else {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val isTomorrow = tomorrow.get(Calendar.YEAR) == tripDate.get(Calendar.YEAR) &&
                tomorrow.get(Calendar.DAY_OF_YEAR) == tripDate.get(Calendar.DAY_OF_YEAR)
        if (isTomorrow) {
            "Ngày mai lúc $timeString"
        } else {
            SimpleDateFormat("d MMM 'lúc' h:mm a", Locale.getDefault()).format(date)
        }
    }
}