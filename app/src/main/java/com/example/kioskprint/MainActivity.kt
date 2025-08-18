package com.example.kioskprint

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.hardware.usb.UsbManager
import android.os.Build
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
import com.example.kioskprint.ui.EmptyPrintView
import com.example.kioskprint.ui.PrintView
import com.example.kioskprint.ui.TestQRScreen
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.HexDump
import com.hoho.android.usbserial.util.SerialInputOutputManager
import com.sunmi.peripheral.printer.InnerResultCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity(), SerialInputOutputManager.Listener {

    private val ACTION_USB_PERMISSION = BuildConfig.APPLICATION_ID + ".GRANT_USB"
    private lateinit var usbManager: UsbManager
    private var port: UsbSerialPort? = null
    private var usbIoManager: SerialInputOutputManager? = null
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        appendLog("usb permission granted")
                    } else {
                        appendLog("usb permission denied")
                    }
                }
            }
        }
    }
    private val qrStringData = mutableStateOf("")
    private val qrAmount = 0.0

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

    private fun registerUSBReceiver(){
        val filter = IntentFilter(ACTION_USB_PERMISSION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(usbReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            registerReceiver(usbReceiver, filter)
        }
    }

    private fun requestUSBPermission(driver: UsbSerialDriver){
        val permissionIntent = PendingIntent.getBroadcast(this, 0,
            Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE)

        usbManager.requestPermission(driver.device, permissionIntent)
    }

    private fun listUSBDevices(){
        usbManager = getSystemService(USB_SERVICE) as UsbManager

        //find drivers
        val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        availableDrivers.forEach { driver ->
            appendLog("device: ${driver.device.productName}, ports size: ${driver.ports.size}")
        }
        if(availableDrivers.isNotEmpty()){
            //get first index
            val driver = availableDrivers[0]
            val connection = usbManager.openDevice(driver.device)
            if(connection == null){
                requestUSBPermission(driver)
                return
            }

            //if there is connection, open it
            port = driver.ports[0] // Most devices have just one port (port 0)
            appendLog("opening port $port")
            port?.open(connection)
            appendLog("setting port parameters...")
            port?.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            //add port listener
            usbIoManager = SerialInputOutputManager(port, this)
            usbIoManager?.start()
        }
    }

    private fun sendData(data: ByteArray){
        appendLog("SendDataECR...")
        port?.write(data, 1000)
    }

    override fun onNewData(data: ByteArray) {
        appendLog("onNewData serial usb received")

        if(data.isNotEmpty()){
            val hexReceived = HexDump.dumpHexString(data)
            appendLog("hexReceived: $hexReceived with size: ${data.size}")

            val hexData = data.joinToString(" ") { "%02X".format(it) }
            appendLog("raw hex received:\n$hexData")

            if(data[0] == 0x06.toByte() && data.size == 1){
                appendLog("ACK received")
            }

            if(data.size > 11){
                if (data[4] == 0x30.toByte() && data[5] == 0x30.toByte()) {
                    appendLog("transaction success")

                    val asciiString = hexStringToAscii(hexData)
                    val transactionType = extractFieldFromAscii(asciiString, 4)

                    if(transactionType == "SALE QR"){
                        val QRISTransactionId = extractFieldFromAscii(asciiString, 15)
                        appendLog("sending command to get qris data")
                        sendData(
                            generateShowQRISCommand(
                                amount = qrAmount,
                                transactionId = QRISTransactionId
                            ).hexToByteArray()
                        )
                    }

                    if(asciiString.contains("GENERATE QR")){
                        appendLog("receiving QRIS data result")
                        val qrData = extractFieldFromAscii(asciiString, 8)
                        runOnUiThread {
                            qrStringData.value = qrData
                        }
                    }

                }else{
                    appendLog("transaction failed")
                }
            }
        }else{
            appendLog("empty data received")
        }
    }

    override fun onRunError(e: Exception) {
        appendLog("usb onRunError: ${e.message}")
    }

    override fun onStart() {
        setupPrinter()
        registerUSBReceiver()
        super.onStart()
    }

    override fun onStop() {
        printService?.let {
            appendLog("unbinding printing service")
            InnerPrinterManager.getInstance().unBindService(this, innerPrinterCallback)
            printService = null
            appendLog("printer service unbinded")
        }
        usbIoManager?.listener = null
        usbIoManager?.stop()
        unregisterReceiver(usbReceiver)
        port?.close()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = Screen.QRTest
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
                composable<Screen.QRTest> {
                    TestQRScreen(
                        eventLog = eventLog.value,
                        qrStringData = qrStringData.value,
                        onSubmit = { amount ->
                            if(port != null){
                                sendData(generateSaleQRISBNICommand(amount).hexToByteArray())
                            }else{
                                appendLog("no EDC found")
                            }
                        }
                    )
                }
            }
        }

        listUSBDevices()
    }

    fun printText(content: String){
    }

    fun printImage(content: String){
        if(printService == null){
            appendLog("printer service not available")
            return
        }

        lifecycleScope.launch {
            appendLog("creating bitmap with content $content")
            val bitmapHeader = createBitmapFromComposable(
                activity = this@MainActivity,
                composable = { EmptyPrintView() }
            )

            val bitmap = createBitmapFromComposable(
                activity = this@MainActivity,
                composable = { PrintView(content) }
            )

            appendLog("bitmap created")

            appendLog("printing bitmap images")
            printService?.printBitmap(bitmapHeader, null)
            printService?.printText("-", null)
            printService?.printBitmap(bitmap, null)
            printService?.printText("-", null)

            appendLog("execute autoOutPaper")
            printService?.autoOutPaper(null)
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