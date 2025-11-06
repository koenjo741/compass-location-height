package com.example.compasslocationheight

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.compasslocationheight.ui.theme.CompassLocationHeightTheme
import com.google.android.gms.location.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.toArgb
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// --- ERSETZE DAS KOMPLETTE AppColors OBJEKT HIERMIT ---
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
    val NightText = Color(0xFFB71C1C)     // Dunkles Rot
    val NightHeading = Color(0xFF4A148C).copy(alpha = 0.7f) // Abgedunkeltes Blau -> Violettstich
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

    var currentThemeMode by mutableStateOf(ThemeMode.Dark)

    private var lastWeatherApiCall: Long = 0

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { hasLocationPermission = true; startLocationUpdates() }
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
        setContent { UserInterface() }
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
                } ?: "Adresse nicht gefunden"
                withContext(Dispatchers.Main) { addressText = address }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { addressText = "Fehler bei Adresssuche" }
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
@Composable
fun ThemeSwitcher(currentMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val nextMode = when (currentMode) {
        ThemeMode.Dark -> ThemeMode.Light
        ThemeMode.Light -> ThemeMode.Night
        ThemeMode.Night -> ThemeMode.Dark
    }
    val nextModeIcon = when (nextMode) {
        ThemeMode.Light -> "☀️"
        ThemeMode.Night -> "🌙"
        ThemeMode.Dark -> "✨"
    }

    Text(
        text = nextModeIcon,
        fontSize = 30.sp,
        modifier = Modifier.clickable { onThemeChange(nextMode) }
    )
}
fun degreesToCardinalDirection(degrees: Int): String {
    val directions = arrayOf("N", "NNO", "NO", "ONO", "O", "OSO", "SO", "SSO", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    return directions[((degrees + 11.25) / 22.5).toInt() % 16]
}

@Composable
fun MainActivity.UserInterface() {
    // Zeit-Logik, bleibt unverändert
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(now)
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(now)
            delay(1000)
        }
    }

    // --- NEUE FARB-LOGIK BASIEREND AUF DEM THEME-SCHALTER ---
    val isDarkTheme = when (currentThemeMode) {
        ThemeMode.Dark, ThemeMode.Night -> true
        ThemeMode.Light -> false
    }
    val backgroundColor = when (currentThemeMode) {
        ThemeMode.Light -> AppColors.LightBackground
        else -> AppColors.DarkBackground // Gilt für Dark und Night
    }
    val headingColor = when (currentThemeMode) {
        ThemeMode.Light -> AppColors.LightHeading
        ThemeMode.Night -> AppColors.NightHeading
        ThemeMode.Dark -> AppColors.DarkHeading
    }
    val textColor = when (currentThemeMode) {
        ThemeMode.Light -> AppColors.LightText
        ThemeMode.Night -> AppColors.NightText
        ThemeMode.Dark -> AppColors.DarkText
    }
    val accentColor = when (currentThemeMode) {
        ThemeMode.Light -> AppColors.LightAccent
        ThemeMode.Night -> AppColors.NightAccent
        ThemeMode.Dark -> AppColors.DarkAccent
    }
    val subtleColor = when (currentThemeMode) {
        ThemeMode.Light -> AppColors.LightSubtle
        ThemeMode.Night -> AppColors.NightSubtle
        ThemeMode.Dark -> AppColors.DarkSubtle
    }

    // Das App-Theme wird jetzt dynamisch gesteuert
    CompassLocationHeightTheme(darkTheme = isDarkTheme) {
        Scaffold(modifier = Modifier.fillMaxSize().background(backgroundColor)) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Die Farben werden jetzt als Parameter übergeben
                    CompassHeader(
                        azimuth = azimuth,
                        magneticDeclination = magneticDeclination,
                        color = headingColor
                    )
                    Box(contentAlignment = Alignment.Center) {
                        CompassRose(
                            azimuth = azimuth,
                            magneticDeclination = magneticDeclination,
                            textColor = textColor,
                            accentColor = accentColor,
                            subtleColor = subtleColor
                        )
                        CompassOverlay(
                            pitch = pitch,
                            roll = roll,
                            headingColor = headingColor
                        )
                    }
                    if (hasLocationPermission) {
                        LocationDisplay(
                            latitude = gpsLatitude,
                            longitude = gpsLongitude,
                            barometricAltitude = barometricAltitude,
                            isLocationAvailable = isLocationAvailable,
                            address = addressText,
                            currentDate = currentDate,
                            currentTime = currentTime,
                            currentTemperature = currentTemperature,
                            currentPressure = currentPressure,
                            pressureTrend = pressureTrend,
                            textColor = textColor,
                            subtleColor = subtleColor
                        )
                    } else {
                        Text(text = "Standort Erlaubnis benötigt", fontSize = 20.sp, color = textColor)
                    }
                }
                // HIER WIRD DER ACCURACY INDICATOR (PUNKT) AUF TOP-RECHTS VERSCHOBEN
                AccuracyIndicator(
                    accuracy = magnetometerAccuracy,
                    modifier = Modifier
                        .align(Alignment.TopEnd) // <-- GEÄNDERT von TopStart
                        .padding(16.dp)
                )
                // DER THEME SWITCHER (EMOJI) WIRD AUF TOP-LINKS VERSCHOBEN
                ThemeSwitcher(
                    currentMode = currentThemeMode,
                    onThemeChange = { newMode -> currentThemeMode = newMode },
                    modifier = Modifier
                        .align(Alignment.TopStart) // <-- GEÄNDERT von TopEnd
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun CompassHeader(azimuth: Float, magneticDeclination: Float, color: Color) { // Nimmt die Farbe als Parameter
    val trueAzimuth = (azimuth - magneticDeclination + 360) % 360
    val degrees = trueAzimuth.toInt()
    val cardinal = degreesToCardinalDirection(degrees)
    Text(
        text = buildAnnotatedString {
            append("$degrees° ")
            pushStyle(TextStyle(fontWeight = FontWeight.Bold).toSpanStyle())
            append(cardinal)
            pop()
        },
        color = color, // <-- HIER IST DIE ÄNDERUNG
        fontSize = 48.sp,
        modifier = Modifier.padding(top = 24.dp)
    )
}

@Composable
fun CompassRose(
    azimuth: Float,
    magneticDeclination: Float,
    textColor: Color,
    accentColor: Color,
    subtleColor: Color
) {
    val trueAzimuth = azimuth - magneticDeclination
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = Modifier.size(300.dp)) {
        val radius = size.minDimension / 2
        val center = this.center
        rotate(degrees = -trueAzimuth) {
            for (i in 0 until 360 step 5) {
                val angleInRad = Math.toRadians(i.toDouble())
                val isMajorLine = i % 30 == 0
                val isCardinal = i % 90 == 0
                val lineLength = if (isCardinal) 48f else if (isMajorLine) 36f else 30f
                val color = textColor.copy(alpha = if (isMajorLine) 1f else 0.5f) // Verwendet textColor
                val strokeWidth = if (isMajorLine) 4f else 2f
                val startOffset = Offset(x = center.x + (radius - lineLength) * sin(angleInRad).toFloat(), y = center.y - (radius - lineLength) * cos(angleInRad).toFloat())
                val endOffset = Offset(x = center.x + radius * sin(angleInRad).toFloat(), y = center.y - radius * cos(angleInRad).toFloat())
                drawLine(color, start = startOffset, end = endOffset, strokeWidth = strokeWidth)
            }
            val northAngleInRad = Math.toRadians(0.0)
            val northLineLength = 48f
            drawLine(color = accentColor, start = Offset(x = center.x + (radius - northLineLength) * sin(northAngleInRad).toFloat(), y = center.y - (radius - northLineLength) * cos(northAngleInRad).toFloat()), end = Offset(x = center.x + radius * sin(northAngleInRad).toFloat(), y = center.y - radius * cos(northAngleInRad).toFloat()), strokeWidth = 8f) // Verwendet accentColor
        }
        val directionRadius = radius * 0.82f
        val numberRadius = radius * 0.72f
        val textSize = 24.sp * 1.15f
        val numberTextSize = 18.sp
        val textStyleN = TextStyle(color = accentColor, fontSize = textSize, fontWeight = FontWeight.Bold)
        val textStyleOthers = TextStyle(color = textColor, fontSize = textSize, fontWeight = FontWeight.SemiBold) // Verwendet textColor
        val numberStyle = TextStyle(color = textColor.copy(alpha = 0.7f), fontSize = numberTextSize) // Verwendet textColor

        drawTextCustom(textMeasurer, "N", center, directionRadius, 0f - trueAzimuth, textStyleN)
        drawTextCustom(textMeasurer, "E", center, directionRadius, 90f - trueAzimuth, textStyleOthers)
        drawTextCustom(textMeasurer, "S", center, directionRadius, 180f - trueAzimuth, textStyleOthers)
        drawTextCustom(textMeasurer, "W", center, directionRadius, 270f - trueAzimuth, textStyleOthers)
        for (i in 0 until 360 step 30) {
            if (i % 90 != 0) {
                drawTextCustom(textMeasurer, i.toString(), center, numberRadius, i.toFloat() - trueAzimuth, style = numberStyle)
            }
        }
    }
}

