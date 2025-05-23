package com.example.dacs31.ui.screen.customer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.dacs31.R
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.data.Transaction
import com.example.dacs31.data.User
import com.example.dacs31.map.MapComponent
import com.example.dacs31.payment.PaymentScreen
import com.example.dacs31.ui.screen.WaitingScreen
import com.example.dacs31.ui.screen.componentsUI.BottomControlBar
import com.example.dacs31.ui.screen.componentsUI.SideMenuDrawer
import com.example.dacs31.ui.screen.componentsUI.TopControlBar
import com.example.dacs31.ui.screen.location.RouteInfoDialog
import com.example.dacs31.ui.screen.location.SelectAddressDialog
import com.example.dacs31.ui.screen.transport.SelectTransportScreen
import com.example.dacs31.ui.screen.wallet.WalletModelView
import com.example.dacs31.ui.screen.wallet.WalletModelViewFactory
import com.google.firebase.database.core.RepoManager.clear
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.turf.TurfMeasurement
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import kotlinx.coroutines.launch
import java.util.UUID

@SuppressLint("RestrictedApi")
@Composable
fun CustomerHomeScreen(
    navController: NavController,
    authRepository: AuthRepository
) {
    val viewModel: CustomerHomeViewModel = hiltViewModel()
    val walletViewModel: WalletModelView = viewModel(factory = WalletModelViewFactory(authRepository))
    val context = LocalContext.current
    val uiState by viewModel.uiState.observeAsState(CustomerHomeUiState())
    val wallet by walletViewModel.wallet.collectAsState()
    val walletErrorMessage by walletViewModel.errorMessage.collectAsState()
    var mapViewInstance by remember { mutableStateOf<MapView?>(null) }
    val mapboxAccessToken = context.getString(R.string.mapbox_access_token)
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val db = Firebase.firestore
    val requestsCollection = db.collection("requests")

    var currentUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(Unit) {
        val user = authRepository.getCurrentUser()
        if (user == null) {
            Log.d("CustomerHomeScreen", "Người dùng chưa đăng nhập, điều hướng đến màn hình đăng nhập")
            viewModel.setErrorMessage("Vui lòng đăng nhập để tiếp tục.")
            navController.navigate("signin") {
                popUpTo(navController.graph.startDestinationId) { inclusive = true }
            }
            return@LaunchedEffect
        }

        if (user.role != "Customer") {
            Log.d("CustomerHomeScreen", "Người dùng không phải khách hàng, vai trò: ${user.role}")
//            viewModel.setErrorMessage("Vui lòng đăng nhập với tài khoản khách hàng.")
            if (user.role == "Driver") {
                navController.navigate("driver_home") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            } else {
                navController.navigate("signin") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
            return@LaunchedEffect
        }

        currentUser = user
        viewModel.initializeCustomer()
    }

    LaunchedEffect(Unit) {
        val testDocRef = db.collection("test").document("test_message")
        testDocRef.set(mapOf("message" to "Hello, Firestore!"))
            .addOnSuccessListener {
                Log.d("FirestoreTest", "Ghi dữ liệu thành công!")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreTest", "Ghi dữ liệu thất bại: ${e.message}")
            }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.setPermissionDeniedDialog(false)
        } else {
            viewModel.setPermissionDeniedDialog(true)
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (uiState.isPending && uiState.currentRequestId != null) {
        WaitingScreen(
            userLocation = uiState.userLocation,
            mapboxAccessToken = mapboxAccessToken,
            currentRequestId = uiState.currentRequestId!!,
            selectedTransport = uiState.selectedTransport,
            onRequestAccepted = {
                viewModel.setIsPending(false)
                viewModel.setDriverAcceptedDialog(true)
            },
            onRequestCanceled = {
                viewModel.setIsPending(false)
                viewModel.setRequestCanceledDialog(true)
                viewModel.clearCurrentRequest()
            }
        )
        return
    }

    if (uiState.showPermissionDeniedDialog) {
//        AlertDialog(
//            onDismissRequest = { viewModel.setPermissionDeniedDialog(false) },
//            title = { Text("Quyền vị trí bị từ chối") },
//            text = { Text("Ứng dụng cần quyền vị trí để hiển thị bản đồ và vị trí của bạn. Vui lòng cấp quyền trong cài đặt.") },
//            confirmButton = {
//                TextButton(onClick = { viewModel.setPermissionDeniedDialog(false) }) {
//                    Text("OK")
//                }
//            }
//        )
    } else {
        uiState.userLocation?.let { point ->
            uiState.driverLocation?.let { driver ->
                val points = listOf(point, driver)
                val bounds = TurfMeasurement.bbox(LineString.fromLngLats(points))
                mapViewInstance?.getMapboxMap()?.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2))
                        .zoom(12.0)
                        .build()
                )
            } ?: run {
                mapViewInstance?.getMapboxMap()?.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
            }
        } ?: run {
            viewModel.setPermissionDeniedDialog(true)
        }
    }

    if (uiState.showDriverAcceptedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setDriverAcceptedDialog(false) },
            title = { Text("Yêu cầu được chấp nhận") },
            text = { Text("Tài xế đã chấp nhận yêu cầu của bạn. Hãy chuẩn bị cho chuyến đi!") },
            confirmButton = {
                TextButton(onClick = { viewModel.setDriverAcceptedDialog(false) }) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.showRequestCanceledDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setRequestCanceledDialog(false) },
            title = { Text("Yêu cầu bị hủy") },
            text = { Text("Yêu cầu của bạn đã bị hủy. Vui lòng đặt lại chuyến đi.") },
            confirmButton = {
                TextButton(onClick = { viewModel.setRequestCanceledDialog(false) }) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.showTripCompletedDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setTripCompletedDialog(false) },
            title = { Text("Chuyến đi hoàn thành") },
            text = { Text("Chuyến đi của bạn đã hoàn thành thành công!") },
            confirmButton = {
                TextButton(onClick = { viewModel.setTripCompletedDialog(false) }) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.setErrorMessage(null) },
            title = { Text("Lỗi") },
            text = { Text(uiState.errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.setErrorMessage(null) }) {
                    Text("OK")
                }
            }
        )
    }

    if (walletErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { walletViewModel.loadWallet() },
            title = { Text("Lỗi Ví") },
            text = { Text(walletErrorMessage!!) },
            confirmButton = {
                TextButton(onClick = { walletViewModel.loadWallet() }) {
                    Text("OK")
                }
            }
        )
    }

    if (uiState.showSelectAddressDialog) {
        SelectAddressDialog(
            onDismiss = {
                viewModel.setSelectAddressDialog(false)
//                viewModel.clearMapData()
            },
            onConfirm = { from, fromAddr, to, toAddr ->
                viewModel.handleAddressSelection(from, fromAddr, to, toAddr, mapboxAccessToken)
            },
            userLocation = uiState.userLocation,
            mapboxAccessToken = mapboxAccessToken
        )
    }

    if (uiState.showSelectTransportDialog) {
        SelectTransportScreen(
            onDismiss = {
                viewModel.setSelectTransportDialog(false)
//                viewModel.clearMapData()
            },
            onTransportSelected = { transport -> viewModel.selectTransport(transport) }
        )
    }

    if (uiState.showPaymentScreen) {
        if (uiState.isSendingRequest) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Use the standalone PaymentScreen composable
        PaymentScreen(
            selectedTransport = uiState.selectedTransport ?: "Car",
            routeDistance = uiState.routeDistance,
            onDismiss = { viewModel.setPaymentScreen(false) },
            onConfirmRide = { paymentMethod, fee, requestId ->
                Log.d("Test", uiState.routePoints.toString())
                val effectiveRequestId = requestId.takeIf { it.isNotEmpty() } ?: uiState.currentRequestId ?: UUID.randomUUID().toString()
                Log.d("CustomerHomeScreen", "onConfirmRide called with paymentMethod=$paymentMethod, fee=$fee, requestId=$effectiveRequestId")

                if (paymentMethod == "Wallet") {
                    // Check for existing transaction based on requestId
                    val existingTransaction = wallet.transactions.entries.find { it.value.name.contains(effectiveRequestId) }
                    if (existingTransaction != null) {
                        Log.d("CustomerHomeScreen", "Transaction already exists for requestId: $effectiveRequestId")
                        viewModel.setErrorMessage("Giao dịch cho chuyến đi này đã được xử lý.")
                        viewModel.setPaymentScreen(false)
                        return@PaymentScreen
                    }

                    // Check wallet balance
                    if (wallet.balance < fee) {
                        Log.d("CustomerHomeScreen", "Insufficient balance: ${wallet.balance} < $fee")
                        viewModel.setErrorMessage("Số dư không đủ. Vui lòng nạp thêm tiền vào ví.")
                        viewModel.setPaymentScreen(false)
                        return@PaymentScreen
                    }

                    // Create new transaction
                    val newTransaction = Transaction(
                        id = UUID.randomUUID().toString(),
                        name = "Ride Payment - $effectiveRequestId",
                        amount = fee.toDouble(),
                        timestamp = System.currentTimeMillis(),
                        type = "debit"
                    )

                    coroutineScope.launch {
                        try {
                            walletViewModel.addTransaction(newTransaction)
                            viewModel.confirmRide(paymentMethod, fee)
                            viewModel.setPaymentConfirmed(true)
                            viewModel.setPaymentScreen(false)
                            Log.d("CustomerHomeScreen", "Transaction added: $newTransaction")
                        } catch (e: Exception) {
                            Log.e("CustomerHomeScreen", "Failed to add transaction: ${e.message}")
                            viewModel.setErrorMessage("Lỗi khi xử lý thanh toán: ${e.message}")
                        }
                    }
                } else {
                    viewModel.confirmRide(paymentMethod, fee)
                    if (paymentMethod == "SePay") {
                        viewModel.setPaymentConfirmed(true)
                    } else {
                        viewModel.setIsPending(true)
                        viewModel.setPaymentScreen(false)
                    }
                }
                Log.d(
                    "CustomerHomeScreen",
                    "Ride confirmed with paymentMethod: $paymentMethod, fee: $fee, requestId: $effectiveRequestId"
                )
            },
            requestId = uiState.currentRequestId,
            fee = uiState.fee
        )

        if (uiState.paymentConfirmed) {
            AlertDialog(
                onDismissRequest = {
                    viewModel.setPaymentConfirmed(false)
                },
                title = { Text("Thanh toán thành công") },
                text = {
                    Text(
                        when (uiState.selectedPaymentMethod) {
                            "SePay" -> "Giao dịch của bạn đã được xác nhận qua SePay."
                            "Cash" -> "Thanh toán bằng tiền mặt đã được xác nhận."
                            "Wallet" -> "Thanh toán qua ví đã được xác nhận."
                            else -> "Thanh toán đã được xác nhận."
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.setPaymentConfirmed(false)
                    }) {
                        Text("OK")
                    }
                }
            )
        }
    }

    var requestListener by remember { mutableStateOf<ListenerRegistration?>(null) }
    var driverListener by remember { mutableStateOf<ListenerRegistration?>(null) }

    LaunchedEffect(uiState.currentRequestId) {
        requestListener?.remove()
        requestListener = null

        uiState.currentRequestId?.let { requestId ->
            requestListener = requestsCollection.document(requestId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Lỗi khi lắng nghe trạng thái yêu cầu: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val status = snapshot.getString("status")
                        val newDriverId = snapshot.getString("driverId")
                        val paymentMethod = snapshot.getString("paymentMethod")
                        val fee = snapshot.getDouble("fee")?.toInt() ?: 0

                        when (status) {
                            "accepted" -> {
                                if (!uiState.isPending) {
                                    viewModel.setDriverAcceptedDialog(true)
                                }
                                viewModel.setDriverId(newDriverId)
                            }
                            "canceled" -> {
                                if (!uiState.isPending) {
                                    viewModel.setRequestCanceledDialog(true)
                                }
                                viewModel.clearCurrentRequest()
                            }
                            "completed" -> {
                                // Only add transaction if it hasn't been added
                                if (paymentMethod == "Wallet" && fee > 0) {
                                    coroutineScope.launch {
                                        try {
                                            val existingTransaction = wallet.transactions.entries.find {
                                                it.value.name.contains(requestId)
                                            }
                                            if (existingTransaction == null && wallet.balance >= fee) {
                                                val newTransaction = Transaction(
                                                    id = UUID.randomUUID().toString(),
                                                    name = "Ride Payment - $requestId",
                                                    amount = fee.toDouble(),
                                                    timestamp = System.currentTimeMillis(),
                                                    type = "debit"
                                                )
                                                walletViewModel.addTransaction(newTransaction)
                                                Log.d("CustomerHomeScreen", "Added transaction for completed request: $newTransaction")
                                            } else if (existingTransaction != null) {
                                                Log.d("CustomerHomeScreen", "Transaction already exists for requestId: $requestId")
                                            } else {
                                                viewModel.setErrorMessage("Số dư không đủ để ghi nhận giao dịch cho chuyến đi đã hoàn thành.")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("CustomerHomeScreen", "Failed to add transaction for completed request: ${e.message}")
                                            viewModel.setErrorMessage("Lỗi khi ghi nhận giao dịch: ${e.message}")
                                        }
                                    }
                                }

                                requestsCollection.document(requestId).get()
                                    .addOnSuccessListener { document ->
                                        if (document.exists()) {
                                            // Optionally delete request
                                        }
                                        viewModel.setTripCompletedDialog(true)
                                        viewModel.clearCurrentRequest()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firestore", "Lỗi khi kiểm tra tài liệu: ${e.message}")
                                        viewModel.clearCurrentRequest()
                                    }
                            }
                        }
                    }
                }
        }
    }

    LaunchedEffect(uiState.driverId) {
        driverListener?.remove()
        driverListener = null

        uiState.driverId?.let { id ->
            driverListener = db.collection("drivers").document(id)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Firestore", "Lỗi khi lắng nghe vị trí tài xế: ${error.message}")
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val locationData = snapshot.get("location") as? Map<String, Double>
                        locationData?.let {
                            val latitude = it["latitude"] ?: return@let
                            val longitude = it["longitude"] ?: return@let
                            viewModel.updateDriverLocation(Point.fromLngLat(longitude, latitude))
                        }
                    }
                }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            requestListener?.remove()
            driverListener?.remove()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SideMenuDrawer(
                user = currentUser,
                navController = navController,
                authRepository = authRepository,
                onDrawerClose = {
                    coroutineScope.launch { drawerState.close() }
                    viewModel.toggleDrawer(false)
                }
            )
        },
        modifier = Modifier.fillMaxSize(),
        gesturesEnabled = false
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            MapComponent(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(0f),
                routePoints = uiState.routePoints,
                fromPoint = uiState.fromPoint,
                toPoint = uiState.toPoint,
                driverLocation = uiState.driverLocation,
                userLocation = uiState.userLocation,
                onUserLocationUpdated = { point ->
                    viewModel.updateUserLocation(point)
                },
                onMapReady = { mapView, pointAnnotationManager ->
                    Log.d("CustomerHomeScreen", "Map is ready")
                },
                onMapViewReady = { mapView ->
                    mapViewInstance = mapView
                }
            )

            LaunchedEffect(uiState.userLocation) {
                uiState.userLocation?.let { point ->
                    mapViewInstance?.getMapboxMap()?.setCamera(
                        CameraOptions.Builder()
                            .center(point)
                            .zoom(15.0)
                            .build()
                    )
                }
            }

            TopControlBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.TopCenter)
                    .zIndex(2f),
                onMenuClick = {
                    coroutineScope.launch {
                        if (drawerState.isOpen) {
                            drawerState.close()
                            viewModel.toggleDrawer(false)
                        } else {
                            drawerState.open()
                            viewModel.toggleDrawer(true)
                        }
                    }
                }
            )

            BottomControlBar(
                navController = navController,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
            )

            FloatingActionButton(
                onClick = {
                    uiState.userLocation?.let { point ->
                        uiState.driverLocation?.let { driver ->
                            val points = listOf(point, driver)
                            val bounds = TurfMeasurement.bbox(LineString.fromLngLats(points))
                            mapViewInstance?.getMapboxMap()?.setCamera(
                                CameraOptions.Builder()
                                    .center(Point.fromLngLat((bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2))
                                    .zoom(12.0)
                                    .build()
                            )
                        } ?: run {
                            mapViewInstance?.getMapboxMap()?.setCamera(
                                CameraOptions.Builder()
                                    .center(point)
                                    .zoom(15.0)
                                    .build()
                            )
                        }
                    } ?: run {
                        viewModel.setPermissionDeniedDialog(true)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 150.dp, end = 16.dp)
                    .zIndex(2f),
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location"
                )
            }

            if (uiState.showRouteInfoDialog) {
                RouteInfoDialog(
                    fromAddress = uiState.fromAddress,
                    toAddress = uiState.toAddress,
                    distance = uiState.routeDistance,
                    onDismiss = {
                        viewModel.setRouteInfoDialog(false)
                        viewModel.setSelectTransportDialog(true)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.BottomCenter)
                        .zIndex(2f)
                )
            }

            if (!uiState.showRouteInfoDialog && !uiState.isDrawerOpen) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                        .zIndex(2f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Trong suốt với sự kiện chạm ở khu vực không có nút */ }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
//                    Button(
//                        onClick = { /* TODO: Xử lý khi nhấn Rental */ },
//                        modifier = Modifier
//                            .width(172.dp)
//                            .height(54.dp)
//                            .padding(start = 15.dp)
//                            .clip(RoundedCornerShape(8.dp)),
//                        colors = ButtonDefaults.buttonColors(
//                            containerColor = Color(0xFFEDAE10)
//                        ),
//                        contentPadding = PaddingValues(0.dp)
//                    ) {
//                        Text(
//                            text = "Rental",
//                            color = Color.Black,
//                            style = TextStyle(
//                                fontSize = 16.sp,
//                                fontWeight = FontWeight.Medium,
//                                lineHeight = 23.sp
//                            )
//                        )
//                    }

                    Box(
                        modifier = Modifier
                            .width(336.dp)
                            .height(48.dp)
                            .padding(horizontal = 28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFFBE7))
                            .border(
                                BorderStroke(2.dp, Color(0xFFF3BD06)),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { viewModel.setSelectAddressDialog(true) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Where would you go?",
                                color = Color.Gray,
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 23.sp
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = Color.Gray
                            )
                        }
                    }

