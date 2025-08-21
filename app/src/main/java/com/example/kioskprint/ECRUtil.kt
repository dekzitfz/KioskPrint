package com.example.kioskprint

import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.HEADER
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.STOP_TAG
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.SUB_TRANS_TYPE_CARD
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.SUB_TRANS_TYPE_CHECK_CONNECTION
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.SUB_TRANS_TYPE_SALE_QRIS_BNI
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.SUB_TRANS_TYPE_SALE_QRIS_OTHER
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.SUB_TRANS_TYPE_SHOW_QRIS
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.TRANS_TYPE_CARD
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.TRANS_TYPE_CHECK_CONNECTION
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.TRANS_TYPE_SALE_QRIS
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.TRANS_TYPE_SHOW_QRIS
import com.example.kioskprint.ElectronicCashRegisterUtil.Companion.VERSION
import kotlin.math.roundToInt

class ElectronicCashRegisterUtil {
    companion object {
        const val HEADER = "02"
        const val VERSION = "01"
        const val STOP_TAG = "03"

        //trans type
        const val TRANS_TYPE_CARD = "31"
        const val TRANS_TYPE_CHECK_CONNECTION = "3B"
        const val TRANS_TYPE_SALE_QRIS = "65"
        const val TRANS_TYPE_SHOW_QRIS = "66"

        //sub trans type
        const val SUB_TRANS_TYPE_CARD = "30"
        const val SUB_TRANS_TYPE_SHOW_QRIS = "31"
        const val SUB_TRANS_TYPE_CHECK_CONNECTION = "33"
        const val SUB_TRANS_TYPE_SALE_QRIS_BNI = "37"
        const val SUB_TRANS_TYPE_SALE_QRIS_OTHER = "3D"
    }
}

fun generateSaleCardCommand(amount: Double): String {
    val amountHex = amount.roundToInt().toPriceAmountHexString()
    val additionalAmountHex = 0.toPriceAmountHexString()
    val command = "$HEADER$VERSION$TRANS_TYPE_CARD$SUB_TRANS_TYPE_CARD$amountHex$additionalAmountHex$STOP_TAG"
    val crc = command.calculateCRC()
    return "$command$crc"
}

/**
 * CheckConnection
 * should return 02013B33030A
 * */
fun generateCheckConnectionCommand(): String {
    val command = "$HEADER$VERSION$TRANS_TYPE_CHECK_CONNECTION$SUB_TRANS_TYPE_CHECK_CONNECTION$STOP_TAG"
    val crc = command.calculateCRC()
    return "$command$crc"
}

fun Int.toPriceAmountHexString(): String {
    // Convert amount to string and pad left with '0' to length 12
    val paddedAmount = this.toString().padStart(12, '0')
    // Convert each character digit to its ASCII hex representation
    return paddedAmount.map { c -> "%02X".format(c.code) }.joinToString(separator = "")
}

//generate show QRIS Command
fun generateShowQRISCommand(amount: Double, transactionId: String? = null): String {
    val amountHex = amount.roundToInt().toPriceAmountHexString()
    //val trxIdHex = "4944" + transactionId.asciiToHexString()
    //val command = "$HEADER$VERSION$TRANS_TYPE_SHOW_QRIS$SUB_TRANS_TYPE_SHOW_QRIS$amountHex$trxIdHex$STOP_TAG"
    val command = "$HEADER$VERSION$TRANS_TYPE_SHOW_QRIS$SUB_TRANS_TYPE_SHOW_QRIS$amountHex$STOP_TAG"
    val crc = command.calculateCRC()
    return "$command$crc"
}

fun String.asciiToHexString(): String {
    return this.map { char -> "%02X".format(char.code) }.joinToString("")
}

//generate sale qris BNI
fun generateSaleQRISBNICommand(amount: Double): String {
    //format amount price to hex
    val amountHex = amount.roundToInt().toPriceAmountHexString()
    val command = "$HEADER$VERSION$TRANS_TYPE_SALE_QRIS$SUB_TRANS_TYPE_SALE_QRIS_BNI$amountHex$STOP_TAG"
    val crc = command.calculateCRC()
    return "$command$crc"
}

fun generateSaleQRISOtherCommand(amount: Double): String {
    //format amount price to hex
    val amountHex = amount.roundToInt().toPriceAmountHexString()
    val command = "$HEADER$VERSION$TRANS_TYPE_SALE_QRIS$SUB_TRANS_TYPE_SALE_QRIS_OTHER$amountHex$STOP_TAG"
    val crc = command.calculateCRC()
    return "$command$crc"
}

fun String.hexToByteArray(): ByteArray {
    val result = ByteArray(length / 2)
    for (i in indices step 2) {
        val byte = substring(i, i + 2).toInt(16)
        result[i / 2] = byte.toByte()
    }
    return result
}

fun String.calculateCRC(): String {
    // Parse the string two characters at a time into bytes
    val bytes = this.chunked(2)
        .map { it.toInt(16).toByte() }

    // Exclude the first byte (STX)
    val bytesToCrc = bytes.drop(1)

    // Calculate XOR CRC over the selected bytes
    val crc = bytesToCrc.fold(0.toByte()) { acc, b -> (acc.toInt() xor b.toInt()).toByte() }

    // Return the CRC as uppercase hex string
    return "%02X".format(crc)
}

fun hexStringToAscii(hex: String): String {
    // Remove spaces, then convert each pair to character
    return hex.replace(" ", "")
        .chunked(2)
        .map { it.toInt(16).toChar() }
        .joinToString("")
}

fun extractFieldFromAscii(ascii: String, fieldIndex: Int): String {
    val fields = ascii.split('|')
    // Make sure fieldIndex is within the field bounds
    return fields.getOrElse(fieldIndex) { "" }
}