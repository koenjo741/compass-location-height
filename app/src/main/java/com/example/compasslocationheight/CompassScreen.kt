// --- CompassScreen.kt ---

package com.example.compasslocationheight

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.compasslocationheight.ui.theme.CompassLocationHeightTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import android.hardware.SensorManager

@Composable
fun MainActivity.CompassScreen() { // <-- Umbenannt von UserInterface zu CompassScreen
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentDate = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(now)
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.GERMANY).format(now)
            delay(1000)
        }
    }

    val context = LocalContext.current
    val window = (context as ComponentActivity).window
    val view = LocalView.current

    val isDarkTheme = when (currentThemeMode) {
        ThemeMode.Dark, ThemeMode.Night -> true
        ThemeMode.Light -> false
    }

    SideEffect {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        window.statusBarColor = android.graphics.Color.TRANSPARENT
    }

    val backgroundColor = when (currentThemeMode) {
        ThemeMode.Light -> AppColors.LightBackground
        else -> AppColors.DarkBackground
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
                        Text(text = stringResource(R.string.location_permission_needed), fontSize = 20.sp, color = textColor)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AccuracyIndicator(accuracy = magnetometerAccuracy)
                    ThemeSwitcher(
                        currentMode = currentThemeMode,
                        onThemeChange = { newMode -> currentThemeMode = newMode }
                    )
                }
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
    val currentModeIcon = when (currentMode) {
        ThemeMode.Light -> Icons.Filled.WbSunny
        ThemeMode.Night -> Icons.Filled.NightsStay
        ThemeMode.Dark -> Icons.Filled.DarkMode
    }
    val iconColor = when (currentMode) {
        ThemeMode.Light -> Color(0xFFFFD700)
        ThemeMode.Night -> AppColors.NightHeading
        ThemeMode.Dark -> AppColors.DarkHeading
    }
    Icon(
        imageVector = currentModeIcon,
        contentDescription = "Aktueller Modus: $currentMode. Klicke, um zu $nextMode zu wechseln",
        tint = iconColor,
        modifier = Modifier
            .size(30.dp)
            .clickable {
                onThemeChange(nextMode)
            }
    )
}

fun degreesToCardinalDirection(degrees: Int): String {
    val directions = arrayOf("N", "NNO", "NO", "ONO", "O", "OSO", "SO", "SSO", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    return directions[((degrees + 11.25) / 22.5).toInt() % 16]
}

@Composable
fun CompassHeader(azimuth: Float, magneticDeclination: Float, color: Color) {
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
        color = color,
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
                val color = textColor.copy(alpha = if (isMajorLine) 1f else 0.5f)
                val strokeWidth = if (isMajorLine) 4f else 2f
                val startOffset = Offset(x = center.x + (radius - lineLength) * sin(angleInRad).toFloat(), y = center.y - (radius - lineLength) * cos(angleInRad).toFloat())
                val endOffset = Offset(x = center.x + radius * sin(angleInRad).toFloat(), y = center.y - radius * cos(angleInRad).toFloat())
                drawLine(color, start = startOffset, end = endOffset, strokeWidth = strokeWidth)
            }
            val northAngleInRad = Math.toRadians(0.0)
            val northLineLength = 48f
            drawLine(color = accentColor, start = Offset(x = center.x + (radius - northLineLength) * sin(northAngleInRad).toFloat(), y = center.y - (radius - northLineLength) * cos(northAngleInRad).toFloat()), end = Offset(x = center.x + radius * sin(northAngleInRad).toFloat(), y = center.y - radius * cos(northAngleInRad).toFloat()), strokeWidth = 8f)
        }
        val directionRadius = radius * 0.82f
        val numberRadius = radius * 0.72f
        val textSize = 24.sp * 1.15f
        val numberTextSize = 18.sp
        val textStyleN = TextStyle(color = accentColor, fontSize = textSize, fontWeight = FontWeight.Bold)
        val textStyleOthers = TextStyle(color = textColor, fontSize = textSize, fontWeight = FontWeight.SemiBold)
        val numberStyle = TextStyle(color = textColor.copy(alpha = 0.7f), fontSize = numberTextSize)

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
            drawPath(path, color = headingColor)

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
        Text(text = stringResource(R.string.loading_location), fontSize = 20.sp, color = textColor)
        return
    }

    val context = LocalContext.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = address,
            fontSize = 20.sp,
            color = textColor,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .border(
                    width = 1.dp,
                    color = subtleColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable { openMaps(context, latitude, longitude) }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )

        val lat = String.format(Locale.US, "%.6f", latitude)
        val lon = String.format(Locale.US, "%.6f", longitude)
        val altBaro = String.format(Locale.US, "%.1f", barometricAltitude)
        Text(text = stringResource(R.string.latitude_label, lat), fontSize = 16.sp, color = subtleColor)
        Text(text = stringResource(R.string.longitude_label, lon), fontSize = 16.sp, color = subtleColor)
        Text(text = stringResource(R.string.altitude_label, altBaro), fontSize = 16.sp, color = subtleColor)

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = stringResource(R.string.date_label, currentDate), fontSize = 16.sp, color = subtleColor)
        Text(text = stringResource(R.string.time_label, currentTime), fontSize = 16.sp, color = subtleColor)

        currentTemperature?.let { temp ->
            val tempFormatted = String.format(Locale.US, "%.1f", temp)
            Text(text = stringResource(R.string.temperature_label, tempFormatted), fontSize = 16.sp, color = subtleColor)
        }

        if (currentPressure > 0f) {
            val pressureFormatted = String.format(Locale.US, "%.2f", currentPressure)
            Text(text = stringResource(R.string.pressure_label, pressureFormatted, pressureTrend), fontSize = 16.sp, color = subtleColor)
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

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun DefaultPreview() {
    CompassLocationHeightTheme(darkTheme = true) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            CompassHeader(
                azimuth = 330f,
                magneticDeclination = 4f,
                color = AppColors.DarkHeading
            )
            Box(contentAlignment = Alignment.Center) {
                CompassRose(
                    azimuth = 330f,
                    magneticDeclination = 4f,
                    textColor = AppColors.DarkText,
                    accentColor = AppColors.DarkAccent,
                    subtleColor = AppColors.DarkSubtle
                )
                CompassOverlay(
                    pitch = -1.5f,
                    roll = 2.5f,
                    headingColor = AppColors.DarkHeading
                )
            }
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