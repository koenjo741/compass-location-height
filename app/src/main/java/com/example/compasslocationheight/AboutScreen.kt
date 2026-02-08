package com.example.compasslocationheight

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun AboutScreen(
    navController: NavController,
    backgroundColor: Color,
    textColor: Color,
    headingColor: Color
) {
    val actionGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFF512F), Color(0xFFDD2476))
    )
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = stringResource(R.string.about_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = headingColor
            )
            
            Text(
                text = stringResource(R.string.about_version, BuildConfig.VERSION_NAME),
                fontSize = 14.sp,
                color = textColor.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            AboutSection(
                title = stringResource(R.string.about_section_functions),
                items = listOf(
                    stringResource(R.string.about_desc_north),
                    stringResource(R.string.about_desc_maps),
                    stringResource(R.string.about_desc_accuracy),
                    stringResource(R.string.about_desc_themes),
                    stringResource(R.string.about_desc_languages),
                    stringResource(R.string.about_desc_ads)
                ),
                textColor = textColor,
                headingColor = headingColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            AboutSection(
                title = stringResource(R.string.about_section_contact),
                items = listOf(
                    stringResource(R.string.about_desc_email)
                ),
                textColor = textColor,
                headingColor = headingColor
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            AboutSection(
                title = stringResource(R.string.about_section_credits),
                items = listOf(
                    stringResource(R.string.about_desc_open_meteo)
                ),
                textColor = textColor,
                headingColor = headingColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            AboutSection(
                title = stringResource(R.string.about_section_privacy),
                items = listOf(
                    stringResource(R.string.about_desc_privacy_policy)
                ),
                textColor = textColor,
                headingColor = headingColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            AboutSection(
                title = stringResource(R.string.about_section_disclaimer),
                items = listOf(
                    stringResource(R.string.about_desc_risk),
                    stringResource(R.string.about_desc_compass_interference),
                    stringResource(R.string.about_desc_gps_interference),
                    stringResource(R.string.about_desc_weather_data),
                    stringResource(R.string.about_desc_software_error)
                ),
                textColor = textColor,
                headingColor = headingColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            GradientButton(
                text = stringResource(R.string.button_back),
                onClick = { navController.popBackStack() },
                gradient = actionGradient,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
fun AboutSection(
    title: String,
    items: List<String>,
    textColor: Color,
    headingColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = headingColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        items.forEach { item ->
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "• ",
                    color = textColor,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = item,
                    color = textColor,
                    fontSize = 16.sp
                )
            }
        }
    }
}