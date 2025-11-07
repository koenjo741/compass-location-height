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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.compasslocationheight.ui.theme.CompassLocationHeightTheme
import androidx.activity.viewModels

// --- AppColors OBJEKT ---
object AppColors {
    // Dark Mode (unser bisheriger Standard)
    val DarkBackground = Color.Black
    val DarkText = Color(0xFFFFFAF0) // FloralWhite
    val DarkHeading = Color(0xFF1E90FF) // Blau
    val DarkAccent = Color(0xFFFE0000)  // Rot
    val DarkSubtle = Color.Gray

    // Light Mode
    val LightBackground = Color.White
    val LightText = Color.Black
    val LightHeading = Color(0xFF0000DD) // Dunkleres Blau
    val LightAccent = Color(0xFFCC0000)  // Dunkleres Rot
    val LightSubtle = Color.DarkGray

    // Night Mode (Rotlicht)
    val NightBackground = Color.Black
    val NightText = Color(0xFFB71C1C) // Dunkles Rot
    val NightHeading = Color(0xFF0045F5).copy(alpha = 0.7f) // Abgedunkeltes Blau -> Violettstich
    val NightAccent = Color(0xFFF44336)   // Helles Rot
    val NightSubtle = Color(0xFF4E342E)   // Sehr dunkles Rotbraun

    // Fadenkreuz & Wasserwaage (bleiben meist gleich)
    val CrosshairGreen = Color(0xFF33FF33)
    val BubbleOrange = Color(0xFFFF9933)
}

enum class ThemeMode {
    Dark, Light, Night
}

@Serializable data class WeatherResponse(val current_weather: CurrentWeather, val hourly: HourlyData)
@Serializable data class CurrentWeather(val temperature: Double)
@Serializable data class HourlyData(val time: List<String>, val pressure_msl: List<Double>)

class MainActivity : ComponentActivity(), SensorEventListener {
    private val settingsViewModel: SettingsViewModel by viewModels()
    // Manager & Sensoren
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var pressureSensor: Sensor? = null
    private var rotationVectorSensor: Sensor? = null
    private var magnetometer: Sensor? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Rohdaten & UI States
    var azimuth by mutableFloatStateOf(0f)
    private var smoothedAzimuth = 0f
    private var isFirstValue = true

    var hasLocationPermission by mutableStateOf(false)
    var gpsLatitude by mutableDoubleStateOf(0.0)
    var gpsLongitude by mutableDoubleStateOf(0.0)
    var isLocationAvailable by mutableStateOf(false)
    var barometricAltitude by mutableDoubleStateOf(0.0)
    var addressText by mutableStateOf("Suche Adresse...")
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
                    if (now - lastWeatherApiCall > 300000) { // Alle 5 Minuten
                        getCurrentTemperature(location.latitude, location.longitude)
                        lastWeatherApiCall = now
                    }
                }
            }
        }

        window.statusBarColor = Color.Black.toArgb()

        // --- NEUER setContent BLOCK in MainActivity.kt ---
        setContent {
            val navController = rememberNavController()

            // NEU: Wir holen den Theme-Zustand direkt aus dem ViewModel
            val currentTheme = settingsViewModel.themeMode

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
                        // NEU: Wir übergeben den aktuellen Theme-Modus und die Funktion zum Ändern
                        CompassScreen(
                            navController = navController,
                            currentThemeMode = currentTheme,
                            onThemeChange = { newTheme -> settingsViewModel.setTheme(newTheme) }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(backgroundColor = backgroundColor, textColor = textColor)
                    }
                    composable("about") {
                        AboutScreen(backgroundColor = backgroundColor, textColor = textColor)
                    }
                }
            }
        }

        checkLocationPermission()
    }
    private fun getAddressFromCoordinates(latitude: Double, longitude: Double) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(this@MainActivity, Locale.GERMANY)

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
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true })
                    }
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
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:00", Locale.GERMANY).format(Date())
        val currentIndex = hourlyData.time.indexOf(now)
        if (currentIndex == -1 || currentIndex + 3 >= hourlyData.pressure_msl.size) {
            return "→"
        }
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