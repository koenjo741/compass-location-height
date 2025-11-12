package com.example.compasslocationheight

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.compasslocationheight.ui.theme.CompassLocationHeightTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable data class WeatherResponse(val current_weather: CurrentWeather, val hourly: HourlyData)
@Serializable data class CurrentWeather(val temperature: Double)
@Serializable data class HourlyData(val time: List<String>, val pressure_msl: List<Double>)

class SettingsViewModelFactory(private val dataStore: SettingsDataStore) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(dataStore) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity(), SensorEventListener {
    private val settingsDataStore by lazy { SettingsDataStore(this) }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(settingsDataStore)
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var pressureSensor: Sensor? = null
    private var rotationVectorSensor: Sensor? = null
    private var magnetometer: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    var azimuth by mutableFloatStateOf(0f)
    private var smoothedAzimuth = 0f
    private var isFirstValue = true
    var hasLocationPermission by mutableStateOf(false)
    var gpsLatitude by mutableDoubleStateOf(0.0)
    var gpsLongitude by mutableDoubleStateOf(0.0)
    var isLocationAvailable by mutableStateOf(false)
    var barometricAltitude by mutableDoubleStateOf(0.0)
    var addressText by mutableStateOf("")
    var currentDate by mutableStateOf("")
    var currentTime by mutableStateOf("")
    var pitch by mutableFloatStateOf(0f)
    var roll by mutableFloatStateOf(0f)
    var magnetometerAccuracy by mutableIntStateOf(0)
    var magneticDeclination by mutableFloatStateOf(0f)
    var currentTemperature by mutableStateOf<Double?>(null)
    var currentPressure by mutableFloatStateOf(0f)
    var pressureTrend by mutableStateOf("→")
    private var lastWeatherApiCall: Long = 0
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            hasLocationPermission = true
            startLocationUpdates()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addressText = getString(R.string.searching_address)

