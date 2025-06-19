package hr.ferit.belic.parking3.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import hr.ferit.belic.parking3.model.ParkingSpot
import hr.ferit.belic.parking3.viewmodel.ParkingViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingScreen(navController: NavController, viewModel: ParkingViewModel = viewModel()) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val parkingData by viewModel.parkingData.collectAsState()
    var hasLocationPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(true) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var mapError by remember { mutableStateOf<String?>(null) }
    var parkedCarLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedParkingSpot by remember { mutableStateOf<ParkingSpot?>(null) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val placesClient = remember { Places.createClient(context) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDropdownMenu by remember { mutableStateOf(false) }


    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            coroutineScope.launch {
                try {
                    val location = fusedLocationClient.lastLocation.await()
                    if (location != null) {
                        userLocation = LatLng(location.latitude, location.longitude)
                        Log.d("ParkingScreen", "User location set: $userLocation")
                    } else {
                        mapError = "Unable to retrieve location"
                        Log.e("ParkingScreen", "Location is null")
                    }
                } catch (e: SecurityException) {
                    mapError = "Location permission denied"
                    Log.e("ParkingScreen", "SecurityException: ${e.message}")
                }
            }
        } else {
            mapError = "Location permission required to show map"
            Log.e("ParkingScreen", "Location permission denied")
        }
    }


    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            mapError = "Notification permission required for timer alerts"
            Log.e("ParkingScreen", "Notification permission denied")
        }
    }


    LaunchedEffect(Unit) {
        hasLocationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                    Log.d("ParkingScreen", "Initial user location: $userLocation")
                } else {
                    mapError = "Unable to retrieve location"
                    Log.e("ParkingScreen", "Initial location is null")
                }
            } catch (e: SecurityException) {
                mapError = "Location permission error"
                Log.e("ParkingScreen", "Initial SecurityException: ${e.message}")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasNotificationPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }


    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation ?: LatLng(45.5548, 18.6954), 15f)
    }


    var parkingMarkers by remember { mutableStateOf(listOf<ParkingSpot>()) }
    var searchQuery by remember { mutableStateOf("") }


    LaunchedEffect(userLocation, cameraPositionState.position.target) {
        if (userLocation != null) {
            coroutineScope.launch {
                try {
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery("parking")
                        .setLocationBias(com.google.android.libraries.places.api.model.RectangularBounds.newInstance(
                            LatLng(userLocation!!.latitude - 0.05, userLocation!!.longitude - 0.05),
                            LatLng(userLocation!!.latitude + 0.05, userLocation!!.longitude + 0.05)
                        ))
                        .setTypeFilter(com.google.android.libraries.places.api.model.TypeFilter.ESTABLISHMENT)
                        .build()
                    val response = placesClient.findAutocompletePredictions(request).await()
                    if (isActive) {
                        parkingMarkers = response.autocompletePredictions.mapNotNull { prediction ->
                            try {
                                val placeRequest = FetchPlaceRequest.builder(
                                    prediction.placeId,
                                    listOf(Place.Field.LAT_LNG, Place.Field.NAME)
                                ).build()
                                val placeResponse = placesClient.fetchPlace(placeRequest).await()
                                placeResponse.place.latLng?.let {
                                    ParkingSpot(it, placeResponse.place.name)
                                }
                            } catch (e: Exception) {
                                Log.e("ParkingScreen", "Error fetching place: ${e.message}")
                                null
                            }
                        }.take(5)
                        Log.d("ParkingScreen", "Parking spots found: ${parkingMarkers.size}")
                        if (parkingMarkers.isEmpty()) {
                            mapError = "No parking spots found nearby"
                        }
                    }
                } catch (e: Exception) {
                    if (isActive) {
                        mapError = "Error fetching parking locations: ${e.message}"
                        Log.e("ParkingScreen", "Error fetching parking: ${e.message}")
                    }
                }
            }
        }
    }


    suspend fun getAddressFromLatLng(latLng: LatLng): String {
        return try {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery("")
                .setLocationBias(com.google.android.libraries.places.api.model.RectangularBounds.newInstance(
                    LatLng(latLng.latitude - 0.01, latLng.longitude - 0.01),
                    LatLng(latLng.latitude + 0.01, latLng.longitude + 0.01)
                ))
                .setTypeFilter(com.google.android.libraries.places.api.model.TypeFilter.ADDRESS)
                .build()
            val response = placesClient.findAutocompletePredictions(request).await()
            val placeId = response.autocompletePredictions.firstOrNull()?.placeId
                ?: return "Unknown address"
            val placeRequest = FetchPlaceRequest.builder(
                placeId,
                listOf(Place.Field.ADDRESS)
            ).build()
            val placeResponse = placesClient.fetchPlace(placeRequest).await()
            placeResponse.place.address ?: "Unknown address"
        } catch (e: Exception) {
            Log.e("ParkingScreen", "Error fetching address: ${e.message}")
            "Unknown address"
        }
    }


    suspend fun saveToHistory(
        latLng: LatLng,
        address: String,
        snackbarHostState: SnackbarResult
    ) {
        val historyData = hashMapOf(
            "adress" to address,
            "date" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "id" to java.util.UUID.randomUUID().toString()
        )

        db.collection("parkingHistory")
            .add(historyData)


    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Parking Locator") },
                actions = {
                    IconButton(onClick = { showDropdownMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    DropdownMenu(
                        expanded = showDropdownMenu,
                        onDismissRequest = { showDropdownMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("History") },
                            onClick = {
                                showDropdownMenu = false
                                navController.navigate("history")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Log Out") },
                            onClick = {
                                showDropdownMenu = false
                                viewModel.logout {
                                    navController.navigate("sign_in") {
                                        popUpTo("sign_in") { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { value: String ->
                    searchQuery = value
                    if (value.isNotEmpty()) {
                        coroutineScope.launch {
                            try {
                                val request = FindAutocompletePredictionsRequest.builder()
                                    .setQuery(value)
                                    .setTypeFilter(com.google.android.libraries.places.api.model.TypeFilter.CITIES)
                                    .build()
                                val response = placesClient.findAutocompletePredictions(request).await()
                                response.autocompletePredictions.firstOrNull()?.let { prediction ->
                                    try {
                                        val placeRequest = FetchPlaceRequest.builder(
                                            prediction.placeId,
                                            listOf(Place.Field.LAT_LNG)
                                        ).build()
                                        val placeResponse = placesClient.fetchPlace(placeRequest).await()
                                        placeResponse.place.latLng?.let { latLng ->
                                            if (isActive) {
                                                userLocation = latLng
                                                cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                                            }
                                        }
                                    } catch (e: Exception) {

                                    }
                                }
                            } catch (e: Exception) {
                                if (isActive) {
                                    mapError = "Error searching city: ${e.message}"
                                    Log.e("ParkingScreen", "Error searching city: ${e.message}")
                                }
                            }
                        }
                    }
                },
                label = { Text("Search for a city") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(8.dp)),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp)
            ) {
                if (mapError != null) {
                    Text(
                        text = mapError ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp)
                    )
                } else {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        properties = MapProperties(
                            isMyLocationEnabled = hasLocationPermission,
                            mapType = MapType.NORMAL
                        ),
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = true,
                            myLocationButtonEnabled = true
                        ),
                        onMapLongClick = { latLng ->
                            parkedCarLocation = latLng
                            selectedParkingSpot = null
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Pinned car location at latitude: ${latLng.latitude}, longitude: ${latLng.longitude}")
                            }
                            Log.d("ParkingScreen", "Pinned car location at: $latLng")
                        }
                    ) {

                        userLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Your Location",
                                snippet = "Current position",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                            )
                        }

                        parkingMarkers.forEach { spot ->
                            Marker(
                                state = MarkerState(position = spot.latLng),
                                title = spot.name ?: "Parking Spot",
                                snippet = "Nearby parking location",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                                onClick = {
                                    selectedParkingSpot = spot
                                    parkedCarLocation = null
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Selected parking spot: ${spot.name ?: "Unknown"}")
                                    }
                                    true
                                }
                            )
                        }

                        parkedCarLocation?.let {
                            Marker(
                                state = MarkerState(position = it),
                                title = "Pinned Car",
                                snippet = "Your selected car location",
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                    }
                }
            }


            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Set Parking Timer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        OutlinedTextField(
                            value = parkingData.hours.toString(),
                            onValueChange = { viewModel.updateHours(it.toIntOrNull() ?: 0) },
                            label = { Text("Hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                        OutlinedTextField(
                            value = parkingData.minutes.toString(),
                            onValueChange = { viewModel.updateMinutes(it.toIntOrNull() ?: 0) },
                            label = { Text("Minutes") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                        OutlinedTextField(
                            value = parkingData.seconds.toString(),
                            onValueChange = { viewModel.updateSeconds(it.toIntOrNull() ?: 0) },
                            label = { Text("Seconds") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(100.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Time Remaining: ${parkingData.timeLeftInSeconds / 3600}h " +
                                "${(parkingData.timeLeftInSeconds % 3600) / 60}m ${parkingData.timeLeftInSeconds % 60}s",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { viewModel.startTimer(context) },
                            enabled = !parkingData.isTimerRunning,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Start Timer", fontSize = 16.sp)
                        }
                        Button(
                            onClick = { viewModel.cancelTimer() },
                            enabled = parkingData.isTimerRunning,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Cancel Timer", fontSize = 16.sp)
                        }
                    }


                    if (parkedCarLocation != null || selectedParkingSpot != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val latLng = parkedCarLocation ?: selectedParkingSpot?.latLng ?: return@launch
                                    val address = selectedParkingSpot?.name ?: getAddressFromLatLng(latLng)
                                    val snackbarHostState = snackbarHostState.showSnackbar("Location saved: $address")
                                    saveToHistory(latLng, address, snackbarHostState)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Text("Save Location", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}