fun DrawScope.drawTextCustom(textMeasurer: androidx.compose.ui.text.TextMeasurer, text: String, center: Offset, radius: Float, angleDegrees: Float, style: TextStyle) {
    val angleRad = Math.toRadians(angleDegrees.toDouble())
    val textLayoutResult = textMeasurer.measure(text, style)
    val textWidth = textLayoutResult.size.width
    val textHeight = textLayoutResult.size.height
    val x = center.x + radius * sin(angleRad).toFloat() - textWidth / 2
    val y = center.y - radius * cos(angleRad).toFloat() - textHeight / 2
    drawText(textLayoutResult, topLeft = Offset(x, y))
}

@Composable
fun CompassOverlay(pitch: Float, roll: Float, headingColor: Color) {
    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val path = Path().apply {
                moveTo(canvasWidth / 2, -15f)
                lineTo(canvasWidth / 2 - 35, 60f)
                lineTo(canvasWidth / 2 + 35, 60f)
                close()
            }
            drawPath(path, color = headingColor) // Verwendet die übergebene Farbe

            val crosshairLength = 96f
            drawLine(AppColors.CrosshairGreen, start = Offset(center.x - crosshairLength, center.y), end = Offset(center.x + crosshairLength, center.y), strokeWidth = 3f)
            drawLine(AppColors.CrosshairGreen, start = Offset(center.x, center.y - crosshairLength), end = Offset(center.x, center.y + crosshairLength), strokeWidth = 3f)
        }
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-roll * 10).dp, y = (pitch * 10).dp)
                .size(25.dp)
                .background(AppColors.BubbleOrange, shape = CircleShape)
        )
    }
}

