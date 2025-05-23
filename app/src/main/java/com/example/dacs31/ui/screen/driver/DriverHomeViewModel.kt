package com.example.dacs31.ui.screen.driver

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.dacs31.data.AuthRepository
import com.example.dacs31.data.User
import com.example.dacs31.ui.screen.location.getRoute
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import kotlinx.coroutines.launch

data class RideRequest(
    val id: String,
    val customerId: String,
    val pickupLocation: Point,
    val destination: Point,
    val status: String,
    val paymentMethod: String = "",
    val fee: Int = 0
)

data class DriverHomeUiState(
    val driverId: String? = null,
    val user: User? = null,
    val userLocation: Point? = null,
    val showPermissionDeniedDialog: Boolean = false,
    val errorMessage: String? = null,
    val isConnected: Boolean = false,
    val incomingRequest: RideRequest? = null,
    val selectedRequest: RideRequest? = null,
    val fromPoint: Point? = null,
    val toPoint: Point? = null,
    val routePoints: List<Point> = emptyList(),
    val showSelectAddressDialog: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val selectedMode: String = "Transport"
)

class DriverHomeViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableLiveData<DriverHomeUiState>()
    val uiState: LiveData<DriverHomeUiState> = _uiState

    private var requestListener: ListenerRegistration? = null
    private val db = Firebase.firestore

    init {
        _uiState.value = DriverHomeUiState()
    }

    fun initializeDriver() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null || currentUser.role != "Driver") {
                Log.e("DriverHomeViewModel", "User is not a driver, role: ${currentUser?.role}")
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Please sign in with a driver account.",
                    user = currentUser
                )
                return@launch
            }
            _uiState.value = _uiState.value?.copy(
                driverId = currentUser.uid,
                user = currentUser
            )
            Log.d("DriverHomeViewModel", "Driver ID: ${currentUser.uid}")
            syncDriverDocument()
            listenToConnectionStatus()
        }
    }

    private fun syncDriverDocument() {
        val driverId = _uiState.value?.driverId ?: return
        val driverRef = db.collection("drivers").document(driverId)
        driverRef.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    driverRef.set(
                        mapOf(
                            "location" to mapOf("latitude" to 0.0, "longitude" to 0.0),
                            "isConnected" to false,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )
                    ).addOnSuccessListener {
                        Log.d("Firestore", "Created new driver document successfully")
                    }.addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to create driver document: ${e.message}")
                        _uiState.value = _uiState.value?.copy(
                            errorMessage = "Failed to create driver document: ${e.message}"
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error checking document: ${e.message}")
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Error checking document: ${e.message}"
                )
            }
    }

    private fun listenToConnectionStatus() {
        val driverId = _uiState.value?.driverId ?: return
        val driverRef = db.collection("drivers").document(driverId)
        driverRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("Firestore", "Error reading isConnected status: ${error.message}")
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Error reading connection status: ${error.message}"
                )
                return@addSnapshotListener
            }
            val value = snapshot?.getBoolean("isConnected") ?: false
            _uiState.value = _uiState.value?.copy(isConnected = value)
            Log.d("Firestore", "Updated isConnected status: $value")
        }
    }

    fun updateUserLocation(point: Point?) {
        _uiState.value = _uiState.value?.copy(userLocation = point)
        if (point != null) {
            val driverId = _uiState.value?.driverId ?: return
            val driverRef = db.collection("drivers").document(driverId)
            val locationData = mapOf(
                "location" to mapOf(
                    "latitude" to point.latitude(),
                    "longitude" to point.longitude()
                ),
                "isConnected" to _uiState.value?.isConnected
            )
            driverRef.set(locationData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("Firestore", "Updated driver location successfully: $point")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Failed to update location: ${e.message}")
                    _uiState.value = _uiState.value?.copy(
                        errorMessage = "Failed to update location: ${e.message}"
                    )
                }
        } else {
            Log.w("DriverHomeViewModel", "userLocation is null, cannot update location")
        }
    }

    fun toggleConnectionStatus() {
        val newStatus = !(_uiState.value?.isConnected ?: false)
        _uiState.value = _uiState.value?.copy(isConnected = newStatus)
        val driverId = _uiState.value?.driverId ?: return
        val driverRef = db.collection("drivers").document(driverId)
        driverRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    driverRef.update("isConnected", newStatus)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Updated isConnected status successfully: $newStatus")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Failed to update status: ${e.message}")
                            _uiState.value = _uiState.value?.copy(
                                errorMessage = "Failed to update status: ${e.message}"
                            )
                        }
                } else {
                    driverRef.set(
                        mapOf(
                            "location" to mapOf("latitude" to 0.0, "longitude" to 0.0),
                            "isConnected" to newStatus,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )
                    ).addOnSuccessListener {
                        Log.d("Firestore", "Created driver document successfully")
                    }.addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to create driver document: ${e.message}")
                        _uiState.value = _uiState.value?.copy(
                            errorMessage = "Failed to create driver document: ${e.message}"
                        )
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error checking document: ${e.message}")
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Error checking document: ${e.message}"
                )
            }
    }

    fun listenToRideRequests(mapboxAccessToken: String) {
        val driverId = _uiState.value?.driverId ?: return
        val isConnected = _uiState.value?.isConnected ?: false
        requestListener?.remove()
        requestListener = null

        if (!isConnected) {
            _uiState.value = _uiState.value?.copy(
                incomingRequest = null,
                selectedRequest = null,
                fromPoint = null,
                toPoint = null,
                routePoints = emptyList()
            )
            Log.d("DriverHomeViewModel", "Driver disconnected, stopping request listener")
            return
        }

        Log.d("DriverHomeViewModel", "Driver connected, starting request listener")
        requestListener = db.collection("requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error listening to requests: ${error.message}")
                    _uiState.value = _uiState.value?.copy(
                        errorMessage = "Failed to load requests: ${error.message}"
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val requestList = snapshot.documents.mapNotNull { doc ->
                        val pickupData = doc.get("pickupLocation") as? Map<String, Double>
                        val destData = doc.get("destination") as? Map<String, Double>
                        val customerId = doc.getString("customerId") ?: return@mapNotNull null
                        val status = doc.getString("status") ?: return@mapNotNull null

                        val pickupLocation = pickupData?.let {
                            Point.fromLngLat(it["longitude"] ?: 0.0, it["latitude"] ?: 0.0)
                        } ?: return@mapNotNull null

                        val destination = destData?.let {
                            Point.fromLngLat(it["longitude"] ?: 0.0, it["latitude"] ?: 0.0)
                        } ?: return@mapNotNull null

                        RideRequest(
                            id = doc.id,
                            customerId = customerId,
                            pickupLocation = pickupLocation,
                            destination = destination,
                            status = status
                        )
                    }

                    if (_uiState.value?.selectedRequest == null) {
                        _uiState.value = _uiState.value?.copy(incomingRequest = requestList.firstOrNull())
                        Log.d("DriverHomeViewModel", "New request: ${_uiState.value?.incomingRequest}")
                    }
                }
            }

        // Fetch route for selected request
        _uiState.value?.selectedRequest?.let { request ->
            _uiState.value = _uiState.value?.copy(
                fromPoint = request.pickupLocation,
                toPoint = request.destination
            )
            viewModelScope.launch {
                try {
                    val (points, _) = getRoute(
                        request.pickupLocation,
                        request.destination,
                        mapboxAccessToken
                    )
                    _uiState.value = _uiState.value?.copy(routePoints = points)
                    Log.d("DriverHomeViewModel", "Route points received: $points")
                } catch (e: Exception) {
                    Log.e("MapboxDirections", "Error fetching route: ${e.message}")
                    _uiState.value = _uiState.value?.copy(routePoints = emptyList())
                }
            }
        }
    }

    fun acceptRideRequest(request: RideRequest?) {
        if (request == null) {
            _uiState.value = _uiState.value?.copy(incomingRequest = null)
            return
        }

        val driverId = _uiState.value?.driverId ?: return
        val requestsCollection = db.collection("requests")
        requestListener?.remove()
        requestsCollection.document(request.id).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val status = document.getString("status")
                    val currentDriverId = document.getString("driverId")
                    if (status == "pending" && currentDriverId == null) {
                        requestsCollection.document(request.id)
                            .update(
                                mapOf(
                                    "status" to "accepted",
                                    "driverId" to driverId
                                )
                            )
                            .addOnSuccessListener {
                                _uiState.value = _uiState.value?.copy(
                                    selectedRequest = request,
                                    incomingRequest = null
                                )
                                Log.d("Firestore", "Accepted request successfully: ${request.id}")
                                listenToRideRequests("") // Re-attach listener (mapboxAccessToken handled in Composable)
                            }
                            .addOnFailureListener { e ->
                                Log.e("Firestore", "Failed to accept request: ${e.message}")
                                _uiState.value = _uiState.value?.copy(
                                    errorMessage = "Failed to accept request: ${e.message}"
                                )
                                listenToRideRequests("") // Re-attach listener
                            }
                    } else {
                        Log.w("Firestore", "Request no longer pending or already assigned")
                        _uiState.value = _uiState.value?.copy(
                            errorMessage = "Request has been processed by another driver.",
                            incomingRequest = null
                        )
                        listenToRideRequests("") // Re-attach listener
                    }
                } else {
                    Log.w("Firestore", "Request does not exist")
                    _uiState.value = _uiState.value?.copy(
                        errorMessage = "Request does not exist.",
                        incomingRequest = null
                    )
                    listenToRideRequests("") // Re-attach listener
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error checking request: ${e.message}")
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Error checking request: ${e.message}",
                    incomingRequest = null
                )
                listenToRideRequests("") // Re-attach listener
            }
    }

    fun declineRideRequest() {
        _uiState.value = _uiState.value?.copy(incomingRequest = null)
    }

    fun completeRide() {
        val request = _uiState.value?.selectedRequest ?: return
        db.collection("requests").document(request.id)
            .update("status", "completed")
            .addOnSuccessListener {
                _uiState.value = _uiState.value?.copy(
                    selectedRequest = null,
                    fromPoint = null,
                    toPoint = null,
                    routePoints = emptyList()
                )
                Log.d("Firestore", "Ride completed successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to complete ride: ${e.message}")
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Failed to complete ride: ${e.message}"
                )
            }
    }

    fun cancelRide() {
        val request = _uiState.value?.selectedRequest ?: return
        db.collection("requests").document(request.id)
            .update("status", "canceled")
            .addOnSuccessListener {
                _uiState.value = _uiState.value?.copy(
                    selectedRequest = null,
                    fromPoint = null,
                    toPoint = null,
                    routePoints = emptyList()
                )
                Log.d("Firestore", "Ride canceled successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to cancel ride: ${e.message}")
                _uiState.value = _uiState.value?.copy(
                    errorMessage = "Failed to cancel ride: ${e.message}"
                )
            }
    }

    fun setShowSelectAddressDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showSelectAddressDialog = show)
    }

    fun handleAddressSelection(from: Point?, to: Point?, mapboxAccessToken: String) {
        if (from != null && to != null) {
            _uiState.value = _uiState.value?.copy(
                fromPoint = from,
                toPoint = to
            )
            viewModelScope.launch {
                try {
                    val (points, _) = getRoute(from, to, mapboxAccessToken)
                    _uiState.value = _uiState.value?.copy(routePoints = points)
                    Log.d("DriverHomeViewModel", "Route points received: $points")
                } catch (e: Exception) {
                    Log.e("MapboxDirections", "Error fetching route: ${e.message}")
                    _uiState.value = _uiState.value?.copy(routePoints = emptyList())
                }
            }
        } else {
            Log.w("DriverHomeViewModel", "From or To point is null")
        }
    }

    fun toggleDrawer(isOpen: Boolean) {
        _uiState.value = _uiState.value?.copy(isDrawerOpen = isOpen)
    }

    fun setPermissionDeniedDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showPermissionDeniedDialog = show)
    }

    fun setErrorMessage(message: String?) {
        _uiState.value = _uiState.value?.copy(errorMessage = message)
    }

    override fun onCleared() {
        requestListener?.remove()
        super.onCleared()
    }
}

class DriverHomeViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DriverHomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DriverHomeViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}