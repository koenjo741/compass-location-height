package com.example.compasslocationheight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.about_title),
            fontSize = 24.sp,
            color = headingColor,
            modifier = Modifier.padding(bottom = 16.dp) // Ein kleiner Abstand nach dem Titel
        )
        Text(
            text = stringResource(R.string.about_placeholder),
            fontSize = 20.sp,
            color = textColor
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = headingColor)
        ) {
            Text(text = stringResource(R.string.button_back), fontSize = 18.sp)
        }
    }
}