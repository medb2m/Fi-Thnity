package tn.esprit.fithnity.ui.rides

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// Firebase removed - using JWT tokens instead
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import tn.esprit.fithnity.data.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

sealed class RideUiState {
    object Idle : RideUiState()
    object Loading : RideUiState()
    data class Success(val rides: List<RideResponse>) : RideUiState()
    data class Error(val message: String) : RideUiState()
}

sealed class CreateRideUiState {
    object Idle : CreateRideUiState()
    object Loading : CreateRideUiState()
    data class Success(val ride: RideResponse) : CreateRideUiState()
    data class Error(val message: String) : CreateRideUiState()
}

class RideViewModel : ViewModel() {
    private val api = NetworkModule.rideApi
    private val TAG = "RideViewModel"

    private val _uiState = MutableStateFlow<RideUiState>(RideUiState.Idle)
    val uiState: StateFlow<RideUiState> = _uiState.asStateFlow()

    private val _createRideState = MutableStateFlow<CreateRideUiState>(CreateRideUiState.Idle)
    val createRideState: StateFlow<CreateRideUiState> = _createRideState.asStateFlow()

    /**
     * Create a new ride (offer or request)
     */
    fun createRide(
        authToken: String?,
        rideType: String, // "REQUEST" or "OFFER"
        transportType: String, // "TAXI", "TAXI_COLLECTIF", "PRIVATE_CAR", "METRO", "BUS"
        origin: Location,
        destination: Location,
        availableSeats: Int? = null,
        notes: String? = null,
        departureDate: String? = null, // ISO 8601 format
        price: Double? = null // Required for TAXI/TAXI_COLLECTIF
    ) = viewModelScope.launch {
        Log.d(TAG, "createRide: Starting ride creation")
        _createRideState.value = CreateRideUiState.Loading

        try {
            val token = authToken
            if (token == null) {
                _createRideState.value = CreateRideUiState.Error("Not authenticated. Please sign in.")
                return@launch
            }

            val request = CreateRideRequest(
                rideType = rideType,
                transportType = transportType,
                origin = origin,
                destination = destination,
                availableSeats = availableSeats,
                notes = notes,
                departureDate = departureDate,
                price = price
            )

            val response = api.createRide(bearer = "Bearer $token", request = request)
            Log.d(TAG, "createRide: Response received - success: ${response.success}")

            if (response.success && response.data != null) {
                Log.d(TAG, "createRide: Ride created successfully - ${response.data._id}")
                _createRideState.value = CreateRideUiState.Success(response.data)
                // Refresh rides list
                loadRides()
            } else {
                val errorMsg = response.message ?: response.error ?: "Failed to create ride"
                Log.e(TAG, "createRide: Failed - $errorMsg")
                _createRideState.value = CreateRideUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "createRide: Exception occurred", e)
            _createRideState.value = CreateRideUiState.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Load all active rides with optional filtering
     */
    fun loadRides(
        rideType: String? = null, // "REQUEST" or "OFFER"
        transportType: String? = null, // "TAXI", "TAXI_COLLECTIF", "PRIVATE_CAR", "METRO", "BUS"
        page: Int = 1,
        limit: Int = 20
    ) = viewModelScope.launch {
        Log.d(TAG, "loadRides: Loading rides")
        _uiState.value = RideUiState.Loading

        try {
            val response = api.getRides(rideType = rideType, transportType = transportType, page = page, limit = limit)
            Log.d(TAG, "loadRides: Response received - success: ${response.success}")

            if (response.success && response.data != null) {
                val rides = response.data.data
                Log.d(TAG, "loadRides: Loaded ${rides.size} rides")
                _uiState.value = RideUiState.Success(rides)
            } else {
                val errorMsg = response.message ?: response.error ?: "Failed to load rides"
                Log.e(TAG, "loadRides: Failed - $errorMsg")
                _uiState.value = RideUiState.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "loadRides: Exception occurred", e)
            _uiState.value = RideUiState.Error(e.message ?: "Unknown error occurred")
        }
    }

    /**
     * Reset create ride state
     */
    fun resetCreateRideState() {
        _createRideState.value = CreateRideUiState.Idle
    }
}

