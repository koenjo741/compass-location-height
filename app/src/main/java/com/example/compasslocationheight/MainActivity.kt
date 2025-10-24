
package com.example.compasslocationheight

// Nötige Imports, die hinzugefügt wurden
import android.content.Context
import android.hardware.SensorManager
// Ende der neuen Imports

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.compasslocationheight.ui.theme.CompassLocationHeightTheme

class MainActivity : ComponentActivity() {

    // --- NEUE ZEILE 1 ---
    // Variable für den Sensor-Manager auf Klassenebene deklariert.
    private lateinit var sensorManager: SensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- NEUE ZEILE 2 ---
        // Der Sensor-Manager wird initialisiert, sobald die App startet.
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        enableEdgeToEdge()
        setContent {
            CompassLocationHeightTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CompassLocationHeightTheme {
        Greeting("Android")
    }
}