@Composable
fun LocationDisplay(
    latitude: Double,
    longitude: Double,
    barometricAltitude: Double,
    isLocationAvailable: Boolean,
    address: String,
    currentDate: String,
    currentTime: String,
    currentTemperature: Double?,
    currentPressure: Float,
    pressureTrend: String,
    textColor: Color,
    subtleColor: Color
) {
    if (!isLocationAvailable) {
        Text(text = "Lade Standortdaten...", fontSize = 20.sp, color = textColor)
        return
    }

    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = address,
            fontSize = 20.sp,
            color = textColor, // Verwendet die übergebene Textfarbe
            modifier = Modifier
                .padding(bottom = 16.dp)
                .border(
                    width = 1.dp,
                    color = subtleColor, // Verwendet die übergebene "subtile" Farbe
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable { openMaps(context, latitude, longitude) }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )

        val lat = String.format(Locale.US, "%.6f", latitude)
        val lon = String.format(Locale.US, "%.6f", longitude)
        val altBaro = String.format(Locale.US, "%.1f", barometricAltitude)
        Text(text = "Breite: $lat", fontSize = 16.sp, color = subtleColor) // Verwendet die übergebene "subtile" Farbe
        Text(text = "Länge: $lon", fontSize = 16.sp, color = subtleColor) // Verwendet die übergebene "subtile" Farbe
        Text(text = "Höhe: $altBaro m", fontSize = 16.sp, color = subtleColor) // Verwendet die übergebene "subtile" Farbe

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "Datum: $currentDate", fontSize = 16.sp, color = subtleColor) // Verwendet die übergebene "subtile" Farbe
        Text(text = "Zeit: $currentTime", fontSize = 16.sp, color = subtleColor) // Verwendet die übergebene "subtile" Farbe

        currentTemperature?.let { temp ->
            val tempFormatted = String.format(Locale.US, "%.1f", temp)
            Text(text = "Temperatur: $tempFormatted °C", fontSize = 16.sp, color = subtleColor) // Verwendet die übergebene "subtile" Farbe
        }

        if (currentPressure > 0f) {
            val pressureFormatted = String.format(Locale.US, "%.2f", currentPressure)
            Text(text = "Luftdruck: $pressureFormatted hPa $pressureTrend", fontSize = 16.sp, color = subtleColor) // Verwendet die übergebene "subtile" Farbe
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun DefaultPreview() {
    CompassLocationHeightTheme(darkTheme = true) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            // Übergib die Dummy-Farbe für den Dark Mode
            CompassHeader(
                azimuth = 330f,
                magneticDeclination = 4f,
                color = AppColors.DarkHeading
            )
            Box(contentAlignment = Alignment.Center) {
                // Übergib die Dummy-Farben für den Dark Mode
                CompassRose(
                    azimuth = 330f,
                    magneticDeclination = 4f,
                    textColor = AppColors.DarkText,
                    accentColor = AppColors.DarkAccent,
                    subtleColor = AppColors.DarkSubtle
                )
                // Übergib die Dummy-Farbe für den Dark Mode
                CompassOverlay(
                    pitch = -1.5f,
                    roll = 2.5f,
                    headingColor = AppColors.DarkHeading
                )
            }
            // Übergib die Dummy-Farben für den Dark Mode
            LocationDisplay(
                latitude = 48.330967,
                longitude = 14.272329,
                barometricAltitude = 370.0,
                isLocationAvailable = true,
                address = "Teststraße 1, 4020 Linz",
                currentDate = "29.10.2025",
                currentTime = "23:59:59",
                currentTemperature = 15.3,
                currentPressure = 1013.25f,
                pressureTrend = "↑",
                textColor = AppColors.DarkText,
                subtleColor = AppColors.DarkSubtle
            )
        }
    }
}

private fun openMaps(context: Context, latitude: Double, longitude: Double) {
    val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Mein Standort)")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    context.startActivity(mapIntent)
}

@Composable
fun AccuracyIndicator(accuracy: Int, modifier: Modifier = Modifier) {
    val color = when (accuracy) {
        SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> Color.Green
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> Color.Yellow
        SensorManager.SENSOR_STATUS_ACCURACY_LOW -> Color(0xFFFF8C00)
        else -> Color.Red
    }
    Box(
        modifier = modifier
            .size(20.dp)
            .background(color, shape = CircleShape)
            .border(1.dp, Color.White, shape = CircleShape)
    )
}