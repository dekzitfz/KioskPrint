package com.example.kioskprint

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.kioskprint.ui.TestPrintScreen
import com.sunmi.peripheral.printer.InnerPrinterCallback
import com.sunmi.peripheral.printer.InnerPrinterManager
import com.sunmi.peripheral.printer.SunmiPrinterService
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.drawToBitmap
import androidx.lifecycle.lifecycleScope
import com.example.kioskprint.ui.PrintView
import com.sunmi.peripheral.printer.InnerResultCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {

    private var printService: SunmiPrinterService? = null
    private val eventLog = mutableStateOf("")
    private fun appendLog(message: String) {
        runOnUiThread {
            val prefix = if (eventLog.value.isBlank()) "" else "\n"
            eventLog.value += prefix + message
        }
    }
    val innerPrinterCallback = object : InnerPrinterCallback() {
        override fun onConnected(service: SunmiPrinterService) {
            //Timber.i("Printer Connected")
            printService = service
            appendLog("Printer connected")
            appendLog("printer version: ${printService?.printerVersion}")
            appendLog("printer model: ${printService?.printerModal}")
            appendLog("printer service version: ${printService?.serviceVersion}")

            appendLog("printer status code: ${printService?.updatePrinterState()}")

            appendLog("starting printer initialization")
            printService?.printerInit(printerInitCallback)
        }

        override fun onDisconnected() {
            appendLog("Printer disconnected")
        }
    }

    private val printerInitCallback = object : InnerResultCallback() {
        override fun onRunResult(isSuccess: Boolean) {
            appendLog("[onRunResult] printer initialization: isSuccess is $isSuccess")
            appendLog("executing clearBuffer after initialization")
            printService?.clearBuffer()
        }
        override fun onReturnString(result: String?) {
            result?.let { appendLog("[onReturnString] printer initialization: $result") }
        }
        override fun onRaiseException(code: Int, message: String) {
            appendLog("[onRaiseException] printer initialization: code $code, message $message")
        }
        override fun onPrintResult(code: Int, message: String) {
            appendLog("[onPrintResult] printer initialization: code $code, message $message")
        }
    }

    private val printerTextCallback = object : InnerResultCallback() {
        override fun onRunResult(isSuccess: Boolean) {
            appendLog("[onRunResult] print text: isSuccess is $isSuccess")

            if(isSuccess){
                appendLog("execute cutting paper")
                printService?.cutPaper(printerCutCallback)
            }
        }
        override fun onReturnString(result: String?) {
            result?.let { appendLog("[onReturnString] print text: $result") }
        }
        override fun onRaiseException(code: Int, message: String) {
            appendLog("[onRaiseException] print text: code $code, message $message")
        }
        override fun onPrintResult(code: Int, message: String) {
            appendLog("[onPrintResult] print text: code $code, message $message")
        }
    }

    private val printerBitmapCallback = object : InnerResultCallback() {
        override fun onRunResult(isSuccess: Boolean) {
            appendLog("[onRunResult] print bitmap: isSuccess is $isSuccess")

            if(isSuccess){
                appendLog("execute cutting paper")
                printService?.cutPaper(printerCutCallback)
            }
        }
        override fun onReturnString(result: String?) {
            result?.let { appendLog("[onReturnString] print bitmap: $result") }
        }
        override fun onRaiseException(code: Int, message: String) {
            appendLog("[onRaiseException] print bitmap: code $code, message $message")
        }
        override fun onPrintResult(code: Int, message: String) {
            appendLog("[onPrintResult] print bitmap: code $code, message $message")
        }
    }

    private val printerCutCallback = object : InnerResultCallback() {
        override fun onRunResult(isSuccess: Boolean) {
            appendLog("[onRunResult] print cut: isSuccess is $isSuccess")
        }
        override fun onReturnString(result: String?) {
            result?.let { appendLog("[onReturnString] print cut: $result") }
        }
        override fun onRaiseException(code: Int, message: String) {
            appendLog("[onRaiseException] print cut: code $code, message $message")
        }
        override fun onPrintResult(code: Int, message: String) {
            appendLog("[onPrintResult] print cut: code $code, message $message")
        }
    }

    private val printerOutPaperCallback = object : InnerResultCallback() {
        override fun onRunResult(isSuccess: Boolean) {
            appendLog("[onRunResult] print out: isSuccess is $isSuccess")
        }
        override fun onReturnString(result: String?) {
            result?.let { appendLog("[onReturnString] print out: $result") }
        }
        override fun onRaiseException(code: Int, message: String) {
            appendLog("[onRaiseException] print out: code $code, message $message")
        }
        override fun onPrintResult(code: Int, message: String) {
            appendLog("[onPrintResult] print out: code $code, message $message")
        }
    }

    override fun onStart() {
        setupPrinter()
        super.onStart()
    }

    override fun onStop() {
        printService?.let {
            appendLog("unbinding printing service")
            InnerPrinterManager.getInstance().unBindService(this, innerPrinterCallback)
            printService = null
            appendLog("printer service unbinded")
        }
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Screen.PrintTest
            ) {
                composable<Screen.PrintTest> {
                    TestPrintScreen(
                        eventLog = eventLog.value,
                        onPrintAsImage = { content ->
                            printImage(content)
                        },
                        onPrintAsText = { content ->
                            printText(content)
                        }
                    )
                }
            }
        }
    }

    fun printText(content: String){
        if(printService == null){
            appendLog("printer service not available")
            return
        }

        printService?.printText(content, printerTextCallback)
    }

    fun printImage(content: String){
        if(printService == null){
            appendLog("printer service not available")
            return
        }

        lifecycleScope.launch {
            appendLog("creating bitmap with content $content")
            val bitmap = createBitmapFromComposable(
                activity = this@MainActivity,
                composable = { PrintView(content) }
            )

            appendLog("bitmap created")

            appendLog("printing bitmap image")
            printService?.printBitmap(bitmap, printerBitmapCallback)

            appendLog("execute autoOutPaper")
            printService?.autoOutPaper(printerOutPaperCallback)
        }
    }

    private fun setupPrinter(){
        appendLog("preparing to connect printer")
        val result = InnerPrinterManager.getInstance().bindService(this, innerPrinterCallback)
        appendLog("printer bind success is $result")
    }

    suspend fun createBitmapFromComposable(
        activity: ComponentActivity,
        composable: @Composable () -> Unit
    ): Bitmap = withContext(Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val composeView = ComposeView(activity)
            composeView.setContent { composable() }
            // Set to wrap_content for measurement
            composeView.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )

            // We create a container to add/remove the view
            val decorView = activity.window.decorView as FrameLayout
            decorView.addView(composeView)

            // Wait for layout pass
            composeView.post {
                try {
                    val bitmap = composeView.drawToBitmap(Bitmap.Config.ARGB_8888)
                    decorView.removeView(composeView) // Clean up
                    continuation.resume(bitmap)
                } catch (e: Exception) {
                    appendLog("failed create bitmap!")
                    appendLog(e.message?: "no message")
                    decorView.removeView(composeView)
                    throw e
                }
            }
        }
    }
}