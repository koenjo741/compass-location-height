package com.example.compasslocationheight

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import com.example.compasslocationheight.ui.theme.CompassLocationHeightTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import android.hardware.SensorManager

@Composable
fun MainActivity.CompassScreen(
    navController: NavController,
    settingsViewModel: SettingsViewModel
) {
    val currentTheme by settingsViewModel.themeMode.collectAsState()
    val currentTempUnit by settingsViewModel.tempUnit.collectAsState()
    val currentCoordFormat by settingsViewModel.coordFormat.collectAsState()

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

    val isDarkTheme = when (currentTheme) {
        ThemeMode.Dark, ThemeMode.Night -> true
        ThemeMode.Light -> false
        else -> true // Default to dark theme
    }

    SideEffect {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
        window.statusBarColor = android.graphics.Color.TRANSPARENT
    }

    val backgroundColor = when (currentTheme) {
        ThemeMode.Light -> AppColors.LightBackground
        else -> AppColors.DarkBackground
    }
    val headingColor = when (currentTheme) {
        ThemeMode.Light -> AppColors.LightHeading
        ThemeMode.Night -> AppColors.NightHeading
        ThemeMode.Dark -> AppColors.DarkHeading
        else -> AppColors.DarkHeading
    }
    val textColor = when (currentTheme) {
        ThemeMode.Light -> AppColors.LightText
        ThemeMode.Night -> AppColors.NightText
        ThemeMode.Dark -> AppColors.DarkText
        else -> AppColors.DarkText
    }
    val accentColor = when (currentTheme) {
        ThemeMode.Light -> AppColors.LightAccent
        ThemeMode.Night -> AppColors.NightAccent
        ThemeMode.Dark -> AppColors.DarkAccent
        else -> AppColors.DarkAccent
    }
    val subtleColor = when (currentTheme) {
        ThemeMode.Light -> AppColors.LightSubtle
        ThemeMode.Night -> AppColors.NightSubtle
        ThemeMode.Dark -> AppColors.DarkSubtle
        else -> AppColors.DarkSubtle
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
                            gpsAltitude = gpsAltitude,
                            isLocationAvailable = isLocationAvailable,
                            address = addressText,
                            currentDate = currentDate,
                            currentTime = currentTime,
                            currentTemperature = currentTemperature,
                            currentHumidity = currentHumidity,
                            currentPressure = currentPressure,
                            pressureTrend = pressureTrend,
                            textColor = textColor,
                            subtleColor = subtleColor,
                            tempUnit = currentTempUnit,
                            coordFormat = currentCoordFormat
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navController.navigate("settings") }) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.cd_open_settings),
                                tint = headingColor
                            )
                        }
                        IconButton(onClick = { navController.navigate("about") }) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = stringResource(R.string.cd_open_about),
                                tint = headingColor
                            )
                        }
                        ThemeSwitcher(
                            currentMode = currentTheme,
                            onThemeChange = { newTheme -> settingsViewModel.setTheme(newTheme) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSwitcher(currentMode: ThemeMode, onThemeChange: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val nextMode = when (currentMode) {
        ThemeMode.Dark -> ThemeMode.Night
        ThemeMode.Night -> ThemeMode.Light
        ThemeMode.Light -> ThemeMode.Dark
        else -> ThemeMode.Dark
    }
    val currentModeIcon = when (currentMode) {
        ThemeMode.Light -> Icons.Filled.WbSunny
        ThemeMode.Night -> Icons.Filled.NightsStay
        ThemeMode.Dark -> Icons.Filled.DarkMode
        else -> Icons.Filled.DarkMode
    }
    val iconColor = when (currentMode) {
        ThemeMode.Light -> Color(0xFFFFD700)
        ThemeMode.Night -> AppColors.NightHeading
        ThemeMode.Dark -> AppColors.DarkHeading
        else -> AppColors.DarkHeading
    }
    Icon(
        imageVector = currentModeIcon,
        contentDescription = stringResource(R.string.cd_switch_theme, currentMode.name, nextMode.name),
        tint = iconColor,
        modifier = Modifier
            .size(30.dp)
            .clickable {
                onThemeChange(nextMode)
            }
    )
}

fun degreesToCardinalDirection(degrees: Int): String {
    val directions = arrayOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    return directions[((degrees + 11.25) / 22.5).toInt() % 16]
}

// Updated helper function with direction support
fun formatCoordinate(coordinate: Double, format: CoordinateFormat, isLatitude: Boolean): String {
    val direction = if (isLatitude) {
        if (coordinate >= 0) "N" else "S"
    } else {
        if (coordinate >= 0) "E" else "W"
    }
    val absCoord = abs(coordinate)

    return when (format) {
        CoordinateFormat.Decimal -> String.format(Locale.US, "%.6f", coordinate)
        CoordinateFormat.DMS -> {
            val degrees = absCoord.toInt()
            val minutes = ((absCoord - degrees) * 60).toInt()
            val seconds = (absCoord - degrees - minutes / 60.0) * 3600
            String.format(Locale.US, "%d° %d' %.1f\" %s", degrees, minutes, seconds, direction)
        }
        CoordinateFormat.DDM -> {
            val degrees = absCoord.toInt()
            val minutes = (absCoord - degrees) * 60
            String.format(Locale.US, "%d° %.4f' %s", degrees, minutes, direction)
        }
    }
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

fun DrawScope.drawTextCustom(textMeasurer: TextMeasurer, text: String, center: Offset, radius: Float, angleDegrees: Float, style: TextStyle) {
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
    gpsAltitude: Double,
    isLocationAvailable: Boolean,
    address: String,
    currentDate: String,
    currentTime: String,
    currentTemperature: Double?,
    currentHumidity: Int?,
    currentPressure: Float,
    pressureTrend: String,
    textColor: Color,
    subtleColor: Color,
    tempUnit: TemperatureUnit,
    coordFormat: CoordinateFormat
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
                .border(width = 1.dp, color = subtleColor, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable { openMaps(context, latitude, longitude) }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
        
        // Passing true for Latitude, false for Longitude
        val latString = formatCoordinate(latitude, coordFormat, true)
        val lonString = formatCoordinate(longitude, coordFormat, false)
        
        val altBaro = String.format(Locale.US, "%.1f", barometricAltitude)
        
        Text(text = stringResource(R.string.latitude_label, latString), fontSize = 16.sp, color = subtleColor)
        Text(text = stringResource(R.string.longitude_label, lonString), fontSize = 16.sp, color = subtleColor)
        Text(text = stringResource(R.string.altitude_label, altBaro), fontSize = 16.sp, color = subtleColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = stringResource(R.string.date_label, currentDate), fontSize = 16.sp, color = subtleColor)
        Text(text = stringResource(R.string.time_label, currentTime), fontSize = 16.sp, color = subtleColor)
        currentTemperature?.let { tempCelsius ->
            val displayTemp = if (tempUnit == TemperatureUnit.Fahrenheit) {
                (tempCelsius * 9 / 5) + 32
            } else {
                tempCelsius
            }
            val unitSuffix = if (tempUnit == TemperatureUnit.Fahrenheit) "°F" else "°C"
            val tempFormatted = String.format(Locale.US, "%.1f", displayTemp)
            Text(text = "${stringResource(R.string.temperature_label)} $tempFormatted $unitSuffix", fontSize = 16.sp, color = subtleColor)
        }
        currentHumidity?.let {
            Text(text = "${stringResource(R.string.humidity_label)} $it%", fontSize = 16.sp, color = subtleColor)
        }
        if (currentPressure > 0f) {
            val pressureFormatted = if (isLocationAvailable && gpsAltitude != 0.0) {
                val p1 = String.format(Locale.US, "%.1f", currentPressure)
                val qnh = currentPressure + (gpsAltitude / 8.0)
                val p2 = String.format(Locale.US, "%.1f", qnh)
                "$p1 / $p2"
            } else {
                String.format(Locale.US, "%.1f", currentPressure)
            }
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
    // Preview is broken due to missing dependencies, which is fine.
}