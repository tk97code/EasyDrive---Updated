package com.example.dacs31.ui.screen.customer

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.data.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import com.example.dacs31.ui.screen.location.getRoute
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class CustomerHomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<CustomerHomeUiState>()
    val uiState: LiveData<CustomerHomeUiState> = _uiState

    private var requestListener: ListenerRegistration? = null
    private var driverListener: ListenerRegistration? = null
    private val db = Firebase.firestore
    private val requestsCollection = db.collection("requests")

    init {
        _uiState.value = CustomerHomeUiState()
    }

    fun initializeCustomer() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                Log.d("CustomerHomeViewModel", "User not logged in")
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Please sign in to continue."
                )
                return@launch
            }
            if (user.role != "Customer") {
                Log.d("CustomerHomeViewModel", "User is not a customer, role: ${user.role}")
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Please sign in with a customer account.",
                    user = user
                )
                return@launch
            }
            _uiState.value = _uiState.value?.copy(
                customerId = user.uid,
                user = user
            )
            Log.d("CustomerHomeViewModel", "Customer ID: ${user.uid}")

            // Test Firestore write
            db.collection("test").document("test_message")
                .set(mapOf("message" to "Hello, Firestore!"))
                .addOnSuccessListener {
                    Log.d("FirestoreTest", "Write successful")
                }
                .addOnFailureListener { e ->
                    Log.e("FirestoreTest", "Write failed: ${e.message}")
                }
        }
    }

    fun updateUserLocation(point: Point?) {
        _uiState.value = _uiState.value?.copy(userLocation = point)
        Log.d("CustomerHomeViewModel", "User location updated: $point")
    }

    fun updateDriverLocation(point: Point?) {
        _uiState.value = _uiState.value?.copy(driverLocation = point)
        Log.d("CustomerHomeViewModel", "Driver location updated: $point")
    }

    fun setDriverId(driverId: String?) {
        _uiState.value = _uiState.value?.copy(driverId = driverId)
        Log.d("CustomerHomeViewModel", "Driver ID updated: $driverId")
    }

    fun setPermissionDeniedDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showPermissionDeniedDialog = show)
    }

    fun setSelectAddressDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showSelectAddressDialog = show)
    }

    fun setRouteInfoDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showRouteInfoDialog = show)
    }

    fun setSelectTransportDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showSelectTransportDialog = show)
    }

    fun setPaymentScreen(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showPaymentScreen = show)
    }

    fun setDriverAcceptedDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showDriverAcceptedDialog = show)
    }

    fun setRequestCanceledDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showRequestCanceledDialog = show)
    }

    fun setTripCompletedDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showTripCompletedDialog = show)
    }

    fun setErrorMessage(message: String?) {
        _uiState.value = _uiState.value?.copy(errorMessage = message)
    }

    fun setIsSendingRequest(isSending: Boolean) {
        _uiState.value = _uiState.value?.copy(isSendingRequest = isSending)
    }

    fun setIsPending(isPending: Boolean) {
        _uiState.value = _uiState.value?.copy(isPending = isPending)
    }

    fun setPaymentConfirmed(confirmed: Boolean) {
        _uiState.value = _uiState.value?.copy(paymentConfirmed = confirmed)
    }

    fun clearMapData() {
        _uiState.value = _uiState.value?.copy(routePoints = emptyList())
    }

    fun clearCurrentRequest() {
        _uiState.value = _uiState.value?.copy(
            currentRequestId = null,
            driverId = null,
            driverLocation = null,
            paymentMethod = null,
            fee = 0,
            paymentConfirmed = false
        )
    }

    fun handleAddressSelection(from: Point?, fromAddr: String, to: Point?, toAddr: String, mapboxAccessToken: String) {
        if (from != null && to != null) {
            _uiState.value = _uiState.value?.copy(
                fromPoint = from,
                toPoint = to,
                fromAddress = fromAddr,
                toAddress = toAddr
            )
            viewModelScope.launch {
                try {
                    val (points, distance) = getRoute(from, to, mapboxAccessToken)
                    _uiState.value = _uiState.value?.copy(
                        routePoints = points,
                        routeDistance = distance,
                        showRouteInfoDialog = true
                    )
                    Log.d("CustomerHomeViewModel", "Route points received: $points, Distance: $distance m")
                } catch (e: Exception) {
                    Log.e("MapboxDirections", "Error fetching route: ${e.message}")
                    _uiState.value = _uiState.value?.copy(
                        routePoints = emptyList(),
                        routeDistance = 0.0
                    )
                }
            }
        } else {
            Log.w("CustomerHomeViewModel", "From or To point is null")
        }
    }

    fun selectTransport(transport: String?) {
        _uiState.value = _uiState.value?.copy(
            selectedTransport = transport,
            showSelectTransportDialog = false,
            showPaymentScreen = true
        )
        Log.d("CustomerHomeViewModel", "Transport selected: $transport")
    }

    fun confirmRide(paymentMethod: String, fee: Int) {
        val fromPoint = _uiState.value?.fromPoint
        val toPoint = _uiState.value?.toPoint
        val customerId = _uiState.value?.customerId

        if (fromPoint == null || toPoint == null) {
            _uiState.value = _uiState.value?.copy(
                errorMessage = "Invalid pickup or destination. Please try again.",
                showPaymentScreen = false
            )
            return
        }

        _uiState.value = _uiState.value?.copy(
            isSendingRequest = true,
            paymentMethod = paymentMethod,
            selectedPaymentMethod = paymentMethod, // Lưu phương thức thanh toán
            fee = fee
        )

        val requestData = mapOf(
            "customerId" to customerId,
            "pickupLocation" to mapOf(
                "latitude" to fromPoint.latitude(),
                "longitude" to fromPoint.longitude()
            ),
            "destination" to mapOf(
                "latitude" to toPoint.latitude(),
                "longitude" to toPoint.longitude()
            ),
            "status" to "pending",
            "driverId" to null,
            "createdAt" to Timestamp.now(),
            "paymentMethod" to paymentMethod,
            "fee" to fee,
            "paymentStatus" to "pending"
        )

        viewModelScope.launch {
            try {
                val documentReference = requestsCollection.add(requestData).await()
                val requestId = documentReference.id
                _uiState.value = _uiState.value?.copy(
                    currentRequestId = requestId,
                    isPending = true,
                    isSendingRequest = false
                )
                if (paymentMethod == "SePay") {
                    _uiState.value = _uiState.value?.copy(showPaymentScreen = true)
                }
                Log.d("Firestore", "Ride request sent successfully, requestId: $requestId")
            } catch (e: Exception) {
                Log.e("Firestore", "Failed to send request: ${e.message}")
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Failed to send request: ${e.message}. Please try again.",
                    isSendingRequest = false
                )
            }
        }
    }

    fun listenToRequestStatus() {
        val requestId = _uiState.value?.currentRequestId ?: return
        requestListener?.remove()
        requestListener = requestsCollection.document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error listening to request status: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    val newDriverId = snapshot.getString("driverId")
                    when (status) {
                        "accepted" -> {
                            if (!_uiState.value?.isPending!!) {
                                _uiState.value = _uiState.value?.copy(
                                    showDriverAcceptedDialog = true,
                                    driverId = newDriverId
                                )
                            } else {
                                _uiState.value = _uiState.value?.copy(driverId = newDriverId)
                            }
                        }
                        "canceled" -> {
                            if (!_uiState.value?.isPending!!) {
                                _uiState.value = _uiState.value?.copy(
                                    showRequestCanceledDialog = true,
                                    currentRequestId = null,
                                    driverId = null,
                                    driverLocation = null,
                                    paymentMethod = null,
                                    fee = 0,
                                    paymentConfirmed = false
                                )
                            } else {
                                _uiState.value = _uiState.value?.copy(
                                    currentRequestId = null,
                                    driverId = null,
                                    driverLocation = null,
                                    paymentMethod = null,
                                    fee = 0,
                                    paymentConfirmed = false
                                )
                            }
                        }
                        "completed" -> {
                            requestsCollection.document(requestId).get()
                                .addOnSuccessListener { document ->
                                    if (document.exists()) {
                                        // Optionally delete request
                                    }
                                    _uiState.value = _uiState.value?.copy(
                                        currentRequestId = null,
                                        driverId = null,
                                        driverLocation = null,
                                        showTripCompletedDialog = true,
                                        paymentMethod = null,
                                        fee = 0,
                                        paymentConfirmed = false
                                    )
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error checking document: ${e.message}")
                                    _uiState.value = _uiState.value?.copy(
                                        currentRequestId = null,
                                        driverId = null,
                                        driverLocation = null,
                                        paymentMethod = null,
                                        fee = 0,
                                        paymentConfirmed = false
                                    )
                                }
                        }
                    }
                }
            }
    }

    fun listenToDriverLocation() {
        val driverId = _uiState.value?.driverId ?: return
        driverListener?.remove()
        driverListener = db.collection("drivers").document(driverId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error listening to driver location: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val locationData = snapshot.get("location") as? Map<String, Double>
                    locationData?.let {
                        val latitude = it["latitude"] ?: return@let
                        val longitude = it["longitude"] ?: return@let
                        updateDriverLocation(Point.fromLngLat(longitude, latitude))
                    }
                }
            }
    }

    fun toggleDrawer(isOpen: Boolean) {
        _uiState.value = _uiState.value?.copy(isDrawerOpen = isOpen)
    }

    fun setSelectedMode(mode: String) {
        _uiState.value = _uiState.value?.copy(selectedMode = mode)
    }

    override fun onCleared() {
        requestListener?.remove()
        driverListener?.remove()
        super.onCleared()
    }
}



class CustomerHomeViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerHomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerHomeViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}