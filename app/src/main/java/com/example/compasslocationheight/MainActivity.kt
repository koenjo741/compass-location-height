package com.example.compasslocationheight

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.toArgb

object AppColors {
    val HeadingBlue = Color(0xFF1E90FF)
    val NorthRed = Color(0xFFFE0000)
    val CrosshairGreen = Color(0xFF33FF33)
    val BubbleOrange = Color(0xFFFF9933)
    val FloralWhite = Color(0xFFFFFFA)
}

class MainActivity : ComponentActivity(), SensorEventListener {
    // Manager & Sensoren
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var pressureSensor: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Rohdaten & UI States
    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null
    var azimuth by mutableFloatStateOf(0f)
    private var smoothedAzimuth = 0f

    var hasLocationPermission by mutableStateOf(false)
    var gpsLatitude by mutableDoubleStateOf(0.0)
    var gpsLongitude by mutableDoubleStateOf(0.0)
    var isLocationAvailable by mutableStateOf(false)
    var barometricAltitude by mutableDoubleStateOf(0.0)
    var addressText by mutableStateOf("Suche Adresse...")
    var pitch by mutableFloatStateOf(0f)
    var roll by mutableFloatStateOf(0f)

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) { hasLocationPermission = true; startLocationUpdates() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0.lastLocation?.let { location ->
                    gpsLatitude = location.latitude
                    gpsLongitude = location.longitude
                    isLocationAvailable = true
                    getAddressFromCoordinates(location.latitude, location.longitude)
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

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                hasLocationPermission = true
                startLocationUpdates()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_UI)
        if (hasLocationPermission) { startLocationUpdates() }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values
            roll = event.values[0]
            pitch = event.values[1]
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) { geomagnetic = event.values }
        if (event.sensor.type == Sensor.TYPE_PRESSURE) {
            val pressureValue = event.values[0]
            val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressureValue)
            barometricAltitude = altitude.toDouble()
        }
        if (gravity != null && geomagnetic != null) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravity!!, geomagnetic!!)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                var currentAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                currentAzimuth = (currentAzimuth + 360) % 360
                val filterFactor = 0.97f
                smoothedAzimuth = (smoothedAzimuth * filterFactor) + (currentAzimuth * (1 - filterFactor))
                smoothedAzimuth %= 360
                if (azimuth.toInt() != smoothedAzimuth.toInt()) {
                    azimuth = smoothedAzimuth
                }
            }
        }
    }
}

fun degreesToCardinalDirection(degrees: Int): String {
    val directions = arrayOf("N", "NNO", "NO", "ONO", "O", "OSO", "SO", "SSO", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    return directions[((degrees + 11.25) / 22.5).toInt() % 16]
}

@Composable
fun MainActivity.UserInterface() {
    CompassLocationHeightTheme(darkTheme = true) {
        Scaffold(modifier = Modifier.fillMaxSize().background(Color.Black)) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color.Black),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompassHeader(azimuth = azimuth)
                Box(contentAlignment = Alignment.Center) {
                    CompassRose(azimuth = azimuth)
                    CompassOverlay(pitch = pitch, roll = roll, azimuth = azimuth)
                }
                if (hasLocationPermission) {
                    LocationDisplay(latitude = gpsLatitude, longitude = gpsLongitude, barometricAltitude = barometricAltitude, isLocationAvailable = isLocationAvailable, address = addressText)
                } else { Text(text = "Standort Erlaubnis benötigt", fontSize = 20.sp, color = Color.White) }
            }
        }
    }
}

@Composable
fun CompassHeader(azimuth: Float) {
    val degrees = azimuth.toInt()
    val cardinal = degreesToCardinalDirection(degrees)
    Text(
        text = buildAnnotatedString {
            append("$degrees° ")
            pushStyle(TextStyle(fontWeight = FontWeight.Bold).toSpanStyle())
            append(cardinal)
            pop()
        },
        color = AppColors.HeadingBlue,
        fontSize = 48.sp,
        modifier = Modifier.padding(top = 24.dp)
    )
}

@Composable
fun CompassRose(azimuth: Float, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Canvas(modifier = modifier.size(300.dp)) {
        val radius = size.minDimension / 2
        val center = this.center

        rotate(degrees = -azimuth) {
            for (i in 0 until 360 step 5) {
                val angleInRad = Math.toRadians(i.toDouble())
                val isMajorLine = i % 30 == 0
                val isCardinal = i % 90 == 0
                val lineLength = if (isCardinal) 48f else 30f
                val color = AppColors.FloralWhite.copy(alpha = if (isMajorLine) 1f else 0.5f)
                val strokeWidth = if (isMajorLine) 4f else 2f
                val startOffset = Offset(x = (radius - lineLength) * cos(angleInRad).toFloat() + center.x, y = (radius - lineLength) * sin(angleInRad).toFloat() + center.y)
                val endOffset = Offset(x = radius * cos(angleInRad).toFloat() + center.x, y = radius * sin(angleInRad).toFloat() + center.y)
                drawLine(color, start = startOffset, end = endOffset, strokeWidth = strokeWidth)
            }
            drawLine(AppColors.NorthRed, start = Offset(center.x, 0f), end = Offset(center.x, 48f), strokeWidth = 8f)
        }

        for (i in 0 until 360 step 30) {
            if (i % 90 != 0) {
                drawTextCustom(textMeasurer, i.toString(), center, radius * 0.72f, (i.toFloat() - azimuth), style = TextStyle(color = AppColors.FloralWhite.copy(alpha = 0.7f), fontSize = 18.sp))
            }
        }
    }
}

