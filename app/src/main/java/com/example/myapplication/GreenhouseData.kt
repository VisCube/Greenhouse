package com.example.myapplication

data class GreenhouseData(
    val sensors: List<Sensor>
)

data class Sensor(
    val id: Int,
    val type: String,
    val reference: Float,
    val reading: Float,
    val device: Int
)