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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.compasslocationheight.ui.theme.CompassLocationHeightTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

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

    // Wasserwaage States
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

        enableEdgeToEdge()
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
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
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
            // Wasserwaage-Werte aktualisieren
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
                val diff = Math.abs(smoothedAzimuth - currentAzimuth)
                if (diff > 180) {
                    if (smoothedAzimuth > currentAzimuth) smoothedAzimuth += (360 - diff) * (1 - filterFactor)
                    else smoothedAzimuth -= (360 - diff) * (1 - filterFactor)
                } else {
                    smoothedAzimuth = (smoothedAzimuth * filterFactor) + (currentAzimuth * (1 - filterFactor))
                }
                smoothedAzimuth %= 360
                if (azimuth.toInt() != smoothedAzimuth.toInt()) azimuth = smoothedAzimuth
            }
        }
    }
}
@Composable
fun MainActivity.UserInterface() {
    CompassLocationHeightTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CompassDisplay(azimuth = azimuth)

                BubbleLevel(pitch = pitch, roll = roll)

                if (hasLocationPermission) {
                    LocationDisplay(
                        latitude = gpsLatitude,
                        longitude = gpsLongitude,
                        barometricAltitude = barometricAltitude,
                        isLocationAvailable = isLocationAvailable,
                        address = addressText
                    )
                } else { Text(text = "Standort Erlaubnis benötigt", fontSize = 20.sp) }
            }
        }
    }
}

@Composable
fun BubbleLevel(pitch: Float, roll: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(150.dp)
            .background(Color.DarkGray, shape = CircleShape)
            .border(2.dp, Color.LightGray, shape = CircleShape)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-roll * 10).dp, y = (pitch * 10).dp) // Multiplizieren, um die Bewegung sichtbarer zu machen
                .size(30.dp)
                .background(Color.Green, shape = CircleShape)
                .border(1.dp, Color.White, shape = CircleShape)

        )
    }
}


@Composable
fun LocationDisplay(
    latitude: Double,
    longitude: Double,
    barometricAltitude: Double,
    isLocationAvailable: Boolean,
    address: String
) {
    if (!isLocationAvailable) {
        Text(text = "Lade Standortdaten...", fontSize = 20.sp)
        return
    }
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = address,
            fontSize = 22.sp,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .clickable {
                    openMaps(context, latitude, longitude)
                }
        )

        val lat = String.format(Locale.US, "%.6f", latitude)
        val lon = String.format(Locale.US, "%.6f", longitude)
        val altBaro = String.format(Locale.US, "%.1f", barometricAltitude)

        Text(text = "Breite: $lat", fontSize = 16.sp)
        Text(text = "Länge: $lon", fontSize = 16.sp)
        Text(text = "Höhe: $altBaro m", fontSize = 16.sp)
    }
}

@Composable
fun CompassDisplay(azimuth: Float, modifier: Modifier = Modifier) {
    val degrees = azimuth.toInt()
    Text(text = "$degrees°", fontSize = 60.sp, modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    CompassLocationHeightTheme {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CompassDisplay(azimuth = 123f)
        }
    }
}

private fun openMaps(context: Context, latitude: Double, longitude: Double) {
    val gmmIntentUri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(Mein Standort)")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
    mapIntent.setPackage("com.google.android.apps.maps")
    context.startActivity(mapIntent)
}