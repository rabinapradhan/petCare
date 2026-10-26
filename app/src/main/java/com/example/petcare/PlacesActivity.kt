package com.example.petcare

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.example.petcare.data.database.PetCareDatabase
import com.example.petcare.data.entity.Place
import com.example.petcare.data.repository.PlaceRepository

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment

import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place as GooglePlace
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.kotlin.awaitSearchByText
import com.google.android.libraries.places.api.net.kotlin.awaitSearchNearby

import kotlinx.coroutines.launch

import java.util.Locale


class PlacesActivity :
    AppCompatActivity(),
    OnMapReadyCallback {

    companion object {

        private const val LOCATION_REQUEST_CODE = 500

        private const val SEARCH_RADIUS_METERS = 5000.0

        private val PLACE_FIELDS =
            listOf(
                GooglePlace.Field.ID,
                GooglePlace.Field.DISPLAY_NAME,
                GooglePlace.Field.FORMATTED_ADDRESS,
                GooglePlace.Field.LOCATION,
                GooglePlace.Field.INTERNATIONAL_PHONE_NUMBER,
                GooglePlace.Field.OPENING_HOURS
            )
    }

    // =========================================================
    // USER
    // =========================================================

    private var userId: Long = -1L

    // =========================================================
    // GOOGLE MAP
    // =========================================================

    private lateinit var map: GoogleMap

    private lateinit var locationClient:
            FusedLocationProviderClient

    // =========================================================
    // GOOGLE PLACES
    // =========================================================

    private lateinit var placesClient: PlacesClient

    // =========================================================
    // ROOM
    // =========================================================

    private lateinit var placeRepository: PlaceRepository

    // =========================================================
    // VIEWS
    // =========================================================

    private lateinit var placesContainer: LinearLayout

    private lateinit var noPlacesText: TextView

    private lateinit var typeFilterSpinner: Spinner

    // =========================================================
    // DATA
    // =========================================================

    private var allPlaces: List<Place> = emptyList()

    private var selectedFilter = "All places"

    // =========================================================
    // SELECTED GOOGLE PLACE
    // =========================================================

    private var selectedGooglePlace: GooglePlace? = null

    private var selectedGoogleMarker: Marker? = null

    // =========================================================
    // ON CREATE
    // =========================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        userId =
            intent.getLongExtra(
                "user_id",
                -1L
            )

        if (userId == -1L) {
            finish()
            return
        }

        setContentView(
            R.layout.activity_places
        )

        // -----------------------------------------------------
        // ROOM
        // -----------------------------------------------------

        val database =
            PetCareDatabase.getDatabase(
                applicationContext
            )

        placeRepository =
            PlaceRepository(
                database.placeDao()
            )

        // -----------------------------------------------------
        // LOCATION
        // -----------------------------------------------------

        locationClient =
            LocationServices
                .getFusedLocationProviderClient(
                    this
                )

        // -----------------------------------------------------
        // GOOGLE PLACES
        // -----------------------------------------------------

        if (!Places.isInitialized()) {

            Places.initializeWithNewPlacesApiEnabled(
                applicationContext,
                BuildConfig.PLACES_API_KEY
            )
        }

        placesClient =
            Places.createClient(this)

        // -----------------------------------------------------
        // INITIALISE
        // -----------------------------------------------------

        initialiseViews()

        setupFilter()

        setupButtons()

        setupMap()

        observePlaces()
    }

    // =========================================================
    // INITIALISE VIEWS
    // =========================================================

    private fun initialiseViews() {

        placesContainer =
            findViewById(
                R.id.placesContainer
            )

        noPlacesText =
            findViewById(
                R.id.noPlacesText
            )

        typeFilterSpinner =
            findViewById(
                R.id.typeFilterSpinner
            )
    }

    // =========================================================
    // MAP
    // =========================================================

    private fun setupMap() {

        val mapFragment =
            supportFragmentManager
                .findFragmentById(
                    R.id.map
                ) as? SupportMapFragment

        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(
        googleMap: GoogleMap
    ) {

        map = googleMap

        map.uiSettings.isZoomControlsEnabled =
            true

        map.uiSettings.isMapToolbarEnabled =
            false

        // -----------------------------------------------------
        // DEFAULT KATHMANDU LOCATION
        // -----------------------------------------------------

        val defaultLocation =
            LatLng(
                27.7172,
                85.3240
            )

        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                defaultLocation,
                12f
            )
        )

        // -----------------------------------------------------
        // MAP CLICK
        // -----------------------------------------------------

        map.setOnMapClickListener { latLng ->

            showAddPlaceDialog(
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
        }

        // -----------------------------------------------------
        // MARKER CLICK
        // -----------------------------------------------------

        map.setOnMarkerClickListener { marker ->

            val place =
                marker.tag as? Place

            if (place != null) {
                showPlaceDetails(place)
            }

            true
        }

        renderMarkers()
    }

    // =========================================================
    // OBSERVE ROOM
    // =========================================================

    private fun observePlaces() {

        lifecycleScope.launch {

            repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                placeRepository
                    .getPlaces(userId)
                    .collect { places ->

                        allPlaces = places

                        renderPlacesList()

                        renderMarkers()
                    }
            }
        }
    }

    // =========================================================
    // FILTER
    // =========================================================

    private fun setupFilter() {

        val filters =
            arrayOf(
                "All places",
                "Veterinary clinic",
                "Grooming salon",
                "Dog park",
                "Pet supply store",
                "Animal shelter"
            )

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                filters
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        typeFilterSpinner.adapter = adapter

        typeFilterSpinner.setSelection(0)

        typeFilterSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    selectedFilter =
                        filters[position]

                    renderMarkers()

                    renderPlacesList()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private fun setupButtons() {

        findViewById<TextView>(
            R.id.addPlaceButton
        ).setOnClickListener {

            showAddPlaceDialog(
                latitude = null,
                longitude = null
            )
        }

        findViewById<View>(
            R.id.useLocationButton
        ).setOnClickListener {

            requestCurrentLocation()
        }

        findViewById<TextView>(
            R.id.navHome
        ).setOnClickListener {

            openActivity(
                DashboardActivity::class.java
            )
        }

        findViewById<TextView>(
            R.id.navPets
        ).setOnClickListener {

            openActivity(
                PetsActivity::class.java
            )
        }

        findViewById<TextView>(
            R.id.navChecklist
        ).setOnClickListener {

            openActivity(
                ChecklistActivity::class.java
            )
        }

        findViewById<TextView>(
            R.id.navPlaces
        ).setOnClickListener {
            // Already here.
        }

        findViewById<TextView>(
            R.id.navSpend
        ).setOnClickListener {

            openActivity(
                SpendingActivity::class.java
            )
        }
    }

    // =========================================================
    // LOCATION PERMISSION
    // =========================================================

    private fun hasLocationPermission(): Boolean {

        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||

                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_REQUEST_CODE
        )
    }

    // =========================================================
    // CURRENT LOCATION
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocation() {

        if (!hasLocationPermission()) {

            requestLocationPermission()

            return
        }

        if (::map.isInitialized) {

            try {
                map.isMyLocationEnabled = true
            } catch (_: SecurityException) {
            }
        }

        locationClient
            .lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {

                    centerOnLocation(location)

                } else {

                    Toast.makeText(
                        this,
                        "Unable to find your location.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (
            requestCode ==
            LOCATION_REQUEST_CODE
        ) {

            if (
                grantResults.isNotEmpty() &&
                grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {

                requestCurrentLocation()

            } else {

                Toast.makeText(
                    this,
                    "Location permission was denied.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun centerOnLocation(
        location: Location
    ) {

        val latLng =
            LatLng(
                location.latitude,
                location.longitude
            )

        if (::map.isInitialized) {

            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    latLng,
                    15f
                )
            )
        }
    }

    // =========================================================
    // MARKERS
    // =========================================================

    private fun renderMarkers() {

        if (!::map.isInitialized) {
            return
        }

        map.clear()

        selectedGoogleMarker = null

        getFilteredPlaces()
            .forEach { place ->

                val marker =
                    map.addMarker(
                        MarkerOptions()
                            .position(
                                LatLng(
                                    place.latitude,
                                    place.longitude
                                )
                            )
                            .title(place.name)
                            .snippet(place.address)
                            .icon(
                                BitmapDescriptorFactory
                                    .defaultMarker(
                                        getMarkerHue(
                                            place.type
                                        )
                                    )
                            )
                    )

                marker?.tag = place
            }
    }

    private fun getFilteredPlaces():
            List<Place> {

        if (
            selectedFilter ==
            "All places"
        ) {
            return allPlaces
        }

        return allPlaces.filter {
            it.type == selectedFilter
        }
    }

    private fun getMarkerHue(
        type: String
    ): Float {

        return when (type) {

            "Veterinary clinic" ->
                BitmapDescriptorFactory.HUE_RED

            "Grooming salon" ->
                300f

            "Dog park" ->
                BitmapDescriptorFactory.HUE_GREEN

            "Pet supply store" ->
                BitmapDescriptorFactory.HUE_AZURE

            "Animal shelter" ->
                35f

            else ->
                BitmapDescriptorFactory.HUE_GREEN
        }
    }

    // =========================================================
    // PLACE LIST
    // =========================================================

    private fun renderPlacesList() {

        placesContainer.removeAllViews()

        val filtered =
            getFilteredPlaces()

        if (filtered.isEmpty()) {

            noPlacesText.visibility =
                View.VISIBLE

            return
        }

        noPlacesText.visibility =
            View.GONE

        filtered.forEach { place ->

            placesContainer.addView(
                createPlaceCard(place)
            )
        }
    }

    private fun createPlaceCard(
        place: Place
    ): View {

        val view =
            layoutInflater.inflate(
                R.layout.item_place,
                placesContainer,
                false
            )

        val placeCard =
            view.findViewById<View>(
                R.id.placeCard
            )

        val placeNameText =
            view.findViewById<TextView>(
                R.id.placeNameText
            )

        val placeTypeText =
            view.findViewById<TextView>(
                R.id.placeTypeText
            )

        val placeCoordinatesText =
            view.findViewById<TextView>(
                R.id.placeCoordinatesText
            )

        val deletePlaceButton =
            view.findViewById<View>(
                R.id.deletePlaceButton
            )

        placeNameText.text =
            place.name

        placeTypeText.text =
            place.type

        placeCoordinatesText.text =
            String.format(
                Locale.US,
                "%.4f, %.4f",
                place.latitude,
                place.longitude
            )

        placeCard.setOnClickListener {

            moveMapToPlace(place)
        }

        deletePlaceButton.setOnClickListener {

            showDeletePlaceConfirmation(place)
        }

        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        params.bottomMargin = dp(12)

        view.layoutParams = params

        return view
    }

    private fun showDeletePlaceConfirmation(
        place: Place
    ) {

        AlertDialog.Builder(this)
            .setTitle("Delete place?")
            .setMessage(
                "Are you sure you want to delete ${place.name}?"
            )
            .setNegativeButton(
                "Cancel",
                null
            )
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                lifecycleScope.launch {

                    try {

                        placeRepository.deletePlace(
                            place.id,
                            userId
                        )

                        Toast.makeText(
                            this@PlacesActivity,
                            "Place deleted",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (
                        exception: Exception
                    ) {

                        Toast.makeText(
                            this@PlacesActivity,
                            exception.message
                                ?: "Unable to delete place",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun moveMapToPlace(
        place: Place
    ) {

        if (!::map.isInitialized) {
            return
        }

        val location =
            LatLng(
                place.latitude,
                place.longitude
            )

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                location,
                16f
            )
        )
    }

    // =========================================================
    // ADD PLACE DIALOG
    // =========================================================

    private fun showAddPlaceDialog(
        latitude: Double?,
        longitude: Double?
    ) {

        val view =
            LayoutInflater
                .from(this)
                .inflate(
                    R.layout.dialog_add_place,
                    null
                )

        val typeSpinner =
            view.findViewById<Spinner>(
                R.id.placeTypeSpinner
            )

        val searchEditText =
            view.findViewById<EditText>(
                R.id.googleSearchEditText
            )

        val findNearbyButton =
            view.findViewById<TextView>(
                R.id.findNearbyButton
            )

        val nameEditText =
            view.findViewById<EditText>(
                R.id.placeNameEditText
            )

        val addressEditText =
            view.findViewById<EditText>(
                R.id.placeAddressEditText
            )

        val latitudeEditText =
            view.findViewById<EditText>(
                R.id.latitudeEditText
            )

        val longitudeEditText =
            view.findViewById<EditText>(
                R.id.longitudeEditText
            )

        val phoneEditText =
            view.findViewById<EditText>(
                R.id.phoneEditText
            )

        val hoursEditText =
            view.findViewById<EditText>(
                R.id.openingHoursEditText
            )

        // -----------------------------------------------------
        // TYPE
        // -----------------------------------------------------

        setupPlaceTypeSpinner(
            typeSpinner,
            findNearbyButton
        )

        // -----------------------------------------------------
        // MAP PIN COORDINATES
        // -----------------------------------------------------

        if (
            latitude != null &&
            longitude != null
        ) {

            latitudeEditText.setText(
                latitude.toString()
            )

            longitudeEditText.setText(
                longitude.toString()
            )
        }

        // -----------------------------------------------------
        // DIALOG
        // -----------------------------------------------------

        val dialog =
            AlertDialog.Builder(this)
                .setView(view)
                .create()

        // -----------------------------------------------------
        // GOOGLE TEXT SEARCH
        // -----------------------------------------------------

        view.findViewById<TextView>(
            R.id.searchGoogleButton
        ).setOnClickListener {

            val query =
                searchEditText.text
                    .toString()
                    .trim()

            if (query.isBlank()) {

                searchEditText.error =
                    "Enter a place name"

                searchEditText.requestFocus()

                return@setOnClickListener
            }

            searchGooglePlace(
                query = query,
                onPlaceSelected = { place ->

                    fillPlaceForm(
                        place,
                        nameEditText,
                        addressEditText,
                        latitudeEditText,
                        longitudeEditText,
                        phoneEditText,
                        hoursEditText
                    )
                }
            )
        }

        // -----------------------------------------------------
        // FIND NEARBY
        // -----------------------------------------------------

        findNearbyButton.setOnClickListener {

            val selectedType =
                typeSpinner.selectedItem
                    ?.toString()
                    ?: "Veterinary clinic"

            findNearbyPlaces(
                selectedType
            ) { place ->

                fillPlaceForm(
                    place,
                    nameEditText,
                    addressEditText,
                    latitudeEditText,
                    longitudeEditText,
                    phoneEditText,
                    hoursEditText
                )
            }
        }

        // -----------------------------------------------------
        // USE CURRENT LOCATION
        // -----------------------------------------------------

        view.findViewById<View>(
            R.id.useCurrentLocationButton
        ).setOnClickListener {

            requestCurrentLocationForForm(
                latitudeEditText,
                longitudeEditText
            )
        }

        // -----------------------------------------------------
        // SAVE
        // -----------------------------------------------------

        view.findViewById<TextView>(
            R.id.savePlaceButton
        ).setOnClickListener {

            savePlace(
                typeSpinner,
                nameEditText,
                addressEditText,
                latitudeEditText,
                longitudeEditText,
                phoneEditText,
                hoursEditText
            ) {

                dialog.dismiss()
            }
        }

        // -----------------------------------------------------
        // CLOSE
        // -----------------------------------------------------

        view.findViewById<TextView>(
            R.id.closePlaceDialog
        ).setOnClickListener {

            dialog.dismiss()
        }

        // -----------------------------------------------------
        // SHOW
        // -----------------------------------------------------

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        val width =
            (
                    resources.displayMetrics.widthPixels *
                            0.96
                    ).toInt()

        dialog.window?.setLayout(
            width,
            (
                    resources.displayMetrics.heightPixels *
                            0.88
                    ).toInt()
        )
    }

    // =========================================================
    // PLACE TYPE SPINNER
    // =========================================================

    private fun setupPlaceTypeSpinner(
        spinner: Spinner,
        nearbyButton: TextView
    ) {

        val types =
            arrayOf(
                "Veterinary clinic",
                "Grooming salon",
                "Dog park",
                "Pet supply store",
                "Animal shelter"
            )

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                types
            )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = adapter

        // Initial button text
        updateNearbyButtonText(
            nearbyButton,
            types[0]
        )

        // Change button when type changes
        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    updateNearbyButtonText(
                        nearbyButton,
                        types[position]
                    )
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }
    }

    private fun updateNearbyButtonText(
        button: TextView,
        type: String
    ) {

        val pluralText =
            when (type) {

                "Veterinary clinic" ->
                    "veterinary clinics"

                "Grooming salon" ->
                    "grooming salons"

                "Dog park" ->
                    "dog parks"

                "Pet supply store" ->
                    "pet supply stores"

                "Animal shelter" ->
                    "animal shelters"

                else ->
                    "places"
            }

        button.text =
            "Find $pluralText near me"
    }

    // =========================================================
    // GOOGLE TEXT SEARCH
    // =========================================================

    private fun searchGooglePlace(
        query: String,
        onPlaceSelected:
            (GooglePlace) -> Unit
    ) {

        lifecycleScope.launch {

            try {

                val response =
                    placesClient
                        .awaitSearchByText(
                            query,
                            PLACE_FIELDS
                        ) {

                            maxResultCount = 5

                            regionCode = "NP"
                        }

                val places =
                    response.places

                if (places.isEmpty()) {

                    Toast.makeText(
                        this@PlacesActivity,
                        "No places found.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                showSearchResults(
                    places,
                    onPlaceSelected
                )

            } catch (
                exception: Exception
            ) {

                Toast.makeText(
                    this@PlacesActivity,
                    exception.message
                        ?: "Google search failed.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // NEARBY SEARCH
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun findNearbyPlaces(
        type: String,
        onPlaceSelected:
            (GooglePlace) -> Unit
    ) {

        if (!hasLocationPermission()) {

            requestLocationPermission()

            Toast.makeText(
                this,
                "Allow location permission first.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        locationClient
            .lastLocation
            .addOnSuccessListener { location ->

                if (location == null) {

                    Toast.makeText(
                        this,
                        "Unable to find your current location.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                lifecycleScope.launch {

                    try {

                        // -------------------------------------------------
                        // USER LOCATION
                        // -------------------------------------------------

                        val center =
                            LatLng(
                                location.latitude,
                                location.longitude
                            )

                        // -------------------------------------------------
                        // 5 KM SEARCH AREA
                        // -------------------------------------------------

                        val bounds =
                            CircularBounds.newInstance(
                                center,
                                SEARCH_RADIUS_METERS
                            )

                        // -------------------------------------------------
                        // GOOGLE TYPE
                        // -------------------------------------------------

                        val googleType =
                            getGoogleNearbyType(type)

                        // -------------------------------------------------
                        // ANIMAL SHELTER FALLBACK
                        // -------------------------------------------------

                        if (
                            googleType == null
                        ) {

                            searchAnimalSheltersNearby(
                                center,
                                onPlaceSelected
                            )

                            return@launch
                        }

                        // -------------------------------------------------
                        // TRUE NEARBY SEARCH
                        // -------------------------------------------------

                        val response =
                            placesClient
                                .awaitSearchNearby(
                                    bounds,
                                    PLACE_FIELDS
                                ) {

                                    setIncludedTypes(
                                        listOf(
                                            googleType
                                        )
                                    )

                                    setMaxResultCount(10)

                                    setRankPreference(
                                        com.google.android.libraries.places.api.net
                                            .SearchNearbyRequest
                                            .RankPreference
                                            .DISTANCE
                                    )

                                    setRegionCode("NP")
                                }

                        val places =
                            response.places

                        if (places.isEmpty()) {

                            Toast.makeText(
                                this@PlacesActivity,
                                "No $type found within 5 km.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@launch
                        }

                        showSearchResults(
                            places,
                            onPlaceSelected
                        )

                    } catch (
                        exception: Exception
                    ) {

                        Toast.makeText(
                            this@PlacesActivity,
                            exception.message
                                ?: "Nearby search failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    this,
                    "Unable to access your location.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    // =========================================================
    // GOOGLE TYPE MAPPING
    // =========================================================

    private fun getGoogleNearbyType(
        type: String
    ): String? {

        return when (type) {

            "Veterinary clinic" ->
                "veterinary_care"

            "Grooming salon" ->
                "pet_care"

            "Dog park" ->
                "dog_park"

            "Pet supply store" ->
                "pet_store"

            /*
             * animal_shelter is not currently listed as a
             * Table A filtering type in the current Places SDK.
             *
             * Therefore we use a text-search fallback below.
             */
            "Animal shelter" ->
                null

            else ->
                null
        }
    }

    // =========================================================
    // ANIMAL SHELTER FALLBACK
    // =========================================================

    private suspend fun searchAnimalSheltersNearby(
        center: LatLng,
        onPlaceSelected:
            (GooglePlace) -> Unit
    ) {

        try {

            val bounds =
                CircularBounds.newInstance(
                    center,
                    SEARCH_RADIUS_METERS
                )

            val response =
                placesClient
                    .awaitSearchByText(
                        "animal shelter",
                        PLACE_FIELDS
                    ) {

                        maxResultCount = 10

                        locationBias = bounds

                        rankPreference =
                            com.google.android.libraries.places.api.net
                                .SearchByTextRequest
                                .RankPreference
                                .DISTANCE

                        regionCode = "NP"
                    }

            if (response.places.isEmpty()) {

                Toast.makeText(
                    this@PlacesActivity,
                    "No animal shelters found nearby.",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

            showSearchResults(
                response.places,
                onPlaceSelected
            )

        } catch (
            exception: Exception
        ) {

            Toast.makeText(
                this@PlacesActivity,
                exception.message
                    ?: "Animal shelter search failed.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // =========================================================
    // SEARCH RESULTS
    // =========================================================

    private fun showSearchResults(
        places: List<GooglePlace>,
        onPlaceSelected:
            (GooglePlace) -> Unit
    ) {

        val names =
            places.map { place ->

                buildString {

                    append(
                        place.displayName
                            ?: "Unnamed place"
                    )

                    if (
                        !place.formattedAddress
                            .isNullOrBlank()
                    ) {

                        append(
                            "\n${place.formattedAddress}"
                        )
                    }
                }
            }

        AlertDialog.Builder(this)
            .setTitle("Select a place")
            .setItems(
                names.toTypedArray()
            ) { _, which ->

                val selectedPlace =
                    places[which]

                selectedGooglePlace =
                    selectedPlace

                onPlaceSelected(
                    selectedPlace
                )

                showSelectedGooglePlaceOnMap(
                    selectedPlace
                )
            }
            .setNegativeButton(
                "Cancel",
                null
            )
            .show()
    }

    // =========================================================
    // SELECTED GOOGLE PLACE ON MAP
    // =========================================================

    private fun showSelectedGooglePlaceOnMap(
        place: GooglePlace
    ) {

        val location =
            place.location
                ?: return

        if (!::map.isInitialized) {
            return
        }

        val latLng =
            LatLng(
                location.latitude,
                location.longitude
            )

        selectedGoogleMarker?.remove()

        selectedGoogleMarker =
            map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(
                        place.displayName
                            ?: "Selected place"
                    )
                    .snippet(
                        place.formattedAddress
                            ?: ""
                    )
                    .icon(
                        BitmapDescriptorFactory
                            .defaultMarker(
                                BitmapDescriptorFactory
                                    .HUE_GREEN
                            )
                    )
            )

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                latLng,
                16f
            )
        )

        selectedGoogleMarker
            ?.showInfoWindow()
    }

    // =========================================================
    // FILL FORM
    // =========================================================

    private fun fillPlaceForm(
        place: GooglePlace,
        name: EditText,
        address: EditText,
        latitude: EditText,
        longitude: EditText,
        phone: EditText,
        hours: EditText
    ) {

        // Keep the complete selected Google place
        selectedGooglePlace =
            place

        name.setText(
            place.displayName
                ?: ""
        )

        address.setText(
            place.formattedAddress
                ?: ""
        )

        place.location?.let {

            latitude.setText(
                it.latitude.toString()
            )

            longitude.setText(
                it.longitude.toString()
            )
        }

        phone.setText(
            place.internationalPhoneNumber
                ?: ""
        )

        hours.setText(
            place.openingHours
                ?.weekdayText
                ?.joinToString("\n")
                ?: ""
        )

        Toast.makeText(
            this,
            "Place information filled.",
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // CURRENT LOCATION FOR FORM
    // =========================================================

    @SuppressLint("MissingPermission")
    private fun requestCurrentLocationForForm(
        latitude: EditText,
        longitude: EditText
    ) {

        if (!hasLocationPermission()) {

            requestLocationPermission()

            return
        }

        locationClient
            .lastLocation
            .addOnSuccessListener { location ->

                if (location != null) {

                    latitude.setText(
                        location.latitude.toString()
                    )

                    longitude.setText(
                        location.longitude.toString()
                    )

                    if (::map.isInitialized) {

                        centerOnLocation(location)
                    }

                } else {

                    Toast.makeText(
                        this,
                        "Unable to find your location.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // =========================================================
    // SAVE PLACE
    // =========================================================

    private fun savePlace(
        typeSpinner: Spinner,
        name: EditText,
        address: EditText,
        latitude: EditText,
        longitude: EditText,
        phone: EditText,
        hours: EditText,
        onSuccess: () -> Unit
    ) {

        val placeName =
            name.text
                .toString()
                .trim()

        val placeAddress =
            address.text
                .toString()
                .trim()

        val lat =
            latitude.text
                .toString()
                .toDoubleOrNull()

        val lng =
            longitude.text
                .toString()
                .toDoubleOrNull()

        if (placeName.isBlank()) {

            name.error =
                "Enter a place name"

            name.requestFocus()

            return
        }

        if (placeAddress.isBlank()) {

            address.error =
                "Enter an address"

            address.requestFocus()

            return
        }

        if (
            lat == null ||
            lng == null
        ) {

            Toast.makeText(
                this,
                "Valid latitude and longitude are required.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // -----------------------------------------------------
        // GOOGLE PLACE ID
        // -----------------------------------------------------

        val googlePlaceId =
            selectedGooglePlace
                ?.id

        // -----------------------------------------------------
        // ROOM ENTITY
        // -----------------------------------------------------

        val place =
            Place(

                userId = userId,

                googlePlaceId =
                    googlePlaceId,

                name = placeName,

                address = placeAddress,

                latitude = lat,

                longitude = lng,

                phone =
                    phone.text
                        .toString()
                        .trim(),

                openingHours =
                    hours.text
                        .toString()
                        .trim(),

                type =
                    typeSpinner
                        .selectedItem
                        .toString()
            )

        // -----------------------------------------------------
        // SAVE
        // -----------------------------------------------------

        lifecycleScope.launch {

            try {

                placeRepository.addPlace(
                    place
                )

                Toast.makeText(
                    this@PlacesActivity,
                    "Place saved successfully.",
                    Toast.LENGTH_SHORT
                ).show()

                // Reset selected Google place
                selectedGooglePlace = null

                onSuccess()

            } catch (
                exception: Exception
            ) {

                Toast.makeText(
                    this@PlacesActivity,
                    exception.message
                        ?: "Unable to save place.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // =========================================================
    // PLACE DETAILS
    // =========================================================

    private fun showPlaceDetails(
        place: Place
    ) {

        val message =
            buildString {

                append(
                    "${place.type}\n\n"
                )

                if (
                    place.address.isNotBlank()
                ) {

                    append(
                        "${place.address}\n\n"
                    )
                }

                if (
                    place.phone.isNotBlank()
                ) {

                    append(
                        "Phone: ${place.phone}\n\n"
                    )
                }

                if (
                    place.openingHours.isNotBlank()
                ) {

                    append(
                        "Opening hours:\n"
                    )

                    append(
                        place.openingHours
                    )
                }
            }

        AlertDialog.Builder(this)
            .setTitle(place.name)
            .setMessage(message)
            .setNegativeButton(
                "Close",
                null
            )
            .setNeutralButton(
                "Open in Google Maps"
            ) { _, _ ->

                openInGoogleMaps(
                    latitude =
                        place.latitude,

                    longitude =
                        place.longitude,

                    placeName =
                        place.name
                )
            }
            .setPositiveButton(
                "Delete"
            ) { _, _ ->

                lifecycleScope.launch {

                    try {

                        placeRepository.deletePlace(
                            place.id,
                            userId
                        )

                        Toast.makeText(
                            this@PlacesActivity,
                            "Place deleted",
                            Toast.LENGTH_SHORT
                        ).show()

                    } catch (
                        exception: Exception
                    ) {

                        Toast.makeText(
                            this@PlacesActivity,
                            exception.message
                                ?: "Unable to delete place",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    // =========================================================
    // GOOGLE MAPS
    // =========================================================

    private fun openInGoogleMaps(
        latitude: Double,
        longitude: Double,
        placeName: String
    ) {

        val uri =
            Uri.parse(
                "geo:$latitude,$longitude?q=" +
                        "$latitude,$longitude(" +
                        Uri.encode(placeName) +
                        ")"
            )

        val intent =
            Intent(
                Intent.ACTION_VIEW,
                uri
            )

        intent.setPackage(
            "com.google.android.apps.maps"
        )

        try {

            startActivity(intent)

        } catch (
            exception: Exception
        ) {

            val browserUri =
                Uri.parse(
                    "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
                )

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    browserUri
                )
            )
        }
    }

    // =========================================================
    // NAVIGATION
    // =========================================================

    private fun openActivity(
        activityClass: Class<*>
    ) {

        val intent =
            Intent(
                this,
                activityClass
            )

        intent.putExtra(
            "user_id",
            userId
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP

        startActivity(intent)

        finish()
    }

    // =========================================================
    // DP
    // =========================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}
