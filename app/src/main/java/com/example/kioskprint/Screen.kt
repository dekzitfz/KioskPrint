package com.example.kioskprint

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    object PrintTest : Screen()
}