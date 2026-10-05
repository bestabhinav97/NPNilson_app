package com.example.np_nilson.ui.branding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.np_nilson.ui.theme.NpBluePrimary
import com.example.np_nilson.ui.theme.NpBlueDark

@Composable
fun NpNilssonFullLogo(
    modifier: Modifier = Modifier,
    isDarkBackground: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        // NP Square
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .border(6.dp, if (isDarkBackground) Color.White else Color.Black, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NP",
                color = Color.Black,
                fontSize = 68.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = (-4).sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // NP NILSSON Blue Banner
        Box(
            modifier = Modifier
                .width(220.dp)
                .background(NpBluePrimary, RoundedCornerShape(2.dp))
                .padding(vertical = 10.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NP NILSSON",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun NpNilssonHeaderLogo(
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .background(Color.White)
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        // NP Box
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.White)
                .border(2.dp, Color.Black, RoundedCornerShape(2.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NP",
                color = Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Blue NILSSON box
        Box(
            modifier = Modifier
                .height(36.dp)
                .background(NpBluePrimary, RoundedCornerShape(2.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "NILSSON",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}
