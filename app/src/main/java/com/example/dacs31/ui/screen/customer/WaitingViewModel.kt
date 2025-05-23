package com.example.dacs31.ui.screen

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point

data class WaitingUiState(
    val showDriverAcceptedDialog: Boolean = false,
    val showRequestCanceledDialog: Boolean = false,
    val showCancelConfirmationDialog: Boolean = false,
    val nearbyDrivers: List<Point> = emptyList()
)

class WaitingViewModel : ViewModel() {

    private val _uiState = MutableLiveData<WaitingUiState>()
    val uiState: LiveData<WaitingUiState> = _uiState

    private var requestListener: ListenerRegistration? = null
    private var driversListener: ListenerRegistration? = null
    private val db = Firebase.firestore
    private val requestsCollection = db.collection("requests")
    private val driversCollection = db.collection("drivers")

    init {
        _uiState.value = WaitingUiState()
    }

    fun listenToNearbyDrivers() {
        driversListener = driversCollection
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error fetching drivers: ${error.message}")
                    return@addSnapshotListener
                }

                val driverLocations = mutableListOf<Point>()
                snapshot?.documents?.forEach { document ->
                    val locationData = document.get("location") as? Map<String, Double>
                    locationData?.let {
                        val latitude = it["latitude"] ?: return@let
                        val longitude = it["longitude"] ?: return@let
                        driverLocations.add(Point.fromLngLat(longitude, latitude))
                    }
                }
                _uiState.value = _uiState.value?.copy(nearbyDrivers = driverLocations)
                Log.d("WaitingViewModel", "Updated nearby drivers: $driverLocations")
            }
    }

    fun listenToRequestStatus(currentRequestId: String) {
        requestListener?.remove()
        requestListener = requestsCollection.document(currentRequestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Error listening to request status: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    when (status) {
                        "accepted" -> {
                            _uiState.value = _uiState.value?.copy(showDriverAcceptedDialog = true)
                        }
                        "canceled" -> {
                            _uiState.value = _uiState.value?.copy(showRequestCanceledDialog = true)
                            requestsCollection.document(currentRequestId).delete()
                        }
                    }
                }
            }
    }

    fun setDriverAcceptedDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showDriverAcceptedDialog = show)
    }

    fun setRequestCanceledDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showRequestCanceledDialog = show)
    }

    fun setCancelConfirmationDialog(show: Boolean) {
        _uiState.value = _uiState.value?.copy(showCancelConfirmationDialog = show)
    }

    fun cancelRequest(currentRequestId: String, onRequestCanceled: () -> Unit) {
        requestsCollection.document(currentRequestId).delete()
            .addOnSuccessListener {
                Log.d("WaitingViewModel", "Request canceled successfully: $currentRequestId")
                onRequestCanceled()
            }
            .addOnFailureListener { e ->
                Log.e("WaitingViewModel", "Error canceling request: ${e.message}")
            }
    }

    override fun onCleared() {
        requestListener?.remove()
        driversListener?.remove()
        super.onCleared()
    }
}

class WaitingViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaitingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WaitingViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}