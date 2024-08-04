package com.enigma.parapo.fragments

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AnticipateOvershootInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SnapHelper
import com.enigma.parapo.R
import com.enigma.parapo.adapters.LocationRecyclerViewAdapter
import com.enigma.parapo.databinding.FragmentMapBinding
import com.enigma.parapo.models.IndividualLocation
import com.enigma.parapo.utils.LinearLayoutManagerWithSmoothScroller
import com.enigma.parapo.utils.dpToPx
import com.enigma.parapo.utils.isPermissionGranted
import com.enigma.parapo.utils.shareIntent
import com.enigma.parapo.utils.userDistanceTo
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.matching.v5.MapboxMapMatching
import com.mapbox.api.matching.v5.models.MapMatchingResponse
import com.mapbox.common.location.LocationProvider
import com.mapbox.core.constants.Constants.PRECISION_6
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.BuildConfig
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.literal
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.LineLayer
import com.mapbox.maps.extension.style.layers.generated.SymbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.getSourceAs
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions.Companion.cameraAnimatorOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.viewport.viewport
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.search.ApiType
import com.mapbox.search.ResponseInfo
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchEngineSettings
import com.mapbox.search.offline.OfflineResponseInfo
import com.mapbox.search.offline.OfflineSearchEngine
import com.mapbox.search.offline.OfflineSearchEngineSettings
import com.mapbox.search.offline.OfflineSearchResult
import com.mapbox.search.record.HistoryRecord
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.mapbox.search.ui.adapter.engines.SearchEngineUiAdapter
import com.mapbox.search.ui.view.CommonSearchViewConfiguration
import com.mapbox.search.ui.view.DistanceUnitType
import com.mapbox.search.ui.view.SearchResultAdapterItem
import com.mapbox.search.ui.view.SearchResultsView
import com.mapbox.search.ui.view.place.SearchPlace
import com.mapbox.search.ui.view.place.SearchPlaceBottomSheetView
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfConversion
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.DecimalFormat


class MapFragment : Fragment(), LocationRecyclerViewAdapter.ClickListener {
    private var _binding: FragmentMapBinding? = null

    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    // Marker is selected
    private val PROPERTY_SELECTED = "selected"

    // Keeps track of the previously selected terminal index
    private var prevSelectedIndex = 0

    private val NAVIGATION_LINE_WIDTH = 9.0

    private lateinit var locationProvider: LocationProvider
    private var userLocation: Point? = null

    private lateinit var toolbar: Toolbar
    private lateinit var searchView: SearchView

    private lateinit var searchResultsView: SearchResultsView
    private lateinit var searchEngineUiAdapter: SearchEngineUiAdapter
    private lateinit var searchPlaceView: SearchPlaceBottomSheetView

    private lateinit var focusLocationView: FloatingActionButton

    private lateinit var mapView: MapView
    private lateinit var mapMarkersManager: MapMarkersManager

    private lateinit var featureCollection: FeatureCollection
    private lateinit var listOfIndividualLocations: ArrayList<IndividualLocation>
    private lateinit var styleRvAdapter: LocationRecyclerViewAdapter
    private lateinit var locationsRecyclerView: RecyclerView

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when {
                !searchPlaceView.isHidden() -> {
                    mapMarkersManager.clearMarkers()
                    searchPlaceView.hide()

                    // Show nav view
                    searchPlaceListener?.showNavView()
                }

                mapMarkersManager.hasMarkers -> {
                    mapMarkersManager.clearMarkers()
                }

                else -> {
                    if (BuildConfig.DEBUG) {
                        error("This OnBackPressedCallback should not be enabled")
                    }
                    Log.i("SearchApiExample", "This OnBackPressedCallback should not be enabled")
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        }
    }

    // Define the interface for hiding the nav view after searching a place
    interface OnSearchPlaceListener {
        fun hideNavView()
        fun showNavView()
    }

    // Instance of the listener
    private var searchPlaceListener: OnSearchPlaceListener? = null

