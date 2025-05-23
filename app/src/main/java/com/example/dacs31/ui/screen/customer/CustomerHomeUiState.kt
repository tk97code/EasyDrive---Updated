package com.example.dacs31.ui.screen.customer

import com.example.dacs31.data.User
import com.mapbox.geojson.Point

data class CustomerHomeUiState(
    val customerId: String? = null,
    val user: User? = null,
    val userLocation: Point? = null,
    val showPermissionDeniedDialog: Boolean = false,
    val showSelectAddressDialog: Boolean = false,
    val showRouteInfoDialog: Boolean = false,
    val showSelectTransportDialog: Boolean = false,
    val showPaymentScreen: Boolean = false,
    val showDriverAcceptedDialog: Boolean = false,
    val showRequestCanceledDialog: Boolean = false,
    val showTripCompletedDialog: Boolean = false,
    val errorMessage: String? = null,
    val isSendingRequest: Boolean = false,
    val routePoints: List<Point> = emptyList(),
    val fromPoint: Point? = null,
    val toPoint: Point? = null,
    val routeDistance: Double = 0.0,
    val fromAddress: String = "Current location",
    val toAddress: String = "",
    val selectedTransport: String? = null,
    val paymentMethod: String? = null,
    val selectedPaymentMethod: String? = null, // Thêm trường này
    val fee: Int = 0,
    val currentRequestId: String? = null,
    val driverId: String? = null,
    val driverLocation: Point? = null,
    val isPending: Boolean = false,
    val selectedMode: String = "Transport",
    val isDrawerOpen: Boolean = false,
    val paymentConfirmed: Boolean = false
)