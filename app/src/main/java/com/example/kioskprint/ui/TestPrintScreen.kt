package com.example.kioskprint.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout

@Preview(
    name = "print",
    device = "spec:width=1080dp, height=1920dp",
    showSystemUi = true
)
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun TestPrintScreen(
    eventLog: String = "",
    onPrintAsText: (String) -> Unit = {},
    onPrintAsImage: (String) -> Unit = {}
){
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .background(Color.White)
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            val logMessages = remember(eventLog) { eventLog.lines().filter { it.isNotBlank() } }
            val printValue = remember { mutableStateOf("") }

            val maxHeightInDp = with(LocalDensity.current) { maxHeight }
            val maxWidthInDp = with(LocalDensity.current) { maxWidth }

            ConstraintLayout(
                modifier = Modifier
                    .height(maxHeightInDp)
                    .padding(10.dp)
            ) {
                val (topView, bottomView) = createRefs()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxHeightInDp * 0.45f)
                        .constrainAs(topView) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            top.linkTo(parent.top)
                        }
                ) {
                    Text(
                        text = "Input Text To Print:",
                        color = Color.Black,
                        fontSize = (maxWidthInDp.value * 0.05f).sp
                    )
                    
                    TextField(
                        value = printValue.value,
                        onValueChange = { newValue ->
                            if (newValue.length <= 4) {
                                printValue.value = newValue
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxHeightInDp * 0.08f)
                            .padding(vertical = 10.dp),
                        placeholder = {
                            Text(
                                text = "Enter up to 4 digits",
                                fontSize = (maxWidthInDp.value * 0.04f).sp
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        textStyle = TextStyle(
                            fontSize = (maxWidthInDp.value * 0.05f).sp,
                            color = Color.Black
                        )
                    )

                    Spacer(Modifier.height(10.dp))
                    ElevatedButton(
                        onClick = { onPrintAsText(printValue.value) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxHeightInDp * 0.05f)
                    ) {
                        Text(
                            text = "Print as Text",
                            fontSize = (maxWidthInDp.value * 0.045f).sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    ElevatedButton(
                        onClick = { onPrintAsImage(printValue.value) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(maxHeightInDp * 0.05f)
                    ) {
                        Text(
                            text = "Print as Image",
                            fontSize = (maxWidthInDp.value * 0.045f).sp
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(maxHeightInDp * 0.5f)
                        .constrainAs(bottomView) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        }
                ) {
                    Text(
                        text = "Log",
                        color = Color.Black,
                        fontSize = (maxWidthInDp.value * 0.05f).sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    20.dp
                                )
                            )
                            .border(
                                width = 2.dp,
                                color = Color.Gray,
                                shape = RoundedCornerShape(
                                    20.dp
                                )
                            )
                            .padding((maxWidthInDp.value * 0.015f).dp)
                    ){
                        LazyColumn {
                            items(logMessages.size){
                                LogView(logMessages[it])
                            }
                        }
                    }
                }
            }

        }
    }
}