    // Get the user's location as coordinates
    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        mapView.mapboxMap.setCamera(CameraOptions.Builder().bearing(it).build())
    }

    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView.mapboxMap.setCamera(CameraOptions.Builder().center(it).build())
        mapView.gestures.focalPoint = mapView.mapboxMap.pixelForCoordinate(it)
    }

    // Shows if onIndicatorPositionChangedListener is added
    private var hasOnIndicatorPositionChangedListener = false

    // Removes the onIndicatorPositionChangedListener and hides the focus location button when the map is moved
    private val moveListener: OnMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            if (hasOnIndicatorPositionChangedListener) {
                mapView.location
                    .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                hasOnIndicatorPositionChangedListener = false
            }
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            focusLocationView.hide()
            return false
        }

        // Shows the focus location button after the map is moved
        override fun onMoveEnd(detector: MoveGestureDetector) {
            focusLocationView.show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // This line is important to notify the system that the fragment has an options menu

        // Prompt the user to grant location permissions
        if (!requireActivity().isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                PERMISSIONS_REQUEST_LOCATION
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        val view = binding.root

        // Create a GeoJSON feature collection from the GeoJSON file in the assets folder.
        try {
            getFeatureCollectionFromJson()
        } catch (exception: java.lang.Exception) {
            Log.e("MapFragment", "onCreate: $exception")
            Toast.makeText(requireActivity(), R.string.failure_to_load_file, Toast.LENGTH_LONG)
                .show()
        }

        // Initialize a list of IndividualLocation objects for future use with recyclerview
        listOfIndividualLocations = ArrayList()

        // Makes screen fullscreen
        requireActivity().window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // Add custom OnBackPressedCallback for search place view
        requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)

        // Initialize Mapbox map
        mapView = binding.mapView
        mapView.apply {
            logo.marginBottom = 120F
            attribution.marginBottom = 120F
            compass.enabled = false
            scalebar.enabled = false
            mapboxMap.loadStyle(Style.STANDARD)

            // Adds an onMoveListener to the MapView
            gestures.addOnMoveListener(moveListener)

            // Check if the style is loaded before adding terminal icons
            mapboxMap.subscribeStyleLoaded {
                // Set up the SymbolLayer which will show the icons for each terminal location
                initTerminalLocationIconSymbolLayer()

                // Set up the SymbolLayer which will show the selected terminal icon
                initSelectedStoreSymbolLayer()

                // Set up the LineLayer which will show the navigation route line to a particular terminal location
                initRoutePolylineLineLayer(
                    "navigation-route-source-id",
                    "navigation-route-layer-id",
                    R.color.navigationRouteLine_default
                )

                // Set up the LineLayer which will show the jeepney route line from a particular terminal location
                initRoutePolylineLineLayer(
                    "jeepney-route-source-id",
                    "jeepney-route-layer-id",
                    R.color.jeepneyRouteLine_default
                )

                // Logs all the map layers
                // enumerateMapLayers(mapboxMap.style)

                // Create a list of features from the feature collection
                val featureList: List<Feature>? = featureCollection.features()

                // Retrieve and update the source designated for showing the terminal location icons
                mapboxMap.style?.getSourceAs<GeoJsonSource>("terminal-location-source-id")
                    ?.featureCollection(FeatureCollection.fromFeatures(featureList!!))

                if (featureList != null) {
                    for (x in featureList.indices) {
                        val singleLocation = featureList[x]

                        // Get the single location's String properties to place in its map marker
                        val singleLocationName = singleLocation.getStringProperty("name")
                        val singleLocationDropOff = singleLocation.getStringProperty("destinations")
                        val singleLocationDescription = singleLocation.getStringProperty("drop-off")
                        val singleLocationFare = singleLocation.getStringProperty("fare")
                        val singleLocationRoute = singleLocation.getStringProperty("route")


                        // Add a boolean property to use for adjusting the icon of the selected terminal location
                        singleLocation.addBooleanProperty(PROPERTY_SELECTED, false)

                        // Create a new LineString object
                        var singleLocationRouteLine: LineString? = null

                        // Generate a LineString from the single location's route coordinates
                        if (singleLocationRoute.isNotEmpty()) {
                            singleLocationRouteLine = LineString.fromJson(singleLocationRoute)
                        }

                        // Get the single location's LngLat coordinates
                        val singleLocationPosition = singleLocation.geometry() as Point?

                        // Create a new LngLat object with the Position object created above
                        val singleLocationLngLat = Point.fromLngLat(
                            singleLocationPosition!!.longitude(),
                            singleLocationPosition.latitude()
                        )

                        // Add the location to the Arraylist of locations for later use in the recyclerview
                        listOfIndividualLocations.add(
                            IndividualLocation(
                                singleLocationName,
                                singleLocationDescription,
                                singleLocationDropOff,
                                singleLocationFare,
                                singleLocationRouteLine,
                                singleLocationLngLat
                            )
                        )
                    }

                    // Hide the location cards initially
                    val chosenTheme = R.style.AppTheme_Default
                    setUpRecyclerViewOfLocationCards(chosenTheme)
                    locationsRecyclerView.visibility = View.GONE

                    mapboxMap.addOnMapClickListener { point ->
                        val screenCoordinate = mapboxMap.pixelForCoordinate(point)
                        handleClickIcon(RenderedQueryGeometry(screenCoordinate))
                        true
                    }
                }
            }
        }

        // Set up a location request
        val locationRequest =
            LocationRequest.Builder(LocationRequest.PRIORITY_HIGH_ACCURACY, 10000).build()

        // Get current location settings
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client: SettingsClient = LocationServices.getSettingsClient(requireActivity())
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        // Prompt the user to change the location settings
        // to enable GPS system setting
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }

        mapMarkersManager = MapMarkersManager(mapView)
        mapMarkersManager.onMarkersChangeListener = {
            updateOnBackPressedCallbackEnabled()
        }

        toolbar = binding.toolbar
        toolbar.apply {
            setTitle(" ")
            (requireActivity() as AppCompatActivity).setSupportActionBar(this)
        }

        searchResultsView = binding.searchResultsView.apply {
            initialize(
                SearchResultsView.Configuration(CommonSearchViewConfiguration(DistanceUnitType.IMPERIAL))
            )
            isVisible = false
        }

        val searchEngineSettings = SearchEngineSettings()
        locationProvider = searchEngineSettings.locationProvider
            ?: throw IllegalStateException("No location provider found")

        // Show and update user location if location permissions were granted
        // and location provider is initialized
        if (requireActivity().isPermissionGranted(Manifest.permission.ACCESS_FINE_LOCATION)) {
            showUserLocation()
            updateUserLocation()
        }

        val searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
            apiType = ApiType.GEOCODING,
            settings = searchEngineSettings
        )

        val offlineSearchEngine = OfflineSearchEngine.create(
            OfflineSearchEngineSettings(locationProvider = locationProvider)
        )

        searchEngineUiAdapter = SearchEngineUiAdapter(
            view = searchResultsView,
            searchEngine = searchEngine,
            offlineSearchEngine = offlineSearchEngine,
        )

        searchEngineUiAdapter.addSearchListener(object : SearchEngineUiAdapter.SearchListener {

            override fun onSuggestionsShown(
                suggestions: List<SearchSuggestion>,
                responseInfo: ResponseInfo
            ) {
                // Nothing to do
            }

            override fun onSearchResultsShown(
                suggestion: SearchSuggestion,
                results: List<SearchResult>,
                responseInfo: ResponseInfo
            ) {
                closeSearchView()
                mapMarkersManager.showMarkers(results.map { it.coordinate })
            }

            override fun onOfflineSearchResultsShown(
                results: List<OfflineSearchResult>,
                responseInfo: OfflineResponseInfo
            ) {
                closeSearchView()
                mapMarkersManager.showMarkers(results.map { it.coordinate })
            }

            override fun onSuggestionSelected(searchSuggestion: SearchSuggestion): Boolean {
                return false
            }

            override fun onSearchResultSelected(
                searchResult: SearchResult,
                responseInfo: ResponseInfo
            ) {
                closeSearchView()
                searchPlaceView.open(SearchPlace.createFromSearchResult(searchResult, responseInfo))
                mapMarkersManager.showMarker(searchResult.coordinate)
            }

            override fun onOfflineSearchResultSelected(
                searchResult: OfflineSearchResult,
                responseInfo: OfflineResponseInfo
            ) {
                closeSearchView()
                searchPlaceView.open(SearchPlace.createFromOfflineSearchResult(searchResult))
                mapMarkersManager.showMarker(searchResult.coordinate)
            }

            override fun onError(e: Exception) {
                Toast.makeText(requireContext(), "Error happened: $e", Toast.LENGTH_SHORT).show()
            }

            override fun onHistoryItemClick(historyRecord: HistoryRecord) {
                closeSearchView()
                searchPlaceView.open(
                    SearchPlace.createFromIndexableRecord(
                        historyRecord,
                        distanceMeters = null
                    )
                )

                locationProvider.userDistanceTo(
                    requireActivity(),
                    historyRecord.coordinate
                ) { distance ->
                    distance?.let {
                        searchPlaceView.updateDistance(distance)
                    }
                }

                mapMarkersManager.showMarker(historyRecord.coordinate)
            }

            override fun onPopulateQueryClick(
                suggestion: SearchSuggestion,
                responseInfo: ResponseInfo
            ) {
                if (::searchView.isInitialized) {
                    searchView.setQuery(suggestion.name, true)
                }
            }

            override fun onFeedbackItemClick(responseInfo: ResponseInfo) {
                // Not implemented
            }
        })

        searchResultsView.addActionListener(object : SearchResultsView.ActionListener {
            override fun onErrorItemClick(item: SearchResultAdapterItem.Error) {
                // Handle error item click
            }

            override fun onHistoryItemClick(item: SearchResultAdapterItem.History) {
                // Handle history item click
            }

            override fun onMissingResultFeedbackClick(item: SearchResultAdapterItem.MissingResultFeedback) {
                // Handle missing result feedback click
            }

            override fun onPopulateQueryClick(item: SearchResultAdapterItem.Result) {
                // Handle populate query click
            }

            override fun onResultItemClick(item: SearchResultAdapterItem.Result) {
                val payload = item.payload as List<*>
                val payloadIndex = payload[0] as Int
                val payloadGeometry = payload[1] as Point

                // Set selected terminal
                setFeatureSelectState(featureCollection.features()!![prevSelectedIndex], false)
                setSelected(payloadIndex)
                repositionMapCamera(payloadGeometry)

                // Show the location card
                Toast.makeText(activity, "Click on a card", Toast.LENGTH_SHORT)
                    .show()
                locationsRecyclerView.visibility = View.VISIBLE
                locationsRecyclerView.scrollToPosition(payloadIndex)

                if (deviceHasInternetConnection()) {
                    // Start call to the Mapbox Directions API
                    getInformationFromDirectionsApi(payloadGeometry, payloadIndex)

                    // Start call to the Mapbox Map Matching API if a route exists
                    val selectedLocation = listOfIndividualLocations[payloadIndex]
                    if (selectedLocation.route != null) {
                        getInformationFromMapMatchingApi(selectedLocation.route)
                    }
                } else {
                    Toast.makeText(activity, R.string.no_internet_message, Toast.LENGTH_LONG).show()
                }

                // Close the search view
                toolbar.collapseActionView()
            }
        })

        searchPlaceView = binding.searchPlaceView
        searchPlaceView.initialize(CommonSearchViewConfiguration(DistanceUnitType.IMPERIAL))

        searchPlaceView.addOnCloseClickListener {
            mapMarkersManager.clearMarkers()
            searchPlaceView.hide()
            trackUserLocation()

            // Remove navigation route
            removeSourceFeatures(
                "navigation-route-source-id",
                listOf("navigation-route-feature-id")
            )

            // Remove jeepney route
            removeSourceFeatures("jeepney-route-source-id", listOf("jeepney-route-feature-id"))

            // Show nav view
            searchPlaceListener?.showNavView()
        }

        searchPlaceView.addOnNavigateClickListener { searchPlace ->
            trackUserLocation()
            updateUserLocation()
            navigateToSearchCoordinate(searchPlace.coordinate)
            //startActivity(geoIntent(searchPlace.coordinate))
        }

        searchPlaceView.addOnShareClickListener { searchPlace ->
            startActivity(shareIntent(searchPlace))
        }

        searchPlaceView.addOnFeedbackClickListener { _, _ ->
            // Not implemented
        }

        searchPlaceView.addOnBottomSheetStateChangedListener { _, _ ->
            updateOnBackPressedCallbackEnabled()
        }

        // Initially, the focus location button was hidden
        focusLocationView = binding.focusLocation
        focusLocationView.hide()

        // Adds the onIndicatorPositionChangedListener and zooms in
        // when the focus location button is clicked then hides the button
        focusLocationView.setOnClickListener {
            if (!hasOnIndicatorPositionChangedListener) {
                mapView.location
                    .addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
                hasOnIndicatorPositionChangedListener = true
            }

            focusLocationView.hide()
            mapView.mapboxMap.setCamera(CameraOptions.Builder().zoom(16.35).build())
        }

        return view
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            // Check if the requested permissions were granted
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permissions granted, proceed with showing the user location
                showUserLocation()
            } else {
                // Permissions denied, show toast message
                Toast.makeText(
                    requireContext(),
                    "You need to accept location permissions to show your location on the map.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun handleClickIcon(screenPoint: RenderedQueryGeometry) {
        // Remove existing jeepney route line on the map
        removeSourceFeatures("jeepney-route-source-id", listOf("jeepney-route-feature-id"))

        mapView.mapboxMap.queryRenderedFeatures(
            screenPoint,
            RenderedQueryOptions(listOf("terminal-location-layer-id"), null)
        ) { features ->
            if (!features.value?.isEmpty()!!) {
                //val name = features[0].getStringProperty("name")
                val name =
                    features.value?.get(0)?.queriedFeature?.feature?.getStringProperty("name")
                val featureList = featureCollection.features()
                for (i in featureList!!.indices) {
                    if (featureList[i].getStringProperty("name").equals(name)) {
                        val selectedFeaturePoint = featureList[i].geometry() as Point?

                        if (!featureSelectStatus(i)) {
                            setSelected(i)
                        }

                        for (x in featureCollection.features()!!.indices) {
                            if (listOfIndividualLocations[x].location
                                    .latitude() == selectedFeaturePoint!!.latitude()
                            ) {
                                // Show the location cards and scroll the recyclerview to the selected marker's card.
                                Toast.makeText(activity, "Click on a card", Toast.LENGTH_SHORT)
                                    .show()
                                locationsRecyclerView.visibility = View.VISIBLE

                                // Removed smooth scrolling when initially displaying the card for a
                                // tapped terminal to prevent overly long scroll durations
                                // locationsRecyclerView.smoothScrollToPosition(x)
                                locationsRecyclerView.scrollToPosition(x)

                                // Reposition the map camera target to the selected marker
                                repositionMapCamera(selectedFeaturePoint)

                                // Check for an internet connection before making the call to Mapbox Directions API
                                if (deviceHasInternetConnection()) {
                                    // Start call to the Mapbox Directions API
                                    getInformationFromDirectionsApi(
                                        selectedFeaturePoint,
                                        x
                                    )

                                    // Start call to the Mapbox Map Matching API if a route exists
                                    if (listOfIndividualLocations[x].route != null) {
                                        getInformationFromMapMatchingApi(listOfIndividualLocations[x].route)
                                    }
                                } else {
                                    Toast.makeText(
                                        activity,
                                        R.string.no_internet_message,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }

                    } else {
                        setFeatureSelectState(featureList[i], false)
                    }
                }

                // Hide search place bottom sheet when the markers are clicked
                // and show nav view
                if (!searchPlaceView.isHidden()) {
                    searchPlaceView.hide()
                    mapMarkersManager.clearMarkers()
                    searchPlaceListener?.showNavView()
                }
            } else {
                // Remove route lines on the map and hide the location cards
                // when clicking outside the markers
                setFeatureSelectState(featureCollection.features()!![prevSelectedIndex], false)
                removeSourceFeatures(
                    "navigation-route-source-id",
                    listOf("navigation-route-feature-id")
                )
                removeSourceFeatures("jeepney-route-source-id", listOf("jeepney-route-feature-id"))
                locationsRecyclerView.visibility = View.GONE
            }
        }
    }

    private fun showUserLocation() {
        // Initialize the location puck
        mapView.apply {
            location.locationPuck = createDefault2DPuck(withBearing = true)
            location.enabled = true
            location.pulsingEnabled = true
            location.puckBearing = PuckBearing.COURSE
            location.addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            location.addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
            hasOnIndicatorPositionChangedListener = true
        }

        trackUserLocation()
    }

    private fun trackUserLocation() {
        // Set camera based on device location
        mapView.viewport.transitionTo(
            targetState = mapView.viewport.makeFollowPuckViewportState(),
            transition = mapView.viewport.makeImmediateViewportTransition()
        )
    }

    /**
     * The LocationRecyclerViewAdapter's interface which listens to clicks on each location's card
     *
     * @param position the clicked card's position/index in the overall list of cards
     */
    override fun onItemClick(position: Int) {
        // Remove existing jeepney route line on the map
        removeSourceFeatures("jeepney-route-source-id", listOf("jeepney-route-feature-id"))

        // Get the selected individual location via its card's position in the recyclerview of cards
        val selectedLocation = listOfIndividualLocations[position]

        // Evaluate each Feature's "select state" to appropriately style the location's icon
        val featureList = featureCollection.features()
        val selectedLocationPoint = featureCollection.features()!![position].geometry() as Point?
        for (i in featureList!!.indices) {
            if (featureList[i].getStringProperty("name") == selectedLocation.name
            ) {
                if (!featureSelectStatus(i)) {
                    setSelected(i)
                }
            } else {
                setFeatureSelectState(featureList[i], false)
            }
        }

        // Reposition the map camera target to the selected marker
        if (selectedLocationPoint != null) {
            repositionMapCamera(selectedLocationPoint)
        }

        // Check for an internet connection before making the call to Mapbox Directions API
        if (deviceHasInternetConnection()) {
            // Start call to the Mapbox Directions API
            getInformationFromDirectionsApi(selectedLocationPoint!!, position)

            // Start call to the Mapbox Map Matching API if a route exists
            if (selectedLocation.route != null) {
                getInformationFromMapMatchingApi(selectedLocation.route)
            }
        } else {
            Toast.makeText(activity, R.string.no_internet_message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.location
            .addOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .addOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        hasOnIndicatorPositionChangedListener = true
    }

    override fun onStop() {
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        hasOnIndicatorPositionChangedListener = false
        mapView.gestures.removeOnMoveListener(moveListener)
        focusLocationView.setOnClickListener(null)
        super.onStop()
    }

    // Override onAttach to make sure the hosting activity implements the listener interface
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSearchPlaceListener) {
            searchPlaceListener = context
        } else {
            throw RuntimeException("$context must implement OnSearchPlaceListener")
        }
    }

    // Override onDetach to release the listener when the fragment is detached
    override fun onDetach() {
        searchPlaceListener = null
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        super.onDetach()
    }

    private fun updateOnBackPressedCallbackEnabled() {
        onBackPressedCallback.isEnabled =
            !searchPlaceView.isHidden() || mapMarkersManager.hasMarkers
    }

    private fun getFeatureCollectionFromJson() {
        try {
            featureCollection =
                FeatureCollection.fromJson(loadGeoJsonFromAsset("terminals.geojson")!!)
        } catch (exception: Exception) {
            Log.e("MapFragment", "getFeatureCollectionFromJson: ${exception.message}")
        }
    }

    private fun loadGeoJsonFromAsset(filename: String): String? {
        return try {
            // Load the GeoJSON file from the local asset folder
            val inputStream = requireActivity().assets.open(filename)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, charset("UTF-8"))
        } catch (exception: java.lang.Exception) {
            Log.e("MapFragment", "Exception Loading GeoJSON: $exception")
            exception.printStackTrace()
            null
        }
    }

    /**
     * Adds a SymbolLayer which will show all the location's icons
     */
    private fun initTerminalLocationIconSymbolLayer() {
        val style: Style? = mapView.mapboxMap.style
        if (style != null) {
            // Add the icon image to the map
            style.addImage(
                "terminal-location-icon-id",
                BitmapFactory.decodeResource(
                    requireContext().resources,
                    R.drawable.default_theme_icon
                )
            )

            // Create and add the GeoJsonSource to the map
            val terminalLocationGeoJsonSource =
                GeoJsonSource.Builder("terminal-location-source-id").build()
            style.addSource(terminalLocationGeoJsonSource)

            // Create and add the terminal location icon SymbolLayer to the map
            val terminalLocationSymbolLayer = SymbolLayer(
                "terminal-location-layer-id",
                "terminal-location-source-id"
            )
            terminalLocationSymbolLayer.apply {
                iconImage("terminal-location-icon-id")
                iconAllowOverlap(true)
                iconIgnorePlacement(true)
            }
            style.addLayer(terminalLocationSymbolLayer)
        } else {
            Log.d(
                "TerminalFinderActivity",
                "initTerminalLocationIconSymbolLayer: Style isn't ready yet."
            )

            throw java.lang.IllegalStateException("Style isn't ready yet.")
        }
    }

    /**
     * Adds a SymbolLayer which will show the selected location's icon
     */
    private fun initSelectedStoreSymbolLayer() {
        val style: Style? = mapView.mapboxMap.style
        if (style != null) {
            // Add the icon image to the map
            style.addImage(
                "selected-terminal-location-icon-id",
                BitmapFactory.decodeResource(
                    requireContext().resources,
                    R.drawable.default_theme_icon_selected
                )
            )

            // Create and add the selected terminal location icon SymbolLayer to the map
            val selectedTerminalLocationSymbolLayer = SymbolLayer(
                "selected-terminal-location-layer-id",
                "terminal-location-source-id"
            )
            selectedTerminalLocationSymbolLayer.apply {
                iconImage("selected-terminal-location-icon-id")
                iconAllowOverlap(true)
                iconIgnorePlacement(true)
                filter(eq(get(PROPERTY_SELECTED), literal(true)))
            }
            style.addLayer(selectedTerminalLocationSymbolLayer)
        } else {
            Log.d(
                "TerminalFinderActivity",
                "initSelectedStoreSymbolLayer: Style isn't ready yet."
            )

            throw java.lang.IllegalStateException("Style isn't ready yet.")
        }
    }

    /**
     * Checks whether a Feature's boolean "selected" property is true or false
     *
     * @param index the specific Feature's index position in the FeatureCollection's list of Features.
     * @return true if "selected" is true. False if the boolean property is false.
     */
    private fun featureSelectStatus(index: Int): Boolean {
        return featureCollection.features()!![index].getBooleanProperty(PROPERTY_SELECTED)
    }

    /**
     * Set a feature selected state.
     *
     * @param index the index of selected feature
     */
    private fun setSelected(index: Int) {
        val feature = featureCollection.features()!![index]
        setFeatureSelectState(feature, true)
        prevSelectedIndex = index
        refreshTerminalSource()
    }

    /**
     * Selects the state of a feature
     *
     * @param feature the feature to be selected.
     */
    private fun setFeatureSelectState(feature: Feature, selectedState: Boolean) {
        feature.properties()!!.addProperty(PROPERTY_SELECTED, selectedState)
        refreshTerminalSource()
    }

    /**
     * Updates the display of data on the map after the FeatureCollection has been modified
     */
    private fun refreshTerminalSource() {
        mapView.mapboxMap.style?.getSourceAs<GeoJsonSource>("terminal-location-source-id")
            ?.featureCollection(featureCollection)
    }

    private fun closeSearchView() {
        toolbar.collapseActionView()
        searchView.setQuery("", false)

        // Hide nav view
        searchPlaceListener?.hideNavView()

        // Remove route lines and hide the location cards
        removeSourceFeatures("navigation-route-source-id", listOf("navigation-route-feature-id"))
        removeSourceFeatures("jeepney-route-source-id", listOf("jeepney-route-feature-id"))
        locationsRecyclerView.visibility = View.GONE
    }

    private fun getInformationFromDirectionsApi(
        destinationPoint: Point,
        listIndex: Int
    ) {
        updateUserLocation()

        // Initialize the directionsApiClient object for eventually drawing a navigation route on the map
        val routeOptions: RouteOptions =
            RouteOptions.builder().applyDefaultNavigationOptions()
                .profile(DirectionsCriteria.PROFILE_WALKING)
                .coordinatesList(listOf(userLocation, destinationPoint)).build()

        val directionsApiClient: MapboxDirections =
            MapboxDirections.builder().routeOptions(routeOptions)
                .accessToken(getString(R.string.mapbox_access_token))
                .build()

        directionsApiClient.enqueueCall(object : Callback<DirectionsResponse?> {
            override fun onResponse(
                call: Call<DirectionsResponse?>,
                response: Response<DirectionsResponse?>
            ) {
                // Check that the response isn't null and that the response has a route
                if (response.body() == null) {
                    Log.e(
                        "MapFragment",
                        "No routes found, make sure you set the right user and access token."
                    )
                } else if (response.body()!!.routes().size < 1) {
                    Log.e("MapFragment", "No routes found")
                } else {
                    // Retrieve and draw the navigation route on the map
                    val currentRoute: DirectionsRoute = response.body()!!.routes()[0]
                    drawPolylineRoute(
                        currentRoute,
                        "navigation-route-source-id",
                        "navigation-route-feature-id"
                    )

                    // Use Mapbox Turf helper method to convert meters to miles and then format the mileage number
                    val df = DecimalFormat("#.#")
                    val finalConvertedFormattedDistance = df.format(
                        TurfConversion.convertLength(
                            response.body()!!.routes()[0].distance(), TurfConstants.UNIT_METERS,
                            TurfConstants.UNIT_MILES
                        )
                    ).toString()

                    // Set the distance for each location object in the list of locations
                    listOfIndividualLocations[listIndex].distance =
                        finalConvertedFormattedDistance
                    // Refresh the displayed recyclerview when the location's distance is set
                    styleRvAdapter.notifyItemChanged(listIndex)
                }
            }

            override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {
                Toast.makeText(activity, R.string.navigation_route_failure, Toast.LENGTH_LONG)
                    .show()
            }
        })
    }

    private fun updateUserLocation() {
        locationProvider.getLastLocation { result ->
            if (result != null) {
                userLocation = Point.fromLngLat(result.longitude, result.latitude)
            } else {
                Toast.makeText(
                    activity, "Failed to get device location provider",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun getInformationFromMapMatchingApi(
        routeLine: LineString,
    ) {
        // Initialize the mapMatchingApiClient object for eventually drawing a jeepney route on the map
        val mapMatchingApiClient: MapboxMapMatching =
            MapboxMapMatching.builder().coordinates(routeLine.coordinates())
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .accessToken(getString(R.string.mapbox_access_token))
                .build()

        mapMatchingApiClient.enqueueCall(object : Callback<MapMatchingResponse?> {
            override fun onResponse(
                call: Call<MapMatchingResponse?>,
                response: Response<MapMatchingResponse?>
            ) {
                // Check that the response isn't null and that the response has a route
                if (response.body() == null) {
                    Log.e(
                        "MapFragment",
                        "No routes found, make sure you set the right user and access token."
                    )
                } else if ((response.body()!!.matchings()?.size ?: 0) < 1) {
                    Log.e("MapFragment", "No routes found")
                } else {
                    // Retrieve and draw the jeepney route on the map
                    val currentRoute: DirectionsRoute =
                        response.body()!!.matchings()!![0].toDirectionRoute()
                    drawPolylineRoute(
                        currentRoute,
                        "jeepney-route-source-id",
                        "jeepney-route-feature-id"
                    )
                }
            }

            override fun onFailure(call: Call<MapMatchingResponse?>, throwable: Throwable) {
                Toast.makeText(activity, R.string.jeepney_route_failure, Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun navigateToSearchCoordinate(destinationPoint: Point) {
        val routeOptions: RouteOptions =
            RouteOptions.builder().applyDefaultNavigationOptions()
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .coordinatesList(listOf(userLocation, destinationPoint)).build()

        val directionsApiClient: MapboxDirections =
            MapboxDirections.builder().routeOptions(routeOptions)
                .accessToken(getString(R.string.mapbox_access_token))
                .build()

        directionsApiClient.enqueueCall(object : Callback<DirectionsResponse?> {
            override fun onResponse(
                call: Call<DirectionsResponse?>,
                response: Response<DirectionsResponse?>
            ) {
                if (response.body() == null) {
                    Log.e(
                        "MapFragment",
                        "No routes found, make sure you set the right user and access token."
                    )
                } else if (response.body()!!.routes().size < 1) {
                    Log.e("MapFragment", "No routes found")
                } else {
                    val currentRoute: DirectionsRoute = response.body()!!.routes()[0]
                    drawPolylineRoute(
                        currentRoute,
                        "navigation-route-source-id",
                        "navigation-route-feature-id"
                    )
                }
            }

            override fun onFailure(call: Call<DirectionsResponse?>, throwable: Throwable) {
                Toast.makeText(activity, R.string.navigation_route_failure, Toast.LENGTH_LONG)
                    .show()
            }
        })
    }

    private fun repositionMapCamera(newTarget: Point) {
        if (hasOnIndicatorPositionChangedListener) {
            mapView.location
                .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
            hasOnIndicatorPositionChangedListener = false
        }
        mapView.mapboxMap.setCamera(CameraOptions.Builder().center(newTarget).build())

        mapView.camera.apply {
            val bearing =
                createBearingAnimator(cameraAnimatorOptions(18.0, 20.0) { startValue(15.0) }) {
                    duration = 8500
                    interpolator = AnticipateOvershootInterpolator()
                }
            val pitch = createPitchAnimator(cameraAnimatorOptions(30.0) { startValue(15.0) }) {
                duration = 2000
            }
            playAnimatorsTogether(bearing, pitch)
        }
    }

    private fun setUpRecyclerViewOfLocationCards(chosenTheme: Int) {
        // Initialize the recyclerview of location cards and a custom class for automatic card scrolling
        locationsRecyclerView = binding.mapLayoutRv
        locationsRecyclerView.setHasFixedSize(true)
        locationsRecyclerView.setLayoutManager(LinearLayoutManagerWithSmoothScroller(requireActivity()))
        styleRvAdapter = LocationRecyclerViewAdapter(
            listOfIndividualLocations,
            requireActivity().applicationContext, this, chosenTheme
        )
        locationsRecyclerView.setAdapter(styleRvAdapter)
        val snapHelper: SnapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(locationsRecyclerView)
    }

    private fun drawPolylineRoute(route: DirectionsRoute, sourceId: String, featureId: String) {
        // Retrieve and update the source designated for showing the polyline route
        mapView.mapboxMap.style?.getSourceAs<GeoJsonSource>(sourceId)
            ?.featureCollection(
                FeatureCollection.fromFeature(
                    Feature.fromGeometry(
                        LineString.fromPolyline(
                            route.geometry()!!,
                            PRECISION_6
                        ), null,
                        featureId
                    )
                )
            )
    }

    private fun initRoutePolylineLineLayer(sourceId: String, layerId: String, color: Int) {
        val style: Style? = mapView.mapboxMap.style
        if (style != null) {
            // Create and add the GeoJsonSource to the map
            val navigationLineLayerGeoJsonSource =
                GeoJsonSource.Builder(sourceId).build()
            style.addSource(navigationLineLayerGeoJsonSource)

            // Create and add the LineLayer to the map to show the route line
            val navigationRouteLineLayer = LineLayer(
                layerId,
                sourceId
            )

            navigationRouteLineLayer.apply {
                lineColor(resources.getColor(color))
                lineWidth(NAVIGATION_LINE_WIDTH)
                slot("middle")
            }

            style.addLayerBelow(navigationRouteLineLayer, "mapbox-location-indicator-layer")
        } else {
            Log.d(
                "RouteFinderActivity",
                "initRoutePolylineLineLayer: Style isn't ready yet."
            )

            throw java.lang.IllegalStateException("Style isn't ready yet.")
        }
    }

    private fun removeSourceFeatures(sourceId: String, featureIds: List<String>) {
        mapView.mapboxMap.style?.removeGeoJSONSourceFeatures(
            sourceId,
            "",
            featureIds
        )
    }

    private fun deviceHasInternetConnection(): Boolean {
        val connectivityManager =
            requireActivity().applicationContext.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.search_menu, menu)

        val searchActionView = menu.findItem(R.id.action_search)
        searchActionView.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                searchPlaceView.hide()
                searchResultsView.isVisible = true
                toolbar.setBackgroundColor(resources.getColor(R.color.white))
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                searchResultsView.isVisible = false
                toolbar.setBackgroundColor(resources.getColor(android.R.color.transparent))
                return true
            }
        })

        searchView = searchActionView.actionView as SearchView
        searchView.queryHint = getString(R.string.query_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                //searchEngineUiAdapter.search(newText)
                featureCollectionSearch(newText)
                return false
            }
        })
    }

    private fun featureCollectionSearch(query: String) {
        val viewItems = mutableListOf<SearchResultAdapterItem>()

        featureCollection.features()?.forEachIndexed { index, feature ->
            if (feature.getStringProperty("name").toLowerCase().contains(query.toLowerCase())) {
                viewItems.add(
                    SearchResultAdapterItem.Result(
                        feature.getStringProperty("name"),
                        null,
                        null,
                        R.drawable.default_theme_icon,
                        payload = listOf(index, feature.geometry())
                    )
                )
            }
        }

        searchResultsView.setAdapterItems(viewItems)
    }

    // TODO: Set the theme dynamically according to the time of the day
    /* private fun getMapStyleUri(): String {
        return when (val darkMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_YES -> Style.STANDARD
            Configuration.UI_MODE_NIGHT_NO,
            Configuration.UI_MODE_NIGHT_UNDEFINED -> Style.STANDARD

            else -> error("Unknown mode: $darkMode")
        }
    } */

    private class MapMarkersManager(mapView: MapView) {

        private val mapboxMap = mapView.mapboxMap
        private val viewport = mapView.viewport
        private val circleAnnotationManager =
            mapView.annotations.createCircleAnnotationManager(null)
        private val markers = mutableMapOf<String, Point>()

        var onMarkersChangeListener: (() -> Unit)? = null

        val hasMarkers: Boolean
            get() = markers.isNotEmpty()

        fun clearMarkers() {
            markers.clear()
            circleAnnotationManager.deleteAll()
        }

        fun showMarker(coordinate: Point) {
            showMarkers(listOf(coordinate))
        }

        fun showMarkers(coordinates: List<Point>) {
            clearMarkers()

            // Sets viewport to idle to prevent camera from recentering to user's location
            viewport.idle()

            if (coordinates.isEmpty()) {
                onMarkersChangeListener?.invoke()
                return
            }

            coordinates.forEach { coordinate ->
                val circleAnnotationOptions: CircleAnnotationOptions = CircleAnnotationOptions()
                    .withPoint(coordinate)
                    .withCircleRadius(8.0)
                    .withCircleColor("#ee4e8b")
                    .withCircleStrokeWidth(2.0)
                    .withCircleStrokeColor("#ffffff")

                val annotation = circleAnnotationManager.create(circleAnnotationOptions)
                markers[annotation.id] = coordinate
            }

            if (coordinates.size == 1) {
                CameraOptions.Builder()
                    .center(coordinates.first())
                    .padding(MARKERS_INSETS_OPEN_CARD)
                    .zoom(13.5)
                    .build()
            } else {
                mapboxMap.cameraForCoordinates(
                    coordinates, MARKERS_INSETS, bearing = null, pitch = null
                )
            }.also {
                mapboxMap.setCamera(it)
            }
            onMarkersChangeListener?.invoke()
        }
    }

    private companion object {

        val MARKERS_EDGE_OFFSET = dpToPx(64).toDouble()
        val PLACE_CARD_HEIGHT = dpToPx(300).toDouble()

        val MARKERS_INSETS = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET
        )

        val MARKERS_INSETS_OPEN_CARD = EdgeInsets(
            MARKERS_EDGE_OFFSET, MARKERS_EDGE_OFFSET, PLACE_CARD_HEIGHT, MARKERS_EDGE_OFFSET
        )

        const val PERMISSIONS_REQUEST_LOCATION = 0
        const val REQUEST_CHECK_SETTINGS = 1
    }
}