fun DrawScope.drawTextCustom(textMeasurer: androidx.compose.ui.text.TextMeasurer, text: String, center: Offset, radius: Float, angleDegrees: Float, style: TextStyle) {
    val angleRad = Math.toRadians(angleDegrees.toDouble() - 90)
    val textLayoutResult = textMeasurer.measure(text, style)
    val textWidth = textLayoutResult.size.width
    val textHeight = textLayoutResult.size.height
    val x = center.x + radius * cos(angleRad).toFloat() - textWidth / 2
    val y = center.y + radius * sin(angleRad).toFloat() - textHeight / 2
    drawText(textLayoutResult, topLeft = Offset(x, y))
}

@Composable
fun CompassOverlay(pitch: Float, roll: Float, azimuth: Float, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    Box(modifier = modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val radius = size.minDimension / 2 // RADIUS MUSS HIER DEFINIERT WERDEN

            val path = Path().apply {
                moveTo(canvasWidth / 2, -15f)
                lineTo(canvasWidth / 2 - 35, 60f)
                lineTo(canvasWidth / 2 + 35, 60f)
                close()
            }
            drawPath(path, color = AppColors.HeadingBlue)

            val crosshairLength = 96f
            drawLine(AppColors.CrosshairGreen, start = Offset(center.x - crosshairLength, center.y), end = Offset(center.x + crosshairLength, center.y), strokeWidth = 3f)
            drawLine(AppColors.CrosshairGreen, start = Offset(center.x, center.y - crosshairLength), end = Offset(center.x, center.y + crosshairLength), strokeWidth = 3f)

            val textRadius = radius * 0.82f
            val textSize = 24.sp * 1.15f
            val textStyleN = TextStyle(color = AppColors.HeadingBlue, fontSize = textSize, fontWeight = FontWeight.Bold)
            val textStyleOthers = TextStyle(color = AppColors.FloralWhite, fontSize = textSize, fontWeight = FontWeight.SemiBold)

            drawTextCustom(textMeasurer, "N", center, textRadius, 270f - azimuth, textStyleN)
            drawTextCustom(textMeasurer, "E", center, textRadius, 0f - azimuth, textStyleOthers)
            drawTextCustom(textMeasurer, "S", center, textRadius, 90f - azimuth, textStyleOthers)
            drawTextCustom(textMeasurer, "W", center, textRadius, 180f - azimuth, textStyleOthers)
        }

        Box(
            modifier = Modifier.align(Alignment.Center).offset(x = (-roll * 10).dp, y = (pitch * 10).dp).size(25.dp).background(AppColors.BubbleOrange, shape = CircleShape)
        )
    }
}

@Composable
fun LocationDisplay(latitude: Double, longitude: Double, barometricAltitude: Double, isLocationAvailable: Boolean, address: String) {
    if (!isLocationAvailable) { Text(text = "Lade Standortdaten...", fontSize = 20.sp, color = Color.White); return }
    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = address, fontSize = 22.sp, color = AppColors.FloralWhite, modifier = Modifier.padding(bottom = 16.dp).clickable { openMaps(context, latitude, longitude) })

        val lat = String.format(Locale.US, "%.6f", latitude)
        val lon = String.format(Locale.US, "%.6f", longitude)
        val altBaro = String.format(Locale.US, "%.1f", barometricAltitude)
        Text(text = "Breite: $lat", fontSize = 16.sp, color = Color.Gray)
        Text(text = "Länge: $lon", fontSize = 16.sp, color = Color.Gray)
        Text(text = "Höhe: $altBaro m", fontSize = 16.sp, color = Color.Gray)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun DefaultPreview() {
    CompassLocationHeightTheme(darkTheme = true) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxSize()) {
            CompassHeader(azimuth = 330f)
            Box(contentAlignment = Alignment.Center) {
                CompassRose(azimuth = 330f)
                CompassOverlay(pitch = -1.5f, roll = 2.5f, azimuth = 330f)
            }
            LocationDisplay(latitude = 48.330967, longitude = 14.272329, barometricAltitude = 370.0, isLocationAvailable = true, address = "Teststraße 1, 4020 Linz")
        }
    }
}

private fun openMaps(context: Context, latitude: Double, longitude: Double) {
    val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Mein Standort)")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    context.startActivity(mapIntent)
}