//                    Row(
//                        modifier = Modifier
//                            .width(336.dp)
//                            .height(48.dp)
//                            .padding(horizontal = 28.dp)
//                            .clip(RoundedCornerShape(8.dp))
//                            .background(Color(0xFFFFFBE7))
//                            .border(
//                                BorderStroke(2.dp, Color(0xFFF3BD06)),
//                                RoundedCornerShape(8.dp)
//                            )
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .weight(1f)
//                                .fillMaxHeight()
//                                .background(
//                                    if (uiState.selectedMode == "Transport") Color(0xFFEDAE10) else Color(0xFFFFFBE7)
//                                )
//                                .clickable { viewModel.setSelectedMode("Transport") },
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = "Transport",
//                                color = if (uiState.selectedMode == "Transport") Color.White else Color(0xFF414141),
//                                style = TextStyle(
//                                    fontSize = 16.sp,
//                                    fontWeight = FontWeight.Medium,
//                                    lineHeight = 23.sp
//                                )
//                            )
//                        }
//
//                        Box(
//                            modifier = Modifier
//                                .weight(1f)
//                                .fillMaxHeight()
//                                .background(
//                                    if (uiState.selectedMode == "Delivery") Color(0xFFEDAE10) else Color(0xFFFFFBE7)
//                                )
//                                .clickable { viewModel.setSelectedMode("Delivery") },
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text(
//                                text = "Delivery",
//                                color = if (uiState.selectedMode == "Delivery") Color.White else Color(0xFF414141),
//                                style = TextStyle(
//                                    fontSize = 16.sp,
//                                    fontWeight = FontWeight.Medium,
//                                    lineHeight = 23.sp
//                                )
//                            )
//                        }
//                    }
                }
            }
        }
    }
}