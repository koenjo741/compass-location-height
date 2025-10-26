package com.example.compasslocationheight

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.compasslocationheight.ui.theme.CompassLocationHeightTheme

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private var gravity: FloatArray? = null
    private var geomagnetic: FloatArray? = null

    var azimuth by mutableStateOf(0f)

    private var smoothedAzimuth = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // HIER IST DIE KORREKTUR
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        enableEdgeToEdge()
        setContent {
            CompassLocationHeightTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CompassDisplay(
                        azimuth = azimuth,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            gravity = event.values
        }
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            geomagnetic = event.values
        }

        if (gravity != null && geomagnetic != null) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            val success = SensorManager.getRotationMatrix(r, i, gravity, geomagnetic)
            if (success) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)

                var currentAzimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
                currentAzimuth = (currentAzimuth + 360) % 360

                val filterFactor = 0.97f

                val diff = Math.abs(smoothedAzimuth - currentAzimuth)

                if (diff > 180) {
                    if (smoothedAzimuth > currentAzimuth) {
                        smoothedAzimuth += (360 - diff) * (1-filterFactor)
                    } else {
                        smoothedAzimuth -= (360 - diff) * (1-filterFactor)
                    }
                } else {
                    smoothedAzimuth = (smoothedAzimuth * filterFactor) + (currentAzimuth * (1 - filterFactor))
                }

                smoothedAzimuth = (smoothedAzimuth + 360) % 360

                if (azimuth.toInt() != smoothedAzimuth.toInt()) {
                    azimuth = smoothedAzimuth
                }
            }
        }
    }
}


@Composable
fun CompassDisplay(azimuth: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val degrees = azimuth.toInt()
        Text(text = "$degrees°")
    }
}


@Preview(showBackground = true)
@Composable
fun CompassDisplayPreview() {
    CompassLocationHeightTheme {
        CompassDisplay(azimuth = 123f)
    }
}