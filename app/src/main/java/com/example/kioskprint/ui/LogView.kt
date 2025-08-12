package com.example.kioskprint.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun getCurrentTimestamp(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}

@Preview(
    name = "print",
    device = "spec:width=1080dp, height=1920dp",
    showSystemUi = false
)
@Composable
fun PreviewLogView(){
    LogView(message = "Test")
}

@Composable
fun LogView(
    message: String
){
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = getCurrentTimestamp(),
            color = Color.Black,
            fontSize = (LocalConfiguration.current.screenWidthDp.dp * 0.04f).value.sp
        )
        Spacer(Modifier.width(20.dp))
        Text(
            text = message,
            color = Color.Black,
            fontSize = (LocalConfiguration.current.screenWidthDp.dp * 0.04f).value.sp
        )
    }
}