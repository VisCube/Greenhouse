package com.example.myapplication

data class GreenhouseReferences(
    val sensors: List<SensorReference>
)

data class SensorReference(
    val id: Int,
    val reference: Float,
)