        lifecycleScope.launch {
            var isInitialLanguageSet = false
            settingsViewModel.language
                .distinctUntilChanged()
                .collect { langCode ->
                    if (isInitialLanguageSet) {
                        LocaleHelper.setLocale(langCode)
                        recreate()
                    } else {
                        LocaleHelper.setLocale(langCode)
                        isInitialLanguageSet = true
                    }
                }
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.lastLocation?.let { location ->
                    gpsLatitude = location.latitude
                    gpsLongitude = location.longitude
                    isLocationAvailable = true
                    getAddressFromCoordinates(location.latitude, location.longitude)
                    updateMagneticDeclination(location.latitude, location.longitude, location.altitude)
                    val now = System.currentTimeMillis()
                    if (now - lastWeatherApiCall > 300000) {
                        getCurrentTemperature(location.latitude, location.longitude)
                        lastWeatherApiCall = now
                    }
                }
            }
        }

        setContent {
            val navController = rememberNavController()
            val currentTheme by settingsViewModel.themeMode.collectAsState()

            val backgroundColor = when (currentTheme) {
                ThemeMode.Light -> AppColors.LightBackground
                else -> AppColors.DarkBackground
            }
            val textColor = when (currentTheme) {
                ThemeMode.Light -> AppColors.LightText
                ThemeMode.Night -> AppColors.NightText
                ThemeMode.Dark -> AppColors.DarkText
            }
            val isDarkTheme = when (currentTheme) {
                ThemeMode.Dark, ThemeMode.Night -> true
                ThemeMode.Light -> false
            }

            CompassLocationHeightTheme(darkTheme = isDarkTheme) {
                NavHost(navController = navController, startDestination = "compass") {
                    composable("compass") {
                        CompassScreen(
                            navController = navController,
                            settingsViewModel = settingsViewModel
                        )
                    }
                    composable("settings") {
                        val headingColor by remember(currentTheme) {
                            mutableStateOf(
                                when (currentTheme) {
                                    ThemeMode.Light -> AppColors.LightHeading
                                    ThemeMode.Night -> AppColors.NightHeading
                                    ThemeMode.Dark -> AppColors.DarkHeading
                                }
                            )
                        }
                        SettingsScreen(
                            navController = navController,
                            settingsViewModel = settingsViewModel,
                            backgroundColor = backgroundColor,
                            textColor = textColor,
                            headingColor = headingColor
                        )
                    }
                    composable("about") {
                        val headingColor by remember(currentTheme) {
                            mutableStateOf(
                                when (currentTheme) {
                                    ThemeMode.Light -> AppColors.LightHeading
                                    ThemeMode.Night -> AppColors.NightHeading
                                    ThemeMode.Dark -> AppColors.DarkHeading
                                }
                            )
                        }
                        AboutScreen(
                            navController = navController,
                            backgroundColor = backgroundColor,
                            textColor = textColor,
                            headingColor = headingColor
                        )
                    }
                }
            }
        }
        checkLocationPermission()
    }

    private fun getAddressFromCoordinates(latitude: Double, longitude: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentLocale = resources.configuration.locales[0]
                val geocoder = Geocoder(this@MainActivity, currentLocale)

                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                val address = addresses?.firstOrNull()?.let { addr ->
                    val street = addr.thoroughfare ?: ""
                    val number = addr.subThoroughfare ?: ""
                    val postal = addr.postalCode ?: ""
                    val city = addr.locality ?: ""
                    "$street $number, $postal $city".trim().trim(',')
                } ?: getString(R.string.address_not_found)
                withContext(Dispatchers.Main) { addressText = address }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { addressText = getString(R.string.address_search_error) }
            }
        }
    }

    private fun getCurrentTemperature(latitude: Double, longitude: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = HttpClient(Android) {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                }
                val response: WeatherResponse = client.get("https://api.open-meteo.com/v1/forecast") {
                    parameter("latitude", latitude)
                    parameter("longitude", longitude)
                    parameter("current_weather", "true")
                    parameter("hourly", "pressure_msl")
                }.body()
                val trend = calculatePressureTrend(response.hourly)
                withContext(Dispatchers.Main) {
                    currentTemperature = response.current_weather.temperature
                    pressureTrend = trend
                }
                client.close()
            } catch (e: Exception) {
                println("Wetter-API Fehler: ${e.message}")
            }
        }
    }

    private fun calculatePressureTrend(hourlyData: HourlyData): String {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:00", resources.configuration.locales[0]).format(Date())
        val currentIndex = hourlyData.time.indexOf(now)
        if (currentIndex == -1 || currentIndex + 3 >= hourlyData.pressure_msl.size) { return "→" }
        val pressureNow = hourlyData.pressure_msl[currentIndex]
        val pressureIn3Hours = hourlyData.pressure_msl[currentIndex + 3]
        val difference = pressureIn3Hours - pressureNow
        return when {
            difference > 3.5 -> "↑↑"
            difference > 1.5 -> "↑"
            difference < -3.5 -> "↓↓"
            difference < -1.5 -> "↓"
            else -> "→"
        }
    }

    private fun updateMagneticDeclination(latitude: Double, longitude: Double, altitude: Double) {
        val time = System.currentTimeMillis()
        val geomagneticField = GeomagneticField(latitude.toFloat(), longitude.toFloat(), altitude.toFloat(), time)
        magneticDeclination = geomagneticField.declination
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                hasLocationPermission = true; startLocationUpdates()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        if (hasLocationPermission) { startLocationUpdates() }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            magnetometerAccuracy = accuracy
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                var currentAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                currentAzimuth = (currentAzimuth + 360) % 360
                if (isFirstValue) {
                    smoothedAzimuth = currentAzimuth
                    isFirstValue = false
                } else {
                    var diff = currentAzimuth - smoothedAzimuth
                    if (diff > 180f) diff -= 360f
                    else if (diff < -180f) diff += 360f
                    smoothedAzimuth = (smoothedAzimuth + diff * 0.1f + 360) % 360f
                }
                azimuth = smoothedAzimuth
            }
            Sensor.TYPE_ACCELEROMETER -> {
                roll = event.values[0]
                pitch = event.values[1]
            }
            Sensor.TYPE_PRESSURE -> {
                val pressureValue = event.values[0]
                val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressureValue)
                barometricAltitude = altitude.toDouble()
                currentPressure = pressureValue
            }
        }
    }
}