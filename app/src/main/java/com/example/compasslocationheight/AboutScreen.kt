package com.example.compasslocationheight

import androidx.compose.foundation.layout.*
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
    headingColor: Color // Dieser Parameter hat gefehlt
) {
    // Derselbe Farbverlauf wie auf der Einstellungs-Seite für Konsistenz
    val actionGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFFF512F), Color(0xFFDD2476))
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = backgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Mehr Abstand zum oberen Rand
            Spacer(modifier = Modifier.height(64.dp))
            
            Text(
                text = stringResource(R.string.about_title),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.about_placeholder),
                fontSize = 18.sp,
                color = textColor
            )

            // Füllt den restlichen Platz, um den Button nach unten zu schieben
            Spacer(Modifier.weight(1f)) 

            // 2. Moderner Gradient-Button
            GradientButton(
                text = stringResource(R.string.button_back),
                onClick = { navController.popBackStack() },
                gradient = actionGradient,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            )
        }
    }
}